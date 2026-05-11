package org.apache.roller.weblogger.business.pipeline;

import org.apache.roller.weblogger.pojos.WeblogEntry;

/**
 * Abstract base class for all pipeline steps.
 *
 * Handles the boilerplate of storing and calling the next step,
 * so concrete steps only need to implement their own logic.
 */
public abstract class AbstractEntryProcessingStep implements EntryProcessingStep {

    private EntryProcessingStep next;

    @Override
    public void setNext(EntryProcessingStep next) {
        this.next = next;
    }

    /**
     * Subclasses call this at the end of their process() to continue the chain.
     */
    protected void processNext(WeblogEntry entry) {
        if (next != null) {
            next.process(entry);
        }
    }
}