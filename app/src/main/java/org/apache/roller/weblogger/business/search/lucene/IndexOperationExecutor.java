package org.apache.roller.weblogger.business.search.lucene;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.business.Weblogger;

class IndexOperationExecutor {

    private final Weblogger roller;
    private final boolean searchEnabled;
    private static final Log logger =
            LogFactory.getFactory().getInstance(IndexOperationExecutor.class);

    IndexOperationExecutor(Weblogger roller, boolean searchEnabled) {
        this.roller = roller;
        this.searchEnabled = searchEnabled;
    }

    void schedule(IndexOperation op) {
        if (!searchEnabled) return;
        try {
            logger.debug("Scheduling index operation: " + op.getClass().getName());
            roller.getThreadManager().executeInBackground(op);
        } catch (InterruptedException e) {
            logger.error("Error scheduling index operation", e);
        }
    }

    void executeNow(IndexOperation op) {
        if (!searchEnabled) return;
        try {
            logger.debug("Executing index operation: " + op.getClass().getName());
            roller.getThreadManager().executeInForeground(op);
        } catch (InterruptedException e) {
            logger.error("Error executing index operation", e);
        }
    }
}
