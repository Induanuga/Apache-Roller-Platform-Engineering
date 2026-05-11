package org.apache.roller.weblogger.business.pipeline;

import org.apache.roller.weblogger.pojos.WeblogEntry;

/**
 * Represents a single step in the blog post processing pipeline.
 * 
 * Chain of Responsibility Pattern:
 * Each step processes the entry and passes it to the next step.
 * Steps can be added or removed without affecting any other step.
 */
public interface EntryProcessingStep {

    /**
     * Process the entry and pass to the next step if present.
     * @param entry the blog entry being processed
     */
    void process(WeblogEntry entry);

    /**
     * Set the next step in the pipeline chain.
     * @param next the next EntryProcessingStep
     */
    void setNext(EntryProcessingStep next);

    /**
     * Get the name of this step (for logging/admin display).
     */
    String getStepName();
}