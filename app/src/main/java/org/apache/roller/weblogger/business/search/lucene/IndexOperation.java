package org.apache.roller.weblogger.business.search.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;

/**
 * Base class for all Lucene index operations.
 * Refactored to delegate document creation and writer management.
 */
public abstract class IndexOperation implements Runnable {

    protected final LuceneIndexManager manager;

    private final LuceneWriterManager writerManager =
            new LuceneWriterManager();

    private final WeblogEntryDocumentBuilder documentBuilder =
            new WeblogEntryDocumentBuilder();

    private IndexWriter writer;

    protected IndexOperation(LuceneIndexManager manager) {
        this.manager = manager;
    }

    protected IndexWriter beginWriting() {
        writer = writerManager.openWriter(manager);
        return writer;
    }

    protected void endWriting() {
        writerManager.closeWriter(writer);
        writer = null;
    }

    protected Document getDocument(org.apache.roller.weblogger.pojos.WeblogEntry entry) {
        return documentBuilder.build(entry);
    }
    
    @Override
    public void run() {
        doRun();
    }

    protected abstract void doRun();
}
