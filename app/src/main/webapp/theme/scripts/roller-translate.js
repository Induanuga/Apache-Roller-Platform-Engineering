/*
 * roller-translate.js  — with Translation Cache (5B)
 *
 * CACHING STRATEGY
 * ----------------
 * Storage  : localStorage (browser-side, per user, per origin)
 * Cache key: "rtc::" + pageURL + "::" + targetLang + "::" + provider
 * Cache val: JSON { userConfig, sections: { textHash -> translatedText } }
 *
 * "Significant change" definition
 * --------------------------------
 * A text node is considered CHANGED if its SHA-like hash (djb2) differs from
 * what was stored at last translation time. Only changed nodes hit the API;
 * unchanged nodes are served from cache instantly.
 *
 * On page load: if a cache entry exists for this page + last-used config,
 * the page is automatically translated using cached values (zero API calls).
 * Only nodes whose content has changed since the last translation are
 * re-fetched from the API — all other nodes use cached translations.
 *
 * Full-page retranslation is only triggered when the user explicitly
 * switches language or provider, or when no cache exists yet.
 */
(function () {
    "use strict";

    var _base = window.location.protocol + "//" + window.location.host;
    var ENDPOINT = window.ROLLER_TRANSLATE_ENDPOINT || (_base + "/roller-services/translation");
    var DEFAULT_PROVIDER = window.ROLLER_TRANSLATE_PROVIDER || "sarvam";
    var PAGE_KEY = window.location.pathname + window.location.search;
    var USER_CONFIG_KEY = "rtc::userconfig::" + PAGE_KEY;
    var CACHE_VERSION = "1";

    var LANGUAGES = [
        { code: "en", name: "English" },
        { code: "hi", name: "Hindi" },
        { code: "ta", name: "Tamil" },
        { code: "te", name: "Telugu" },
        { code: "kn", name: "Kannada" },
        { code: "ml", name: "Malayalam" },
        { code: "mr", name: "Marathi" },
        { code: "bn", name: "Bengali" },
        { code: "gu", name: "Gujarati" },
        { code: "pa", name: "Punjabi" }
    ];

    var SKIP_TAGS = {
        "script":1,"style":1,"code":1,"pre":1,"textarea":1,
        "input":1,"select":1,"noscript":1,"meta":1,"link":1,"option":1
    };

    var savedNodes = [];
    var isTranslated = false;

    // ------------------------------------------------------------------
    // djb2 hash — fast, collision-resistant enough for change detection
    // ------------------------------------------------------------------
    function hashText(str) {
        var h = 5381;
        for (var i = 0; i < str.length; i++) {
            h = ((h << 5) + h) + str.charCodeAt(i);
            h = h & h; // 32-bit
        }
        return (h >>> 0).toString(36);
    }

    // ------------------------------------------------------------------
    // Cache helpers
    // ------------------------------------------------------------------
    function cacheKey(targetLang, provider) {
        return "rtc::" + CACHE_VERSION + "::" + PAGE_KEY + "::" + targetLang + "::" + provider;
    }

    function loadCache(targetLang, provider) {
        try {
            var raw = localStorage.getItem(cacheKey(targetLang, provider));
            return raw ? JSON.parse(raw) : null;
        } catch(e) { return null; }
    }

    function saveCache(targetLang, provider, sections) {
        try {
            var entry = { ts: Date.now(), sections: sections };
            localStorage.setItem(cacheKey(targetLang, provider), JSON.stringify(entry));
        } catch(e) {
            // localStorage full — clear old entries and retry
            clearOldCacheEntries();
            try {
                localStorage.setItem(cacheKey(targetLang, provider), JSON.stringify({ ts: Date.now(), sections: sections }));
            } catch(e2) { /* silent fail */ }
        }
    }

    function clearOldCacheEntries() {
        // Remove translation cache entries older than 7 days
        var cutoff = Date.now() - (7 * 24 * 60 * 60 * 1000);
        var toRemove = [];
        for (var i = 0; i < localStorage.length; i++) {
            var k = localStorage.key(i);
            if (k && k.indexOf("rtc::") === 0) {
                try {
                    var v = JSON.parse(localStorage.getItem(k));
                    if (v && v.ts < cutoff) toRemove.push(k);
                } catch(e) { toRemove.push(k); }
            }
        }
        toRemove.forEach(function(k) { localStorage.removeItem(k); });
    }

    function saveUserConfig(targetLang, provider) {
        try {
            localStorage.setItem(USER_CONFIG_KEY, JSON.stringify({ targetLang: targetLang, provider: provider }));
        } catch(e) {}
    }

    function loadUserConfig() {
        try {
            var raw = localStorage.getItem(USER_CONFIG_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch(e) { return null; }
    }

    // ------------------------------------------------------------------
    // Make element draggable
    // ------------------------------------------------------------------
    function makeDraggable(el) {
        var isDragging = false, offsetX = 0, offsetY = 0;
        el.style.cursor = "move";
        el.addEventListener("mousedown", function(e) {
            isDragging = true;
            var rect = el.getBoundingClientRect();
            offsetX = e.clientX - rect.left;
            offsetY = e.clientY - rect.top;
            document.addEventListener("mousemove", move);
            document.addEventListener("mouseup", stop);
        });
        function move(e) {
            if (!isDragging) return;
            el.style.left = (e.clientX - offsetX) + "px";
            el.style.top = (e.clientY - offsetY) + "px";
            el.style.right = "auto";
            el.style.bottom = "auto";
            el.style.position = "fixed";
        }
        function stop() {
            isDragging = false;
            document.removeEventListener("mousemove", move);
            document.removeEventListener("mouseup", stop);
        }
    }

    // ------------------------------------------------------------------
    // Toolbar
    // ------------------------------------------------------------------
    function buildToolbar() {
        var cfg = loadUserConfig();

        var bar = document.createElement("div");
        bar.id = "roller-translate-bar";
        bar.setAttribute("data-notranslate", "true");
        bar.style.cssText =
            "position:fixed;bottom:16px;right:16px;z-index:99999;" +
            "background:#fff;border:1px solid #ccc;border-radius:8px;" +
            "box-shadow:0 2px 10px rgba(0,0,0,0.15);padding:10px 14px;" +
            "font-family:Arial,sans-serif;font-size:13px;" +
            "display:flex;align-items:center;gap:8px;flex-wrap:wrap;max-width:480px;";

        var label = document.createElement("span");
        label.innerHTML = "&#127760; <b>Translate:</b>";

        var langSel = document.createElement("select");
        langSel.style.cssText = "padding:4px 6px;border:1px solid #bbb;border-radius:4px;font-size:13px;";
        LANGUAGES.forEach(function(l) {
            var o = document.createElement("option");
            o.value = l.code;
            o.text = l.name;
            if (cfg && cfg.targetLang === l.code) o.selected = true;
            langSel.appendChild(o);
        });

        var provSel = document.createElement("select");
        provSel.style.cssText = "padding:4px 6px;border:1px solid #bbb;border-radius:4px;font-size:13px;";
        [["sarvam","Sarvam AI"],["lingva","Lingva"]].forEach(function(p) {
            var o = document.createElement("option");
            o.value = p[0]; o.text = p[1];
            if (cfg ? cfg.provider === p[0] : p[0] === DEFAULT_PROVIDER) o.selected = true;
            provSel.appendChild(o);
        });

        var btn = document.createElement("button");
        btn.textContent = "Translate";
        btn.style.cssText =
            "padding:5px 12px;background:#1a73e8;color:#fff;border:none;" +
            "border-radius:4px;cursor:pointer;font-size:13px;font-weight:bold;";

        var origBtn = document.createElement("button");
        origBtn.textContent = "Original";
        origBtn.style.cssText =
            "padding:5px 10px;background:#f1f3f4;color:#333;" +
            "border:1px solid #ccc;border-radius:4px;cursor:pointer;font-size:13px;display:none;";

        var status = document.createElement("span");
        status.style.cssText = "font-size:12px;color:#555;min-width:140px;";

        // Cache indicator badge
        var cacheBadge = document.createElement("span");
        cacheBadge.style.cssText =
            "font-size:11px;background:#e8f5e9;color:#2e7d32;border:1px solid #a5d6a7;" +
            "border-radius:10px;padding:2px 7px;display:none;";
        cacheBadge.textContent = "⚡ cached";

        btn.onclick = function() {
            doTranslate(langSel.value, provSel.value, btn, origBtn, status, cacheBadge, false);
        };

        origBtn.onclick = function() {
            restoreOriginal(origBtn, status, cacheBadge);
        };

        bar.appendChild(label);
        bar.appendChild(langSel);
        bar.appendChild(provSel);
        bar.appendChild(btn);
        bar.appendChild(origBtn);
        bar.appendChild(cacheBadge);
        bar.appendChild(status);

        document.body.appendChild(bar);
        makeDraggable(bar);

        // Auto-translate on page load if user has a saved config and cache exists
        if (cfg && cfg.targetLang && cfg.targetLang !== "en") {
            var cached = loadCache(cfg.targetLang, cfg.provider);
            if (cached) {
                // Set selects to match saved config
                langSel.value = cfg.targetLang;
                provSel.value = cfg.provider;
                // Auto-apply cached translation (zero API calls)
                setTimeout(function() {
                    doTranslate(cfg.targetLang, cfg.provider, btn, origBtn, status, cacheBadge, true);
                }, 200);
            }
        }
    }

    // ------------------------------------------------------------------
    // Collect text nodes
    // ------------------------------------------------------------------
    function collectTextNodes() {
        var nodes = [];
        function walk(node) {
            if (node.nodeType === 1) {
                if (SKIP_TAGS[node.tagName.toLowerCase()]) return;
                if (node.id === "roller-translate-bar") return;
                if (node.getAttribute && node.getAttribute("data-notranslate")) return;
                for (var i = 0; i < node.childNodes.length; i++) walk(node.childNodes[i]);
            } else if (node.nodeType === 3) {
                if (node.nodeValue.trim().length > 0) nodes.push(node);
            }
        }
        walk(document.body);
        return nodes;
    }

    // ------------------------------------------------------------------
    // Main translate function
    // cacheOnly=true  → only apply cache, never call API (for auto page-load)
    // cacheOnly=false → use cache where possible, API for misses
    // ------------------------------------------------------------------
    function doTranslate(targetLang, provider, btn, origBtn, status, cacheBadge, cacheOnly) {
        if (isTranslated) restoreOriginal(origBtn, status, cacheBadge);

        var nodes = collectTextNodes();
        if (nodes.length === 0) {
            status.textContent = "Nothing to translate.";
            return;
        }

        savedNodes = nodes.map(function(n) {
            return { node: n, original: n.nodeValue };
        });

        var cachedEntry = loadCache(targetLang, provider);
        var cachedSections = cachedEntry ? cachedEntry.sections : {};

        // Split nodes into cache-hits and misses
        var toFetch = [];   // { text, hash, indices[] }
        var textMap = {};   // unique text → [savedNodes indices]

        savedNodes.forEach(function(o, i) {
            var t = o.original.trim();
            if (!t) return;
            var h = hashText(t);
            if (cachedSections[h]) {
                // Cache hit — apply immediately
                o.node.nodeValue = cachedSections[h];
            } else {
                if (!textMap[t]) { textMap[t] = { hash: h, indices: [] }; }
                textMap[t].indices.push(i);
            }
        });

        var uniqueMisses = Object.keys(textMap);
        var cacheHits = savedNodes.length - uniqueMisses.reduce(function(acc, t) {
            return acc + textMap[t].indices.length;
        }, 0);

        // If cache-only mode (auto page load) — only apply what's cached
        if (cacheOnly) {
            if (cacheHits > 0) {
                isTranslated = true;
                origBtn.style.display = "";
                cacheBadge.style.display = "";
                status.style.color = "#1a7340";
                status.textContent = "✓ From cache";
            }
            return;
        }

        // Save user config for next page load
        saveUserConfig(targetLang, provider);

        // If everything was a cache hit
        if (uniqueMisses.length === 0) {
            isTranslated = true;
            btn.disabled = false;
            origBtn.style.display = "";
            cacheBadge.style.display = "";
            status.style.color = "#1a7340";
            status.textContent = "✓ All from cache (" + cacheHits + " nodes)";
            return;
        }

        // Fetch only the misses
        btn.disabled = true;
        cacheBadge.style.display = "none";
        status.style.color = "#555";
        var missCount = uniqueMisses.length;
        status.textContent = cacheHits > 0
            ? "⚡ " + cacheHits + " cached, fetching " + missCount + " new…"
            : "Translating " + missCount + " segments…";

        var sourceLang = (document.documentElement.lang || "en").split("-")[0];
        var done = 0, failed = 0;
        var newSections = {};

        var CONCURRENCY = 1;
        var queue = uniqueMisses.slice();
        var active = 0;

        function oneDone() {
            done++;
            var pct = Math.round((done / missCount) * 100);
            status.textContent = (cacheHits > 0 ? "⚡ " + cacheHits + " cached + " : "") +
                "Fetching… " + pct + "%";

            if (done === missCount) {
                // Merge new translations into cache
                var merged = {};
                for (var k in cachedSections) merged[k] = cachedSections[k];
                for (var k2 in newSections) merged[k2] = newSections[k2];
                saveCache(targetLang, provider, merged);

                isTranslated = true;
                btn.disabled = false;
                origBtn.style.display = "";
                if (cacheHits > 0) cacheBadge.style.display = "";
                status.style.color = failed > 0 ? "#c00" : "#1a7340";
                status.textContent = failed > 0
                    ? "⚠ Done with " + failed + " error(s)"
                    : "✓ Done — " + cacheHits + " cached, " + (missCount - failed) + " new";
            } else {
                setTimeout(dispatch, 300);
            }
        }

        function dispatch() {
            while (active < CONCURRENCY && queue.length > 0) {
                (function(text) {
                    active++;
                    var h = textMap[text].hash;
                    apiCall(text, sourceLang, targetLang, provider, function(err, translated) {
                        active--;
                        if (!err && translated && translated.trim().length > 0) {
                            var result = translated.trim();
                            newSections[h] = result;
                            textMap[text].indices.forEach(function(idx) {
                                savedNodes[idx].node.nodeValue = result;
                            });
                        } else if (err) {
                            failed++;
                        }
                        oneDone();
                    });
                })(queue.shift());
            }
        }

        dispatch();
    }

    function restoreOriginal(origBtn, status, cacheBadge) {
        savedNodes.forEach(function(o) { o.node.nodeValue = o.original; });
        savedNodes = [];
        isTranslated = false;
        origBtn.style.display = "none";
        if (cacheBadge) cacheBadge.style.display = "none";
        status.style.color = "#555";
        status.textContent = "";
    }

    // ------------------------------------------------------------------
    // API call
    // ------------------------------------------------------------------
    function apiCall(text, sourceLang, targetLang, provider, cb) {
        var xhr = new XMLHttpRequest();
        xhr.open("POST", ENDPOINT, true);
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest");
        xhr.timeout = 30000;
        xhr.ontimeout = function() { cb("timeout", null); };
        xhr.onreadystatechange = function() {
            if (xhr.readyState !== 4) return;
            if (xhr.status === 200) {
                try {
                    var d = JSON.parse(xhr.responseText);
                    cb(d.error || null, d.translatedText || "");
                } catch(e) { cb("parse error", null); }
            } else {
                try {
                    var e = JSON.parse(xhr.responseText);
                    cb(e.error || "HTTP " + xhr.status, null);
                } catch(_) { cb("HTTP " + xhr.status, null); }
            }
        };
        xhr.send(JSON.stringify({
            text: text, sourceLang: sourceLang,
            targetLang: targetLang, provider: provider
        }));
    }

    // ------------------------------------------------------------------
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", buildToolbar);
    } else {
        buildToolbar();
    }

})();
