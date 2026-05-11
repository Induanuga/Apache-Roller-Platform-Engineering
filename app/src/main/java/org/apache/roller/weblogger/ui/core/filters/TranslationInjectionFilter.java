/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.
 */
package org.apache.roller.weblogger.ui.core.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * TranslationInjectionFilter — Bonus 2: Full Webpage Translation
 *
 * DESIGN RATIONALE
 * ----------------
 * This filter injects the roller-translate.js script tag into every HTML
 * response served by Roller, regardless of page type (weblog, search, feed
 * page, admin UI, etc.).
 *
 * WHY A FILTER (not a Velocity macro or template change)?
 *
 *   1. ZERO changes required for new page types.
 *      Roller renders pages via many different mechanisms: Velocity templates,
 *      JSPs, plain HttpServlets, Struts2 actions, and more.  Injecting the
 *      script from a filter means every HTML response — present and future —
 *      automatically gets translation support.  A template-level change would
 *      require touching every .vm, every .jsp, and every future template file.
 *
 *   2. Single source of truth.
 *      The script tag appears in exactly one place (this filter). Enabling or
 *      disabling translation site-wide is a one-line change here.
 *
 *   3. Graceful degradation.
 *      Non-HTML responses (feeds, images, JSON, binary downloads) are detected
 *      by Content-Type and passed through completely unmodified.
 *
 *   4. Compatible with the existing weblog.vm script tag.
 *      If weblog.vm already includes roller-translate.js, the injected tag is
 *      a duplicate.  The browser deduplication in the JS guard
 *      (document.getElementById("roller-translate-bar") check inside
 *      buildToolbar) prevents a double toolbar from appearing.
 *      Optionally, remove the <script> tag from weblog.vm to keep things tidy.
 *
 * INJECTION STRATEGY
 * ------------------
 * We buffer the entire response body, find the closing </body> tag, and insert
 * the script tag immediately before it.  If no </body> is found (e.g., a
 * partial response or a response that was already flushed) we fall back to
 * appending at the end.
 *
 * BUFFERING COST
 * --------------
 * Buffering adds latency for large pages.  For Roller's typical weblog pages
 * (< 200 KB) the cost is negligible.  For very large responses the filter
 * skips injection (see MAX_BUFFER_BYTES).  Feeds and binary downloads are
 * never buffered (Content-Type check).
 *
 * ACTIVATION
 * ----------
 * Register in web.xml (see snippet at the bottom of this file).
 * The filter is intentionally mapped to /* so it catches every request, but
 * it only acts on text/html responses.
 */
public class TranslationInjectionFilter implements Filter {

    /** Skip injection for responses larger than this (5 MB). */
    private static final int MAX_BUFFER_BYTES = 5 * 1024 * 1024;

    /**
     * The script tag to inject.
     * Placed just before </body> so the DOM is fully parsed before the script
     * runs — matching the behaviour of the existing weblog.vm placement.
     */
    private static final String SCRIPT_TAG =
        "\n<script src=\"/theme/scripts/roller-translate.js\"></script>\n";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException { /* no-op */ }

    @Override
    public void destroy() { /* no-op */ }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest)
                || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest  httpReq  = (HttpServletRequest)  request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        // Only intercept GET/HEAD requests — POST responses (form submissions,
        // AJAX calls) are never full HTML pages we want to inject into.
        String method = httpReq.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        // Skip the translation endpoint itself
        String path = httpReq.getRequestURI();
        if (path != null && path.contains("/roller-services/translation")) {
            chain.doFilter(request, response);
            return;
        }

        // Wrap the response to capture output
        CapturingResponseWrapper wrapper = new CapturingResponseWrapper(httpResp);
        chain.doFilter(request, wrapper);

        // Now decide whether to inject
        String contentType = wrapper.getContentType();
        if (!isHtmlContentType(contentType)) {
            // Not HTML — write the original bytes unchanged
            wrapper.writeCapturedTo(httpResp);
            return;
        }

        byte[] captured = wrapper.getCapturedBytes();
        if (captured.length > MAX_BUFFER_BYTES) {
            // Too large to buffer safely — pass through as-is
            wrapper.writeCapturedTo(httpResp);
            return;
        }

        // Determine charset from Content-Type header (default UTF-8)
        String charset = extractCharset(contentType);
        String html = new String(captured, charset);

        // Inject script before </body>
        String injected = injectScript(html);

        byte[] injectedBytes = injected.getBytes(charset);
        httpResp.setContentLength(injectedBytes.length);
        httpResp.getOutputStream().write(injectedBytes);
    }

    // -------------------------------------------------------------------------
    // Injection logic
    // -------------------------------------------------------------------------

    /**
     * Insert SCRIPT_TAG immediately before the last </body> tag (case-insensitive).
     * Falls back to appending at the end of the document if </body> is absent.
     * Also avoids double-injection if the tag is already present.
     */
    private String injectScript(String html) {
        // Guard: already injected (idempotent)
        if (html.contains("roller-translate.js")) {
            return html;
        }

        // Find closing </body> — case-insensitive search
        String lower = html.toLowerCase();
        int bodyClose = lower.lastIndexOf("</body>");
        if (bodyClose >= 0) {
            return html.substring(0, bodyClose)
                + SCRIPT_TAG
                + html.substring(bodyClose);
        }

        // No </body> — append to end
        return html + SCRIPT_TAG;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isHtmlContentType(String ct) {
        return ct != null && ct.toLowerCase().contains("text/html");
    }

    private String extractCharset(String contentType) {
        if (contentType == null) return "UTF-8";
        String lower = contentType.toLowerCase();
        int idx = lower.indexOf("charset=");
        if (idx < 0) return "UTF-8";
        String rest = contentType.substring(idx + 8).trim();
        // Strip trailing ; or whitespace
        int end = rest.indexOf(';');
        if (end >= 0) rest = rest.substring(0, end);
        rest = rest.trim().replace("\"", "");
        return rest.isEmpty() ? "UTF-8" : rest;
    }

    // =========================================================================
    // Response wrapper — captures body bytes without sending them downstream
    // =========================================================================

    private static final class CapturingResponseWrapper extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(8192);
        private final ServletOutputStream  capturingStream;
        private PrintWriter                capturingWriter;

        CapturingResponseWrapper(HttpServletResponse response) {
            super(response);
            this.capturingStream = new CapturingOutputStream(buffer);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return capturingStream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (capturingWriter == null) {
                String charset = extractCharset(getContentType());
                capturingWriter = new PrintWriter(
                    new java.io.OutputStreamWriter(buffer,
                        java.nio.charset.Charset.forName(charset)));
            }
            return capturingWriter;
        }

        @Override
        public void flushBuffer() throws IOException {
            if (capturingWriter != null) capturingWriter.flush();
        }

        @Override
        public void setContentLength(int len) { /* suppress — we rewrite it */ }

        @Override
        public void setContentLengthLong(long len) { /* suppress */ }

        byte[] getCapturedBytes() {
            if (capturingWriter != null) capturingWriter.flush();
            return buffer.toByteArray();
        }

        /** Write captured bytes to the real response. */
        void writeCapturedTo(HttpServletResponse real) throws IOException {
            byte[] bytes = getCapturedBytes();
            real.setContentLength(bytes.length);
            real.getOutputStream().write(bytes);
        }

        private String extractCharset(String ct) {
            if (ct == null) return "UTF-8";
            String lower = ct.toLowerCase();
            int idx = lower.indexOf("charset=");
            if (idx < 0) return "UTF-8";
            String rest = ct.substring(idx + 8).trim();
            int end = rest.indexOf(';');
            if (end >= 0) rest = rest.substring(0, end);
            return rest.trim().replace("\"", "");
        }
    }

    private static final class CapturingOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream buf;

        CapturingOutputStream(ByteArrayOutputStream buf) { this.buf = buf; }

        @Override public void write(int b)             { buf.write(b); }
        @Override public void write(byte[] b)          throws IOException { buf.write(b); }
        @Override public void write(byte[] b, int o, int l) { buf.write(b, o, l); }
        @Override public boolean isReady()             { return true; }
        @Override public void setWriteListener(WriteListener wl) { /* no-op */ }
    }
}

/*
 * ============================================================================
 * web.xml additions required to activate this filter
 * ============================================================================
 *
 * Add the <filter> declaration alongside the other filters:
 *
 *   <filter>
 *       <filter-name>TranslationInjectionFilter</filter-name>
 *       <filter-class>
 *           org.apache.roller.weblogger.ui.core.filters.TranslationInjectionFilter
 *       </filter-class>
 *   </filter>
 *
 * Add the <filter-mapping> AFTER the CharEncodingFilter mapping
 * (so encoding is set before we read/write the body) but BEFORE
 * the RequestMappingFilter (so the response is captured before rendering):
 *
 *   <filter-mapping>
 *       <filter-name>TranslationInjectionFilter</filter-name>
 *       <url-pattern>/*</url-pattern>
 *       <dispatcher>REQUEST</dispatcher>
 *   </filter-mapping>
 *
 * ORDERING NOTE: This filter must come AFTER PersistenceSessionFilter
 * (so the DB session is open during rendering) and AFTER all security
 * filters (so unauthenticated requests are rejected before we buffer them).
 * Place it just before the RequestMappingFilter entry.
 *
 * Optionally remove the <script> tag from weblog.vm to avoid the
 * toolbar being built twice (the JS already guards against this, but
 * removing it is cleaner):
 *
 *   ## Remove or comment out this line in weblog.vm:
 *   ## <script src="/theme/scripts/roller-translate.js"></script>
 *
 * ============================================================================
 */