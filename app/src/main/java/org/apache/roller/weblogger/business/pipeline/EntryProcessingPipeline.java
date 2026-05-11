package org.apache.roller.weblogger.business.pipeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.pojos.WeblogEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * EntryProcessingPipeline
 *
 * Assembles the Chain of Responsibility and runs it on a WeblogEntry.
 *
 * HOW TO ADD A NEW STEP:
 *   1. Create a new class extending AbstractEntryProcessingStep
 *   2. Add it to the buildDefaultPipeline() list below
 *   That's it. No other class needs to change.
 *
 * HOW TO REMOVE A STEP:
 *   Simply remove it from the list in buildDefaultPipeline().
 *   No other class needs to change.
 *
 * This class is the ONLY place that knows the pipeline order.
 */
public class EntryProcessingPipeline {

    private static final Log log = LogFactory.getLog(EntryProcessingPipeline.class);

    private final EntryProcessingStep head;
    private final List<String> stepNames;

    /**
     * Build the pipeline with the default set of steps.
     * The order here is the order of execution.
     */
    public static EntryProcessingPipeline buildDefaultPipeline() {
        List<AbstractEntryProcessingStep> steps = new ArrayList<>();

        // Step 1: Reading time estimator — runs first on original full text
        steps.add(new ReadingTimeEstimatorStep());

        // Step 2: Profanity filter — cleans content
        steps.add(new ProfanityFilterStep());

        // Step 3: Summarizer condenses long posts
        steps.add(new TextSummarizerStep());

        // Step 4: Auto tag generator — runs on already-summarized content
        steps.add(new AutoTagGeneratorStep());

        // ---------------------------------------------------------------
        // TO ADD A NEW STEP: just add it here, e.g.:
        //   steps.add(new SpamDetectionStep());
        // ---------------------------------------------------------------

        return new EntryProcessingPipeline(steps);
    }

    /**
     * Constructor: links all steps into a chain.
     */
    private EntryProcessingPipeline(List<AbstractEntryProcessingStep> steps) {
        this.stepNames = new ArrayList<>();

        if (steps == null || steps.isEmpty()) {
            this.head = null;
            return;
        }

        // Wire up the chain: each step points to the next
        for (int i = 0; i < steps.size() - 1; i++) {
            steps.get(i).setNext(steps.get(i + 1));
        }

        this.head = steps.get(0);

        for (AbstractEntryProcessingStep step : steps) {
            stepNames.add(step.getStepName());
        }
    }

    /**
     * Run the full pipeline on the given entry.
     * Call this just before saving the entry.
     *
     * @param entry the WeblogEntry to process (mutated in place)
     */
    public void process(WeblogEntry entry) {
        if (head == null) {
            log.debug("EntryProcessingPipeline: no steps configured, skipping.");
            return;
        }
        log.info("EntryProcessingPipeline: running " + stepNames.size()
            + " steps on entry [" + entry.getId() + "]: " + stepNames);
        head.process(entry);
        log.info("EntryProcessingPipeline: finished processing entry [" + entry.getId() + "]");
    }

    /**
     * Returns the names of all steps in order (useful for admin display).
     */
    public List<String> getStepNames() {
        return stepNames;
    }
}