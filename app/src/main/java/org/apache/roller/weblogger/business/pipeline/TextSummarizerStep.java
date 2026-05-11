package org.apache.roller.weblogger.business.pipeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.pojos.WeblogEntry;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Pipeline Step 3: Text Summarizer
 *
 * Generates a short summary of the blog post and stores it in the
 * entry's dedicated summary field (not the body). The body retains
 * the full profanity-filtered text untouched.
 *
 * Which method is used is controlled by roller-custom.properties:
 *   pipeline.summarizer.mode=extractive   (default, no API needed)
 *   pipeline.summarizer.mode=groq         (requires groq API key)
 *   pipeline.summarizer.groq.apikey=YOUR_KEY_HERE
 *   pipeline.summarizer.maxwords=200
 */
public class TextSummarizerStep extends AbstractEntryProcessingStep {

    private static final Log log = LogFactory.getLog(TextSummarizerStep.class);

    private static final int DEFAULT_MAX_WORDS = 200;
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama3-8b-8192";

    @Override
    public void process(WeblogEntry entry) {
        log.debug("TextSummarizerStep: processing entry [" + entry.getId() + "]");

        String text = entry.getText();
        if (text == null || text.isBlank()) {
            processNext(entry);
            return;
        }

        int maxWords = WebloggerConfig.getIntProperty(
            "pipeline.summarizer.maxwords", DEFAULT_MAX_WORDS);

        String textWithoutBadge = text.replaceAll("\\[\\s*\\d+\\s*min read\\s*\\|\\s*\\d+\\s*words\\s*\\]", "").trim();
        String plainText = stripAll(textWithoutBadge);
            

        // Split into individual words
        String[] words = plainText.trim().split("\\s+");

        // Only summarize if the entry exceeds the max word count
        if (words.length <= maxWords) {
            log.debug("TextSummarizerStep: entry within word limit ("
                + words.length + " words), skipping.");
            processNext(entry);
            return;
        }

        log.debug("TextSummarizerStep: entry has " + words.length
            + " words, summarizing to " + maxWords);

        String mode = WebloggerConfig.getProperty("pipeline.summarizer.mode", "extractive");

        String summary;
        if ("groq".equalsIgnoreCase(mode)) {
            summary = summarizeWithGroq(plainText, maxWords);
        } else {
            // Pass the clean words array directly — guaranteed max N words
            summary = extractiveSummarize(words, maxWords);
        }

        if (summary != null && !summary.isBlank()) {
            // Hard enforce word limit one final time just to be safe
            summary = enforceWordLimit(summary, maxWords);
            entry.setSummary(summary);
            log.debug("TextSummarizerStep: summary stored ("
                + summary.split("\\s+").length + " words)");
        }

        // Body is NOT modified — full filtered text stays in entry.getText()
        processNext(entry);
    }

    @Override
    public String getStepName() {
        return "Text Summarizer";
    }

    // -------------------------------------------------------------------------
    // Extractive: takes exactly the first N clean words
    // -------------------------------------------------------------------------
    private String extractiveSummarize(String[] words, int maxWords) {
        int limit = Math.min(maxWords, words.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(" ");
            sb.append(words[i]);
        }
        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Hard word limit enforcer — trims any summary that is too long
    // -------------------------------------------------------------------------
    private String enforceWordLimit(String text, int maxWords) {
        String[] words = text.trim().split("\\s+");
        if (words.length <= maxWords) {
            return text.trim();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) sb.append(" ");
            sb.append(words[i]);
        }
        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Strip everything: HTML tags, HTML entities, extra whitespace
    // Produces clean plain text words only
    // -------------------------------------------------------------------------
    private String stripAll(String html) {
        return html
            .replaceAll("<[^>]*>", " ")          // remove HTML tags
            .replaceAll("&[a-zA-Z0-9#]+;", " ")  // remove HTML entities &nbsp; &#128336; etc
            .replaceAll("[\\r\\n\\t]+", " ")      // remove newlines and tabs
            .replaceAll("\\s+", " ")              // collapse multiple spaces
            .trim();
    }

    // -------------------------------------------------------------------------
    // Groq LLM: abstractive summary via free-tier Groq API
    // -------------------------------------------------------------------------
    private String summarizeWithGroq(String text, int maxWords) {
        String apiKey = WebloggerConfig.getProperty("pipeline.summarizer.groq.apikey", "");
        if (apiKey.isBlank()) {
            log.warn("TextSummarizerStep: Groq API key not set, falling back to extractive.");
            return extractiveSummarize(text.split("\\s+"), maxWords);
        }

        try {
            String prompt = "Summarize the following blog post in at most " + maxWords
                + " words. Return only the summary text, no extra commentary.\n\n" + text;

            String requestBody = "{"
                + "\"model\":\"" + GROQ_MODEL + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":"
                + escapeJson(prompt) + "}],"
                + "\"max_tokens\":" + (maxWords * 2)
                + "}";

            HttpURLConnection conn = (HttpURLConnection) new URL(GROQ_API_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                String response;
                try (Scanner sc = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    response = sc.useDelimiter("\\A").next();
                }
                return parseGroqResponse(response);
            } else {
                log.warn("TextSummarizerStep: Groq API returned status " + status
                    + ", falling back to extractive.");
            }

        } catch (Exception e) {
            log.error("TextSummarizerStep: Groq API call failed, falling back to extractive.", e);
        }

        return extractiveSummarize(text.split("\\s+"), maxWords);
    }

    private String parseGroqResponse(String json) {
        String marker = "\"content\":\"";
        int start = json.indexOf(marker);
        if (start == -1) return null;
        start += marker.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end)
            .replace("\\n", "\n")
            .replace("\\\"", "\"");
    }

    private String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r") + "\"";
    }
}