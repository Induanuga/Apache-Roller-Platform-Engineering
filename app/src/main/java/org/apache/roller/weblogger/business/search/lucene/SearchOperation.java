package org.apache.roller.weblogger.business.search.lucene;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.roller.weblogger.business.search.IndexManager;

public class SearchOperation extends ReadFromIndexOperation {

    private static final Log logger =
            LogFactory.getFactory().getInstance(SearchOperation.class);

    private static final Sort SORTER =
            new Sort(new SortField(
                    FieldConstants.PUBLISHED,
                    SortField.Type.STRING,
                    true));

    private final LuceneQueryBuilder queryBuilder =
            new LuceneQueryBuilder();

    private IndexSearcher searcher;
    private TopFieldDocs searchresults;

    private String term;
    private String weblogHandle;
    private String category;
    private String locale;
    private String parseError;

    public SearchOperation(IndexManager mgr) {
        super((LuceneIndexManager) mgr);
    }

    public void setTerm(String term) {
        this.term = term;
    }

    @Override
    public void doRun() {
        final int docLimit = 500;
        searchresults = null;
        searcher = null;

        try {
            IndexReader reader = manager.getSharedIndexReader();
            searcher = new IndexSearcher(reader);

            Query query = queryBuilder.buildQuery(
                    term, weblogHandle, category, locale);

            searchresults =
                    searcher.search(query, docLimit, SORTER);

        } catch (IOException e) {
            logger.error("Error searching index", e);
            parseError = e.getMessage();

        } catch (Exception e) {
            parseError = e.getMessage();
        }
    }

    public IndexSearcher getSearcher() {
        return searcher;
    }

    public TopFieldDocs getResults() {
        return searchresults;
    }

    public int getResultsCount() {
        if (searchresults == null) {
            return -1;
        }
        return (int) searchresults.totalHits.value;
    }

    public String getParseError() {
        return parseError;
    }

    public void setWeblogHandle(String weblogHandle) {
        this.weblogHandle = weblogHandle;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
