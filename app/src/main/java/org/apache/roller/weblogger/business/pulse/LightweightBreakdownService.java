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

import org.apache.roller.weblogger.pojos.WeblogEntryComment;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Lightweight implementation of {@link ConversationBreakdownService}.
 *
 * Uses classical, computationally inexpensive techniques — no LLM or deep learning:
 *   1. Stop-word removal + tokenisation
 *   2. For small sets (< 5 comments): each comment becomes its own theme, labelled
 *      by its most meaningful keyword. TF-IDF is not useful with < 5 documents.
 *   3. For larger sets: TF-IDF clustering groups comments by shared top keyword.
 *   4. The recap is built from statistics + top theme names.
 *
 * Design pattern: Strategy (implements ConversationBreakdownService)
 */
public class LightweightBreakdownService implements ConversationBreakdownService {

    private static final int MAX_THEMES = 4;
    private static final int MAX_REPRESENTATIVE = 2;
    private static final int EXCERPT_LENGTH = 150;
    private static final int SMALL_SET_THRESHOLD = 5;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a","an","the","and","or","but","in","on","at","to","for","of","with",
        "is","was","are","were","be","been","being","have","has","had","do","does",
        "did","will","would","could","should","may","might","shall","can","not","no",
        "this","that","these","those","it","its","i","me","my","we","our","you","your",
        "he","she","they","their","his","her","what","which","who","how","when","where",
        "why","so","if","as","by","from","up","about","into","through","during",
        "very","just","also","more","some","such","than","then","there","out","all",
        "any","both","each","few","most","other","same","too","s","t","re",
        "ll","ve","d","m","don","didn","doesn","isn","wasn","aren","weren","won",
        "still","explain","confuses","u1","u2","u3"
    ));

    @Override
    public String getMethodName() {
        return "Lightweight (Keyword Clustering)";
    }

    @Override
    public ConversationBreakdown generate(List<WeblogEntryComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return new ConversationBreakdown(
                Collections.emptyList(), "No comments to analyse.", getMethodName());
        }

        List<ConversationBreakdown.CommentTheme> themes =
            (comments.size() < SMALL_SET_THRESHOLD)
                ? buildThemesFromSmallSet(comments)
                : buildThemesViaClustering(comments);

        if (themes.isEmpty()) {
            List<String> reps = comments.stream()
                .limit(MAX_REPRESENTATIVE)
                .map(c -> excerpt(c.getContent()))
                .collect(Collectors.toList());
            themes = Collections.singletonList(
                new ConversationBreakdown.CommentTheme("General Discussion", reps));
        }

        String recap = buildRecap(comments, themes);
        return new ConversationBreakdown(themes, recap, getMethodName());
    }

    /**
     * Small set path: each comment becomes its own theme.
     * Theme label = most meaningful word (highest raw frequency after stop-word removal),
     * falling back to first 4 words of the comment.
     */
    private List<ConversationBreakdown.CommentTheme> buildThemesFromSmallSet(
            List<WeblogEntryComment> comments) {

        List<ConversationBreakdown.CommentTheme> themes = new ArrayList<>();
        Set<String> usedLabels = new HashSet<>();

        for (WeblogEntryComment c : comments) {
            String text = c.getContent();
            List<String> tokens = tokenise(text);

            String label = tokens.stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> capitalize(e.getKey()))
                .orElse(null);

            if (label == null || usedLabels.contains(label)) {
                label = firstWords(text, 4);
            }
            usedLabels.add(label);

            themes.add(new ConversationBreakdown.CommentTheme(
                label, Collections.singletonList(excerpt(text))));
        }

        return themes.stream().limit(MAX_THEMES).collect(Collectors.toList());
    }

    /** Large set path: TF-IDF clustering. */
    private List<ConversationBreakdown.CommentTheme> buildThemesViaClustering(
            List<WeblogEntryComment> comments) {

        List<List<String>> tokenised = comments.stream()
            .map(c -> tokenise(c.getContent()))
            .collect(Collectors.toList());

        Map<String, Integer> df = computeDocumentFrequency(tokenised);

        List<String> topKeywords = new ArrayList<>();
        for (List<String> tokens : tokenised) {
            topKeywords.add(topKeywordTfIdf(tokens, df, comments.size()));
        }

        Map<String, List<Integer>> clusters = new LinkedHashMap<>();
        for (int i = 0; i < comments.size(); i++) {
            String kw = topKeywords.get(i);
            if (kw != null) clusters.computeIfAbsent(kw, k -> new ArrayList<>()).add(i);
        }

        return clusters.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
            .limit(MAX_THEMES)
            .map(entry -> {
                String label = capitalize(entry.getKey());
                List<String> reps = entry.getValue().stream()
                    .limit(MAX_REPRESENTATIVE)
                    .map(idx -> excerpt(comments.get(idx).getContent()))
                    .collect(Collectors.toList());
                return new ConversationBreakdown.CommentTheme(label, reps);
            })
            .collect(Collectors.toList());
    }

    private List<String> tokenise(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        return Arrays.stream(text.toLowerCase().split("[^a-z]+"))
            .filter(w -> w.length() > 2 && !STOP_WORDS.contains(w))
            .collect(Collectors.toList());
    }

    private Map<String, Integer> computeDocumentFrequency(List<List<String>> tokenised) {
        Map<String, Integer> df = new HashMap<>();
        for (List<String> tokens : tokenised) {
            new HashSet<>(tokens).forEach(t -> df.merge(t, 1, Integer::sum));
        }
        return df;
    }

    private String topKeywordTfIdf(List<String> tokens, Map<String, Integer> df, int N) {
        if (tokens.isEmpty()) return null;
        Map<String, Long> tf = tokens.stream()
            .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        return tf.entrySet().stream()
            .max(Comparator.comparingDouble(e -> {
                double idf = Math.log(1.0 + (double) N / df.getOrDefault(e.getKey(), 1));
                return e.getValue() * idf;
            }))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private String excerpt(String text) {
        if (text == null) return "";
        text = text.replaceAll("<[^>]+>", "").trim();
        return text.length() <= EXCERPT_LENGTH ? text : text.substring(0, EXCERPT_LENGTH) + "…";
    }

    private String firstWords(String text, int n) {
        if (text == null || text.isBlank()) return "Comment";
        text = text.replaceAll("<[^>]+>", "").trim();
        String[] words = text.split("\\s+");
        return String.join(" ", Arrays.copyOfRange(words, 0, Math.min(n, words.length)));
    }

    private String capitalize(String word) {
        if (word == null || word.isEmpty()) return word;
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    private String buildRecap(List<WeblogEntryComment> comments,
                               List<ConversationBreakdown.CommentTheme> themes) {
        int total = comments.size();
        String topTheme = themes.isEmpty() ? "various topics"
            : themes.get(0).getThemeLabel().toLowerCase();
        String allThemes = themes.stream()
            .map(t -> "\"" + t.getThemeLabel() + "\"")
            .collect(Collectors.joining(", "));
        return String.format(
            "The discussion contains %d comment%s. " +
            "Readers are primarily talking about %s. " +
            "Key topics identified: %s.",
            total, total == 1 ? "" : "s",
            topTheme, allThemes
        );
    }
}