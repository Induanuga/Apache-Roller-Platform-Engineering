package org.apache.roller.weblogger.business.search.lucene;

import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.roller.weblogger.config.WebloggerConfig;

public final class LuceneAnalyzerFactory {

    private static final Log logger =
            LogFactory.getFactory().getInstance(LuceneAnalyzerFactory.class);

    private LuceneAnalyzerFactory() {
    }

    public static Analyzer createAnalyzer() {
        final String className =
                WebloggerConfig.getProperty("lucene.analyzer.class");
        try {
            Class<?> clazz = Class.forName(className);
            return (Analyzer) ConstructorUtils.invokeConstructor(clazz, null);
        } catch (Exception ex) {
            logger.warn("Falling back to default analyzer", ex);
            return new StandardAnalyzer();
        }
    }
}
