package org.apache.roller.weblogger.business.pipeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.pojos.WeblogEntry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pipeline Step 1: Profanity Filter
 *
 * Scans the blog entry title and text for banned words and replaces them
 * with asterisks. Uses a static word blacklist — no external API needed.
 *
 * To add/remove banned words, just modify the BANNED_WORDS set.
 * This step has zero impact on any other pipeline step.
 */
public class ProfanityFilterStep extends AbstractEntryProcessingStep {

    private static final Log log = LogFactory.getLog(ProfanityFilterStep.class);

    // Real harmful/profanity words list
    // Add or remove words freely — nothing else in the pipeline changes
    private static final Set<String> BANNED_WORDS = new HashSet<>(Arrays.asList(
        // Violence
        "murder", "kill", "assault", "massacre", "slaughter", "torture",
        "rape", "molest", "abuse", "strangle", "stab", "shoot",
        "terrorist", "extremist",
        "fuck", "fucking", "fucker", "shit", "bullshit", "bitch",
        "bastard", "asshole", "dick", "cunt", "piss", "crap",
        "damn", "hell", "ass", "prick", "whore", "slut",
        "cocaine", "heroin", "meth", "crack",
        "spam", "scam", "phishing", "clickbait"
    ));

    @Override
    public void process(WeblogEntry entry) {
        log.debug("ProfanityFilterStep: processing entry [" + entry.getId() + "]");

        if (entry.getTitle() != null) {
            entry.setTitle(filterText(entry.getTitle()));
        }

        if (entry.getText() != null) {
            entry.setText(filterText(entry.getText()));
        }

        if (entry.getSummary() != null) {
            entry.setSummary(filterText(entry.getSummary()));
        }

        // Pass to the next step in the chain
        processNext(entry);
    }

    @Override
    public String getStepName() {
        return "Profanity Filter";
    }

    /**
     * Replace each banned word (case-insensitive, whole-word match) with asterisks.
     */
    private String filterText(String text) {
        String result = text;
        for (String word : BANNED_WORDS) {
            // Whole-word, case-insensitive replacement
            String regex = "(?i)\\b" + Pattern.quote(word) + "\\b";
            String replacement = "*".repeat(word.length());
            result = result.replaceAll(regex, replacement);
        }
        return result;
    }
}