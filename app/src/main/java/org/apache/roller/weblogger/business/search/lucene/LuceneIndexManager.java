package org.apache.roller.weblogger.business.search.lucene;

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.*;
import org.apache.roller.weblogger.business.search.IndexManager;
import org.apache.roller.weblogger.business.search.SearchResultList;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.lucene.analysis.Analyzer;

@com.google.inject.Singleton

public class LuceneIndexManager implements IndexManager {

    private final Weblogger roller;


    private final IndexOperationExecutor executor;
    private final LuceneIndexLifecycleManager lifecycle;
    private final LuceneSearchResultMapper mapper;

    private final boolean searchEnabled;

    private final SharedIndexReaderManager readerManager;

    

    public ReadWriteLock getReadWriteLock() {
        return readerManager.getReadWriteLock();
    }

    public IndexReader getSharedIndexReader() {
        return readerManager.getSharedReader(getIndexDirectory());
    }    

    public synchronized void resetSharedReader() {
        readerManager.reset();
    }

    public static Analyzer getAnalyzer() {
        return LuceneAnalyzerFactory.createAnalyzer();
    }
    
    public Directory getIndexDirectory() {
        return lifecycle.getDirectory();
    }

    @com.google.inject.Inject
    protected LuceneIndexManager(Weblogger roller) {

        this.searchEnabled =
                !"false".equalsIgnoreCase(
                        WebloggerConfig.getProperty("search.enabled"));

        String dir =
                WebloggerConfig.getProperty("search.index.dir")
                        .replace('/', java.io.File.separatorChar);

        this.executor =
                new IndexOperationExecutor(roller, searchEnabled);
        this.lifecycle =
                new LuceneIndexLifecycleManager(dir);
        this.mapper =
                new LuceneSearchResultMapper();

        this.readerManager =
                new SharedIndexReaderManager();
        
        this.roller = roller;

    }

    @Override
    public void initialize() throws InitializationException {
        if (searchEnabled) lifecycle.initialize();
    }

    @Override
    public boolean isInconsistentAtStartup() {
        return lifecycle.isInconsistentAtStartup();
    }

    @Override
    public void rebuildWeblogIndex() throws WebloggerException {
        executor.schedule(new RebuildWebsiteIndexOperation(roller, this, null));
    }

    @Override
    public void rebuildWeblogIndex(Weblog website) throws WebloggerException {
        executor.schedule(new RebuildWebsiteIndexOperation(roller, this, website));
    }

    @Override
    public void addEntryIndexOperation(WeblogEntry entry) throws WebloggerException {
        executor.schedule(new AddEntryOperation(roller, this, entry));
    }

    @Override
    public SearchResultList search(
            String term, String weblogHandle, String category,
            String locale, int pageNum, int entryCount,
            URLStrategy urlStrategy) throws WebloggerException {

        SearchOperation search = new SearchOperation(this);
        search.setTerm(term);

        boolean weblogSpecific =
                !WebloggerRuntimeConfig.isSiteWideWeblog(weblogHandle);
        if (weblogSpecific) search.setWeblogHandle(weblogHandle);

        executor.executeNow(search);
        TopFieldDocs docs = search.getResults();
        ScoreDoc[] hits = docs.scoreDocs;

        return mapper.map(
                hits, search, pageNum, entryCount,
                weblogHandle, weblogSpecific, urlStrategy);
    }

    @Override
    public void shutdown() {
        lifecycle.clearConsistencyMarker();
        readerManager.shutdown();
    }



    @Override
    public void release() {
    }

    @Override
    public void removeWeblogIndex(Weblog website) throws WebloggerException {
        executor.schedule(new RemoveWebsiteIndexOperation(null, this, website));
    }

    @Override
    public void removeEntryIndexOperation(WeblogEntry entry)
        throws WebloggerException {
    executor.executeNow(
            new RemoveEntryOperation(roller, this, entry));
    }
    
    @Override
    public void addEntryReIndexOperation(WeblogEntry entry)
            throws WebloggerException {
        executor.schedule(
                new ReIndexEntryOperation(roller, this, entry));
    }
    
            
    
}




