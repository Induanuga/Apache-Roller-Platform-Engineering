/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.
 */
package org.apache.roller.weblogger.ui.struts2.ajax;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.translation.TranslationException;
import org.apache.roller.weblogger.translation.TranslationProvider;
import org.apache.roller.weblogger.translation.TranslationProviderFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * REST servlet for in-page translation.
 *
 * POST /roller-services/translation
 *   Body (JSON): { "text": "...", "sourceLang": "en", "targetLang": "hi", "provider": "sarvam" }
 *   Response (JSON): { "translatedText": "...", "provider": "sarvam" }
 *
 * GET /roller-services/translation/languages
 *   Response (JSON): [ { "code": "en", "name": "English" }, ... ]
 *
 * Mapped under /roller-services/ (public) so no login is required —
 * blog readers can translate without being authenticated.
 *
 * The "provider" field is optional; omitting it uses the server-configured default.
 * Pass "provider": "openai" or "provider": "sarvam" to override per-request.
 */
public class TranslationServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(TranslationServlet.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        // Allow same-origin AJAX calls
        response.setHeader("X-Content-Type-Options", "nosniff");

        try {
            String body = readBody(request);
            String text       = extractJsonString(body, "text");
            String sourceLang = extractJsonString(body, "sourceLang");
            String targetLang = extractJsonString(body, "targetLang");
            String providerHint = extractJsonString(body, "provider"); // optional

            if (text == null || text.isEmpty()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'text' field");
                return;
            }
            if (targetLang == null || targetLang.isEmpty()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'targetLang' field");
                return;
            }
            if (sourceLang == null || sourceLang.isEmpty()) {
                sourceLang = "en"; // default source
            }

            // Allow per-request provider override
            TranslationProvider provider;
            if (providerHint != null && !providerHint.isEmpty()) {
                String savedProp = System.getProperty("translation.provider");
                System.setProperty("translation.provider", providerHint);
                TranslationProviderFactory.reset();
                provider = TranslationProviderFactory.getProvider();
                // Restore original setting
                if (savedProp != null) {
                    System.setProperty("translation.provider", savedProp);
                } else {
                    System.clearProperty("translation.provider");
                }
                TranslationProviderFactory.reset();
            } else {
                provider = TranslationProviderFactory.getProvider();
            }

            String translated = provider.translate(text, sourceLang, targetLang);
            String providerId = provider.getProviderId();

            String jsonResponse = "{"
                + "\"translatedText\": \"" + escapeJson(translated) + "\","
                + "\"provider\": \"" + escapeJson(providerId) + "\""
                + "}";

            byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLength(bytes.length);
            try (OutputStream os = response.getOutputStream()) {
                os.write(bytes);
            }

        } catch (TranslationException e) {
            log.error("Translation failed", e);
            sendError(response, HttpServletResponse.SC_BAD_GATEWAY,
                "Translation service error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in TranslationServlet", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Internal error: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        if ("/languages".equals(pathInfo)) {
            // Return supported languages from the configured provider
            TranslationProvider provider = TranslationProviderFactory.getProvider();
            List<Map<String, String>> langs = provider.getSupportedLanguages();

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < langs.size(); i++) {
                Map<String, String> lang = langs.get(i);
                sb.append("{\"code\":\"").append(escapeJson(lang.get("code"))).append("\",")
                  .append("\"name\":\"").append(escapeJson(lang.get("name"))).append("\"}");
                if (i < langs.size() - 1) sb.append(",");
            }
            sb.append("]");

            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLength(bytes.length);
            try (OutputStream os = response.getOutputStream()) {
                os.write(bytes);
            }
        } else {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Unknown path: " + pathInfo);
        }
    }

    // Allow CORS preflight for development setups
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Allow", "GET, POST, OPTIONS");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        String json = "{\"error\": \"" + escapeJson(message) + "\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpServletRequest request) throws IOException {
        try (Scanner scanner = new Scanner(request.getInputStream(), "UTF-8")) {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        }
    }

    /**
     * Minimal JSON string extractor — avoids adding a JSON library dependency.
     * Extracts the string value of a top-level key from a flat JSON object.
     */
    private String extractJsonString(String json, String key) {
        if (json == null || json.isEmpty()) return null;
        String searchKey = "\"" + key + "\"";
        int idx = json.indexOf(searchKey);
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx + searchKey.length());
        if (colon < 0) return null;
        // Skip whitespace after colon
        int pos = colon + 1;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        if (pos >= json.length()) return null;
        if (json.charAt(pos) == '"') {
            // String value
            int start = pos + 1;
            int end = start;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
                end++;
            }
            return unescapeJson(json.substring(start, end));
        } else if (json.charAt(pos) == 'n') {
            return null; // null value
        }
        return null;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }
}