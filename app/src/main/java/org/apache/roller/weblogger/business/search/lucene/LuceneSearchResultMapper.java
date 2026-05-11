package org.apache.roller.weblogger.business.search.lucene;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.URLStrategy;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.search.SearchResultList;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.wrapper.WeblogEntryWrapper;

class LuceneSearchResultMapper {

    SearchResultList map(
            ScoreDoc[] hits,
            SearchOperation search,
            int pageNum,
            int entryCount,
            String weblogHandle,
            boolean weblogSpecific,
            URLStrategy urlStrategy) throws WebloggerException {

        List<WeblogEntryWrapper> results = new ArrayList<>();
        Set<String> categories = new TreeSet<>();

        int offset = Math.max(0, pageNum * entryCount);
        int limit = Math.min(entryCount, hits.length - offset);

        WeblogEntryManager mgr =
                WebloggerFactory.getWeblogger().getWeblogEntryManager();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        try {
    for (int i = offset; i < offset + limit; i++) {

        Document doc = search.getSearcher()
                .storedFields()
                .document(hits[i].doc);

        WeblogEntry entry =
                mgr.getWeblogEntry(doc.get(FieldConstants.ID));

        if (entry != null && entry.getPubTime().before(now)) {
            results.add(WeblogEntryWrapper.wrap(entry, urlStrategy));
        }
        }
    } catch (IOException e) {
        throw new WebloggerException("Error reading Lucene search results", e);
    }

        return new SearchResultList(results, categories, limit, offset);
    }
}
