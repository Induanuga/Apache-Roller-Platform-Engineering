package org.apache.roller.weblogger.business.pipeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.pojos.WeblogEntry;

/**
 * Pipeline Step 4: Reading Time Estimator
 *
 * Calculates the estimated reading time of the blog post and prepends
 * a small "X min read" badge to the top of the entry -- just like Medium.
 *
 * Based on average adult reading speed of 200-250 words per minute.
 *
 * To change the reading speed, update WORDS_PER_MINUTE below.
 * Adding or removing this step has no effect on any other step.
 */
public class ReadingTimeEstimatorStep extends AbstractEntryProcessingStep {

    private static final Log log = LogFactory.getLog(ReadingTimeEstimatorStep.class);

    // Average adult reading speed (words per minute)
    private static final int WORDS_PER_MINUTE = 225;

    @Override
    public void process(WeblogEntry entry) {
        log.debug("ReadingTimeEstimatorStep: processing entry [" + entry.getId() + "]");

        String text = entry.getText();
        if (text == null || text.isBlank()) {
            processNext(entry);
            return;
        }

        String plainText = text.replaceAll("<[^>]*>", " ")
                               .replaceAll("\\s+", " ")
                               .trim();

        int wordCount = plainText.split("\\s+").length;
        int minutes = Math.max(1, (int) Math.ceil((double) wordCount / WORDS_PER_MINUTE));

        String badge = "[ " + minutes + " min read | " + wordCount + " words ]\n\n";

        // Prepend badge to the top of the entry
        entry.setText(badge + text);

        log.debug("ReadingTimeEstimatorStep: " + wordCount + " words, ~" + minutes + " min read");

        processNext(entry);
    }

    @Override
    public String getStepName() {
        return "Reading Time Estimator";
    }
}