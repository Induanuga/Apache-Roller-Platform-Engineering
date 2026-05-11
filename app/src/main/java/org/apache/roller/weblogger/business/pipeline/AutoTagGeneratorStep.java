// package org.apache.roller.weblogger.business.pipeline;

// import org.apache.commons.logging.Log;
// import org.apache.commons.logging.LogFactory;
// import org.apache.roller.weblogger.pojos.WeblogEntry;

// import java.util.*;
// import java.util.stream.Collectors;

// /**
//  * Pipeline Step 2: Auto Tag Generator
//  *
//  * Extracts the top N keywords from the blog entry text using simple
//  * word frequency analysis (TF-style). Tags are added via Roller's
//  * native tag system so they appear properly in the tag cloud.
//  *
//  * No external API required -- purely classical NLP.
//  */
// public class AutoTagGeneratorStep extends AbstractEntryProcessingStep {

//     private static final Log log = LogFactory.getLog(AutoTagGeneratorStep.class);

//     private static final int MAX_TAGS = 5;
//     private static final int MIN_WORD_LENGTH = 4;

//     private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
//         "the", "and", "for", "that", "this", "with", "have", "from",
//         "they", "will", "been", "were", "when", "what", "your", "are",
//         "not", "but", "can", "all", "also", "into", "more", "than",
//         "then", "some", "their", "there", "which", "about", "would",
//         "other", "these", "those", "just", "like", "over", "such",
//         "very", "even", "only", "both", "each", "here", "after",
//         "its", "our", "out", "has", "was", "one", "you", "him",
//         "her", "his", "she", "who", "how", "any", "being", "should",
//         "could", "where", "while", "through", "between", "because"
//     ));

//     @Override
//     public void process(WeblogEntry entry) {
//         log.debug("AutoTagGeneratorStep: processing entry [" + entry.getId() + "]");

//         String rawText = buildRawText(entry);
//         if (rawText == null || rawText.isBlank()) {
//             processNext(entry);
//             return;
//         }

//         List<String> generatedTags = extractTopKeywords(rawText, MAX_TAGS);

//         if (!generatedTags.isEmpty()) {
//             try {
//                 // Use Roller's native tag system -- tags appear in tag cloud
//                 // and post footer, no raw HTML injected into the body
//                 String existingTagsStr = entry.getTagsAsString();
//                 Set<String> allTags = new LinkedHashSet<>();

//                 if (existingTagsStr != null && !existingTagsStr.isBlank()) {
//                     allTags.addAll(Arrays.asList(existingTagsStr.trim().split("\\s+")));
//                 }
//                 allTags.addAll(generatedTags);

//                 entry.setTagsAsString(String.join(" ", allTags));
//                 log.debug("AutoTagGeneratorStep: set tags: " + allTags);

//             } catch (Exception e) {
//                 log.error("AutoTagGeneratorStep: failed to set tags on entry", e);
//             }
//         }

//         processNext(entry);
//     }

//     @Override
//     public String getStepName() {
//         return "Auto Tag Generator";
//     }

//     private String buildRawText(WeblogEntry entry) {
//         StringBuilder sb = new StringBuilder();
//         if (entry.getTitle() != null) sb.append(entry.getTitle()).append(" ");
//         if (entry.getText() != null) sb.append(entry.getText()).append(" ");
//         if (entry.getSummary() != null) sb.append(entry.getSummary());
//         return sb.toString();
//     }

//     private List<String> extractTopKeywords(String text, int topN) {
//         String cleaned = text.replaceAll("<[^>]*>", " ")
//                              .replaceAll("[^a-zA-Z\\s]", " ")
//                              .toLowerCase();

//         Map<String, Integer> freq = new HashMap<>();
//         for (String word : cleaned.split("\\s+")) {
//             if (word.length() >= MIN_WORD_LENGTH && !STOP_WORDS.contains(word)) {
//                 freq.merge(word, 1, Integer::sum);
//             }
//         }

//         return freq.entrySet().stream()
//             .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
//             .limit(topN)
//             .map(Map.Entry::getKey)
//             .collect(Collectors.toList());
//     }
// }

package org.apache.roller.weblogger.business.pipeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.pojos.WeblogEntry;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pipeline Step 2: Auto Tag Generator
 *
 * Extracts the top N keywords from the blog entry text using simple
 * word frequency analysis (TF-style). Tags are appended as plain text
 * at the end of the entry body.
 *
 * No external API required -- purely classical NLP.
 */
public class AutoTagGeneratorStep extends AbstractEntryProcessingStep {

    private static final Log log = LogFactory.getLog(AutoTagGeneratorStep.class);

    private static final int MAX_TAGS = 5;
    private static final int MIN_WORD_LENGTH = 4;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "the", "and", "for", "that", "this", "with", "have", "from",
        "they", "will", "been", "were", "when", "what", "your", "are",
        "not", "but", "can", "all", "also", "into", "more", "than",
        "then", "some", "their", "there", "which", "about", "would",
        "other", "these", "those", "just", "like", "over", "such",
        "very", "even", "only", "both", "each", "here", "after",
        "its", "our", "out", "has", "was", "one", "you", "him",
        "her", "his", "she", "who", "how", "any", "being", "should",
        "could", "where", "while", "through", "between", "because"
    ));

    @Override
    public void process(WeblogEntry entry) {
        log.debug("AutoTagGeneratorStep: processing entry [" + entry.getId() + "]");

        String rawText = buildRawText(entry);
        if (rawText == null || rawText.isBlank()) {
            processNext(entry);
            return;
        }

        List<String> generatedTags = extractTopKeywords(rawText, MAX_TAGS);

        if (!generatedTags.isEmpty()) {
            // Append tags as plain text at the bottom of the entry
            // Simple and reliable -- no dependency on website/locale being set
            String tagLine = "\n\nTags: "
                + generatedTags.stream()
                    .map(t -> "#" + t)
                    .collect(Collectors.joining(" "));

            String existingText = entry.getText() != null ? entry.getText() : "";
            entry.setText(existingText + tagLine);

            log.debug("AutoTagGeneratorStep: appended tags: " + generatedTags);
        }

        processNext(entry);
    }

    @Override
    public String getStepName() {
        return "Auto Tag Generator";
    }

    private String buildRawText(WeblogEntry entry) {
        StringBuilder sb = new StringBuilder();
        if (entry.getTitle() != null) sb.append(entry.getTitle()).append(" ");
        if (entry.getText() != null) sb.append(entry.getText()).append(" ");
        if (entry.getSummary() != null) sb.append(entry.getSummary());
        return sb.toString();
    }

    private List<String> extractTopKeywords(String text, int topN) {
        String cleaned = text.replaceAll("<[^>]*>", " ")
                             .replaceAll("[^a-zA-Z\\s]", " ")
                             .toLowerCase();

        Map<String, Integer> freq = new HashMap<>();
        for (String word : cleaned.split("\\s+")) {
            if (word.length() >= MIN_WORD_LENGTH && !STOP_WORDS.contains(word)) {
                freq.merge(word, 1, Integer::sum);
            }
        }

        return freq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(topN)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
}