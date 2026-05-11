package org.apache.roller.weblogger.business.search.lucene;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for index operations that require read-only access.
 * Ensures execution under a shared read lock.
 */
public abstract class ReadFromIndexOperation extends IndexOperation {

    private static final Log logger =
            LogFactory.getFactory().getInstance(ReadFromIndexOperation.class);

    protected ReadFromIndexOperation(LuceneIndexManager mgr) {
        super(mgr);
    }

    /**
     * Hook method for acquiring the read lock.
     * Extracted to reduce direct coupling to lock implementation.
     */
    protected void acquireReadLock() {
        manager.getReadWriteLock().readLock().lock();
    }

    /**
     * Hook method for releasing the read lock.
     */
    protected void releaseReadLock() {
        manager.getReadWriteLock().readLock().unlock();
    }

    @Override
    public void run() {
        try {
            acquireReadLock();
            doRun();
        } catch (Exception e) {
            logger.error("Error during read index operation", e);
        } finally {
            releaseReadLock();
        }
    }
}
