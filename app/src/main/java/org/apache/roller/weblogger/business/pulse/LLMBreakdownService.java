/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.roller.weblogger.business.pulse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.pojos.WeblogEntryComment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * LLM-based implementation of {@link ConversationBreakdownService}.
 *
 * Uses the Groq API (llama-3.1-8b-instant model) which provides a generous
 * free tier with no billing required.
 *
 * Groq uses the OpenAI-compatible Chat Completions API format:
 *   POST https://api.groq.com/openai/v1/chat/completions
 *   Authorization: Bearer <api_key>
 *
 * Pipeline (selective LLM use):
 *   1. Pre-processing (classical): strip HTML, truncate, cap comment count
 *   2. Single batch Groq API call with a strict structured-output prompt
 *   3. Post-processing (classical): regex parse the response into POJOs
 *
 * Falls back to LightweightBreakdownService if the API call fails.
 *
 * Design pattern: Strategy (implements ConversationBreakdownService)
 */
public class LLMBreakdownService implements ConversationBreakdownService {

    private static final Log log = LogFactory.getLog(LLMBreakdownService.class);

    // Groq Chat Completions endpoint (OpenAI-compatible)
    private static final String GROQ_API_URL =
        "https://api.groq.com/openai/v1/chat/completions";

    // Groq model — llama-3.1-8b-instant is fast and free tier friendly
    private static final String GROQ_MODEL = "llama-3.1-8b-instant";

    private final String apiKey;
    private final ConversationBreakdownService fallback = new LightweightBreakdownService();

    private static final int MAX_COMMENT_CHARS = 300;
    private static final int MAX_COMMENTS_FOR_LLM = 30;

    public LLMBreakdownService(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getMethodName() {
        return "LLM (Groq — " + GROQ_MODEL + ")";
    }

    @Override
    public ConversationBreakdown generate(List<WeblogEntryComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return new ConversationBreakdown(
                Collections.emptyList(), "No comments to analyse.", getMethodName());
        }

        try {
            // Step 1: Pre-process (classical — cheap, no API cost)
            List<String> cleaned = preProcess(comments);

            // Step 2: Build prompt
            String prompt = buildPrompt(cleaned);

            // Step 3: Call Groq API
            String rawResponse = callGroq(prompt);
            log.info("LLMBreakdownService Groq raw response: " + rawResponse);

            // Step 4: Parse structured response (classical)
            return parseResponse(rawResponse, comments);

        } catch (Exception e) {
            log.warn("LLMBreakdownService: Groq API call failed (" + e.getMessage() + "), falling back.");
            ConversationBreakdown fb = fallback.generate(comments);
            return new ConversationBreakdown(fb.getThemes(), fb.getOverallRecap(),
                "LLM (fallback — " + e.getMessage() + ")");
        }
    }

    // -------------------------------------------------------------------------
    // Pre-processing (classical, no API cost)
    // -------------------------------------------------------------------------

    private List<String> preProcess(List<WeblogEntryComment> comments) {
        List<WeblogEntryComment> subset = comments.size() > MAX_COMMENTS_FOR_LLM
            ? comments.subList(comments.size() - MAX_COMMENTS_FOR_LLM, comments.size())
            : comments;

        return subset.stream()
            .map(c -> {
                String text = c.getContent();
                if (text == null) return "";
                text = text.replaceAll("<[^>]+>", "").trim();   // strip HTML
                text = text.replaceAll("\\s+", " ");             // collapse whitespace
                if (text.length() > MAX_COMMENT_CHARS)
                    text = text.substring(0, MAX_COMMENT_CHARS) + "…";
                return text;
            })
            .filter(t -> !t.isBlank())
            .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Prompt — strict format for reliable parsing
    // -------------------------------------------------------------------------

    private String buildPrompt(List<String> comments) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyse the following blog post comments and respond in EXACTLY this format. ");
        sb.append("Do not add anything outside this format. Do not use markdown.\n\n");
        sb.append("RECAP: [2-3 sentences summarising what readers are discussing]\n\n");
        sb.append("THEME 1: [theme label]\n");
        sb.append("REP: [short representative excerpt from a comment, max 100 chars]\n");
        sb.append("REP: [another short excerpt, optional]\n\n");
        sb.append("THEME 2: [theme label]\n");
        sb.append("REP: [short representative excerpt]\n\n");
        sb.append("THEME 3: [theme label]\n");
        sb.append("REP: [short representative excerpt]\n\n");
        sb.append("Rules:\n");
        sb.append("- Always start with RECAP\n");
        sb.append("- Identify 2 to 4 THEME blocks\n");
        sb.append("- Each THEME must have 1-2 REP lines using actual words from the comments\n");
        sb.append("- Keep REP excerpts under 100 characters\n");
        sb.append("- Do NOT use bullet points, asterisks, or markdown\n\n");
        sb.append("COMMENTS:\n");
        for (int i = 0; i < comments.size(); i++) {
            sb.append((i + 1)).append(". ").append(comments.get(i)).append("\n");
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Groq API call — OpenAI-compatible Chat Completions format
    // -------------------------------------------------------------------------

    private String callGroq(String prompt) throws Exception {
        URL url = new URL(GROQ_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        // Build OpenAI-compatible JSON request body
        String jsonBody = "{"
            + "\"model\": " + jsonStr(GROQ_MODEL) + ","
            + "\"messages\": ["
            +   "{"
            +     "\"role\": \"system\","
            +     "\"content\": \"You are a helpful assistant that analyses blog comments and responds only in the exact structured format requested. No markdown, no extra text.\""
            +   "},"
            +   "{"
            +     "\"role\": \"user\","
            +     "\"content\": " + jsonStr(prompt)
            +   "}"
            + "],"
            + "\"temperature\": 0.3,"
            + "\"max_tokens\": 800"
            + "}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            StringBuilder err = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) err.append(line);
            }
            throw new RuntimeException("Groq HTTP " + status + ": " + err);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) response.append(line).append("\n");
        }

        return extractTextFromGroqResponse(response.toString());
    }

    /**
     * Extracts the assistant message content from Groq's OpenAI-compatible response.
     *
     * Response shape:
     * {
     *   "choices": [ { "message": { "role": "assistant", "content": "..." } } ]
     * }
     */
    private String extractTextFromGroqResponse(String json) {
        // Match "content": "..." — the assistant's reply
        Pattern p = Pattern.compile("\"content\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(json);
        // Skip the system/user content fields — we want the assistant's content
        // which appears after "role": "assistant"
        int assistantIdx = json.indexOf("\"assistant\"");
        if (assistantIdx == -1) {
            // Fallback: just grab the last "content" field
            String last = "";
            while (m.find()) last = m.group(1);
            return last.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim();
        }

        // Find "content" after the "assistant" marker
        Matcher m2 = Pattern.compile("\"content\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
            .matcher(json.substring(assistantIdx));
        if (m2.find()) {
            return m2.group(1)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim();
        }
        return "";
    }

    // -------------------------------------------------------------------------
    // Response parsing — matches the strict prompt format
    // -------------------------------------------------------------------------

    private ConversationBreakdown parseResponse(String text,
                                                 List<WeblogEntryComment> originalComments) {
        if (text == null || text.isBlank()) {
            log.warn("LLMBreakdownService: empty Groq response, falling back.");
            return fallbackResult(originalComments);
        }

        // Strip any accidental markdown (Groq sometimes adds it despite instructions)
        text = text.replaceAll("\\*\\*", "").replaceAll("^#+\\s*", "");

        // Extract RECAP
        String recap = "";
        Matcher recapM = Pattern.compile("(?i)RECAP:\\s*(.+?)(?=\\nTHEME\\s*\\d+:|$)",
            Pattern.DOTALL).matcher(text);
        if (recapM.find()) {
            recap = recapM.group(1).trim();
        }

        // Extract THEME N blocks
        List<ConversationBreakdown.CommentTheme> themes = new ArrayList<>();
        Matcher themeM = Pattern.compile(
            "(?i)THEME\\s*\\d+:\\s*(.+?)(?=\\nTHEME\\s*\\d+:|$)",
            Pattern.DOTALL).matcher(text);

        while (themeM.find()) {
            String block = themeM.group(0);
            String label = themeM.group(1).lines().findFirst().orElse("").trim();
            // Strip any stray markdown from label
            label = label.replaceAll("\\*", "").trim();
            if (label.isBlank()) continue;

            List<String> reps = new ArrayList<>();
            Matcher repM = Pattern.compile("(?i)REP:\\s*(.+)").matcher(block);
            while (repM.find()) {
                String r = repM.group(1).trim().replaceAll("\\*", "");
                if (!r.isBlank()) reps.add(r);
            }

            themes.add(new ConversationBreakdown.CommentTheme(label, reps));
        }

        if (themes.isEmpty()) {
            log.warn("LLMBreakdownService: could not parse Groq response themes. Raw: " + text);
            return fallbackResult(originalComments);
        }

        return new ConversationBreakdown(themes, recap, getMethodName());
    }

    private ConversationBreakdown fallbackResult(List<WeblogEntryComment> comments) {
        ConversationBreakdown fb = fallback.generate(comments);
        return new ConversationBreakdown(fb.getThemes(), fb.getOverallRecap(),
            "LLM (response parse failed — showing lightweight result)");
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /** Wraps a Java string as a JSON string literal with proper escaping. */
    private String jsonStr(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }
}