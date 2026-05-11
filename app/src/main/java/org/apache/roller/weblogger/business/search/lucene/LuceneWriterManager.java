package org.apache.roller.weblogger.business.search.lucene;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.roller.weblogger.config.WebloggerConfig;

/**
 * Manages lifecycle of Lucene IndexWriter.
 * Extracted from IndexOperation for encapsulation.
 */
class LuceneWriterManager {

    private static final Log logger =
            LogFactory.getFactory().getInstance(LuceneWriterManager.class);

    IndexWriter openWriter(LuceneIndexManager manager) {
        try {
            LimitTokenCountAnalyzer analyzer =
                    new LimitTokenCountAnalyzer(
                            LuceneIndexManager.getAnalyzer(),
                            WebloggerConfig.getIntProperty(
                                    "lucene.analyzer.maxTokenCount"));

            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            return new IndexWriter(manager.getIndexDirectory(), config);

        } catch (IOException e) {
            logger.error("Error creating IndexWriter", e);
            return null;
        }
    }

    void closeWriter(IndexWriter writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                logger.error("Error closing IndexWriter", e);
            }
        }
    }
}
