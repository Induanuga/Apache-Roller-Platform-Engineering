/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.
 */
package org.apache.roller.weblogger.translation.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.translation.TranslationException;
import org.apache.roller.weblogger.translation.TranslationProvider;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Translation provider using Sarvam AI's translate endpoint.
 * Model: saaras:v2 (Sarvam's document translation model)
 * API docs: https://docs.sarvam.ai/api-reference-docs/endpoints/translate
 *
 * API Key is read from system property: translation.sarvam.apikey
 * or falls back to the hardcoded default below.
 */
public class SarvamTranslationProvider implements TranslationProvider {

    private static final Log log = LogFactory.getLog(SarvamTranslationProvider.class);

    private static final String SARVAM_API_URL = "https://api.sarvam.ai/translate";

    // Fallback key - override via system property translation.sarvam.apikey
    private static final String DEFAULT_API_KEY = "sk_eoxcj8gq_F18FOebkHgNCZgdw7JRcEhA0";

    @Override
    public String getProviderId() {
        return "sarvam";
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang) throws TranslationException {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String apiKey = System.getProperty("translation.sarvam.apikey", DEFAULT_API_KEY);

        // Sarvam uses language codes like "en-IN", "hi-IN", "ta-IN" etc.
        String sarvamSource = toSarvamCode(sourceLang);
        String sarvamTarget = toSarvamCode(targetLang);

        try {
            // Split text into chunks of <=900 chars (Sarvam limit per request)
            List<String> chunks = splitIntoChunks(text, 900);
            StringBuilder result = new StringBuilder();

            for (String chunk : chunks) {
                String translated = callSarvamAPI(chunk, sarvamSource, sarvamTarget, apiKey);
                result.append(translated);
            }

            return result.toString();

        } catch (TranslationException e) {
            throw e;
        } catch (Exception e) {
            throw new TranslationException("Sarvam translation failed: " + e.getMessage(), e);
        }
    }

    private String callSarvamAPI(String text, String sourceCode, String targetCode, String apiKey)
            throws TranslationException {
        try {
            URL url = new URL(SARVAM_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("api-subscription-key", apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            // Build JSON payload - Sarvam translate API does not take a "model" field
            String escapedText = escapeJson(text);
            String payload = "{"
                + "\"input\": \"" + escapedText + "\","
                + "\"source_language_code\": \"" + sourceCode + "\","
                + "\"target_language_code\": \"" + targetCode + "\","
                + "\"speaker_gender\": \"Male\","
                + "\"mode\": \"formal\","
                + "\"enable_preprocessing\": false"
                + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                String errorBody = readStream(conn.getErrorStream());
                throw new TranslationException("Sarvam API error " + status + ": " + errorBody);
            }

            String responseBody = readStream(conn.getInputStream());
            return extractTranslatedText(responseBody);

        } catch (TranslationException e) {
            throw e;
        } catch (Exception e) {
            throw new TranslationException("HTTP error calling Sarvam API: " + e.getMessage(), e);
        }
    }

    /**
     * Extract "translated_text" field from Sarvam JSON response.
     * Avoids pulling in a full JSON library dependency.
     */
    private String extractTranslatedText(String json) throws TranslationException {
        String key = "\"translated_text\"";
        int idx = json.indexOf(key);
        if (idx < 0) {
            throw new TranslationException("Unexpected Sarvam response (no translated_text): " + json);
        }
        int colon = json.indexOf(":", idx + key.length());
        int quoteStart = json.indexOf("\"", colon + 1);
        if (quoteStart < 0) {
            throw new TranslationException("Malformed Sarvam response: " + json);
        }
        // Find end quote, respecting escaped quotes
        int quoteEnd = quoteStart + 1;
        while (quoteEnd < json.length()) {
            if (json.charAt(quoteEnd) == '"' && json.charAt(quoteEnd - 1) != '\\') {
                break;
            }
            quoteEnd++;
        }
        return unescapeJson(json.substring(quoteStart + 1, quoteEnd));
    }

    private String readStream(java.io.InputStream is) {
        if (is == null) return "";
        try (Scanner scanner = new Scanner(is, "UTF-8")) {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        }
    }

    private List<String> splitIntoChunks(String text, int maxLen) {
        List<String> chunks = new ArrayList<>();
        if (text.length() <= maxLen) {
            chunks.add(text);
            return chunks;
        }
        // Split on sentence boundaries where possible
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLen, text.length());
            if (end < text.length()) {
                // Try to break at last sentence boundary
                int lastPeriod = text.lastIndexOf(". ", end);
                if (lastPeriod > start + maxLen / 2) {
                    end = lastPeriod + 2;
                }
            }
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }

    private String escapeJson(String s) {
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

    /**
     * Map generic BCP-47 codes to Sarvam format (xx-IN).
     */
    private String toSarvamCode(String lang) {
        if (lang == null) return "en-IN";
        switch (lang.toLowerCase()) {
            case "en": case "en-in": return "en-IN";
            case "hi": case "hi-in": return "hi-IN";
            case "ta": case "ta-in": return "ta-IN";
            case "te": case "te-in": return "te-IN";
            case "kn": case "kn-in": return "kn-IN";
            case "ml": case "ml-in": return "ml-IN";
            case "mr": case "mr-in": return "mr-IN";
            case "bn": case "bn-in": return "bn-IN";
            case "gu": case "gu-in": return "gu-IN";
            case "pa": case "pa-in": return "pa-IN";
            case "or": case "or-in": return "od-IN"; // Odia
            default: return lang.contains("-") ? lang : lang + "-IN";
        }
    }

    @Override
    public List<Map<String, String>> getSupportedLanguages() {
        List<Map<String, String>> langs = new ArrayList<>();
        String[][] supported = {
            {"en", "English"},
            {"hi", "Hindi"},
            {"ta", "Tamil"},
            {"te", "Telugu"},
            {"kn", "Kannada"},
            {"ml", "Malayalam"},
            {"mr", "Marathi"},
            {"bn", "Bengali"},
            {"gu", "Gujarati"},
            {"pa", "Punjabi"}
        };
        for (String[] pair : supported) {
            Map<String, String> m = new HashMap<>();
            m.put("code", pair[0]);
            m.put("name", pair[1]);
            langs.add(m);
        }
        return langs;
    }
}