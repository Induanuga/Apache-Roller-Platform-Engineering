package org.apache.roller.weblogger.business.search.lucene;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for index operations that modify the index.
 * Ensures execution under an exclusive write lock and
 * resets shared readers after completion.
 */
public abstract class WriteToIndexOperation extends IndexOperation {

    private static final Log logger =
            LogFactory.getFactory().getInstance(WriteToIndexOperation.class);

    protected WriteToIndexOperation(LuceneIndexManager mgr) {
        super(mgr);
    }

    /**
     * Hook for acquiring the write lock.
     */
    protected void acquireWriteLock() {
        manager.getReadWriteLock().writeLock().lock();
    }

    /**
     * Hook for releasing the write lock.
     */
    protected void releaseWriteLock() {
        manager.getReadWriteLock().writeLock().unlock();
    }

    /**
     * Hook for post-write cleanup.
     * Write operations must invalidate shared readers.
     */
    protected void afterWrite() {
        manager.resetSharedReader();
    }

    @Override
    public void run() {
        try {
            acquireWriteLock();
            logger.debug("Starting index write operation");
            doRun();
            logger.debug("Index write operation complete");

        } catch (Exception e) {
            logger.error("Error during write index operation", e);

        } finally {
            releaseWriteLock();
            afterWrite();
        }
    }
}
