package org.apache.roller.weblogger.business.search.lucene;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;

class LuceneQueryBuilder {

    private static final String[] SEARCH_FIELDS = new String[] {
        FieldConstants.CONTENT,
        FieldConstants.TITLE,
        FieldConstants.C_CONTENT
    };

    Query buildQuery(
            String term,
            String weblogHandle,
            String category,
            String locale) throws ParseException {

        MultiFieldQueryParser parser =
                new MultiFieldQueryParser(
                        SEARCH_FIELDS,
                        LuceneIndexManager.getAnalyzer());

        parser.setDefaultOperator(MultiFieldQueryParser.Operator.AND);

        Query query = parser.parse(term);

        Term handleTerm =
                IndexUtil.getTerm(FieldConstants.WEBSITE_HANDLE, weblogHandle);
        if (handleTerm != null) {
            query = new BooleanQuery.Builder()
                    .add(query, BooleanClause.Occur.MUST)
                    .add(new TermQuery(handleTerm), BooleanClause.Occur.MUST)
                    .build();
        }

        if (category != null) {
            Term catTerm =
                    new Term(FieldConstants.CATEGORY, category.toLowerCase());
            query = new BooleanQuery.Builder()
                    .add(query, BooleanClause.Occur.MUST)
                    .add(new TermQuery(catTerm), BooleanClause.Occur.MUST)
                    .build();
        }

        Term localeTerm =
                IndexUtil.getTerm(FieldConstants.LOCALE, locale);
        if (localeTerm != null) {
            query = new BooleanQuery.Builder()
                    .add(query, BooleanClause.Occur.MUST)
                    .add(new TermQuery(localeTerm), BooleanClause.Occur.MUST)
                    .build();
        }

        return query;
    }
}
