package org.apache.roller.weblogger.business.search.lucene;

import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;

/**
 * Manages the lifecycle and concurrency control of a shared Lucene IndexReader.
 * Extracted from LuceneIndexManager to reduce cognitive complexity and
 * centralize reader-related responsibilities.
 */
class SharedIndexReaderManager {

    private static final Log logger =
            LogFactory.getFactory().getInstance(SharedIndexReaderManager.class);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private IndexReader reader;

    ReadWriteLock getReadWriteLock() {
        return lock;
    }

    synchronized IndexReader getSharedReader(Directory directory) {
        if (reader == null) {
            try {
                reader = DirectoryReader.open(directory);
            } catch (IOException e) {
                logger.error("Error opening shared IndexReader", e);
                throw new RuntimeException(e);
            }
        }
        return reader;
    }

    synchronized void reset() {
        reader = null;
    }

    synchronized void shutdown() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                logger.error("Unable to close IndexReader", e);
            } finally {
                reader = null;
            }
        }
    }
}
