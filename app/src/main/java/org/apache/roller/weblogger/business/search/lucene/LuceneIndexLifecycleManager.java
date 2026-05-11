package org.apache.roller.weblogger.business.search.lucene;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountAnalyzer;
import org.apache.roller.weblogger.business.InitializationException;


class LuceneIndexLifecycleManager {

    private final String indexDir;
    private final File marker;
    private boolean inconsistentAtStartup = false;

    private static final Log logger =
            LogFactory.getFactory().getInstance(LuceneIndexLifecycleManager.class);

    LuceneIndexLifecycleManager(String indexDir) {
        this.indexDir = indexDir;
        this.marker = new File(indexDir + File.separator + ".index-inconsistent");
    }

    void initialize() throws InitializationException {

        if (marker.exists()) {
            inconsistentAtStartup = true;
            deleteIndex();
        } else {
            try {
                new File(indexDir).mkdirs();
                marker.createNewFile();
            } catch (IOException e) {
                throw new InitializationException("Failed initializing Lucene index", e);
            }
        }

        if (!indexExists()) {
            inconsistentAtStartup = true;
            deleteIndex();
            createIndex(getDirectory());
        }
    }

    boolean isInconsistentAtStartup() {
        return inconsistentAtStartup;
    }

    Directory getDirectory() {
        try {
            return FSDirectory.open(Path.of(indexDir));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean indexExists() {
        try {
            return DirectoryReader.indexExists(getDirectory());
        } catch (IOException e) {
            return false;
        }
    }

    private void deleteIndex() {
        try (FSDirectory dir = FSDirectory.open(Path.of(indexDir))) {
            for (String f : dir.listAll()) {
                Files.delete(Path.of(indexDir, f));
            }
        } catch (IOException ex) {
            logger.error("Failed deleting index", ex);
        }
    }

    private void createIndex(Directory dir) {
        try (IndexWriter writer =
                new IndexWriter(dir,
                        new IndexWriterConfig(
                                new LimitTokenCountAnalyzer(
                                        LuceneAnalyzerFactory.createAnalyzer(), 128)))) {
        } catch (IOException e) {
            logger.error("Failed creating index", e);
        }
    }

    void clearConsistencyMarker() {
        marker.delete();
    }
    
}
