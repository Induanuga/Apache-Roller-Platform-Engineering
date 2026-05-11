package org.apache.roller.weblogger.business.search.lucene;

import java.util.List;

import org.apache.lucene.document.*;
import org.apache.lucene.util.BytesRef;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.pojos.WeblogCategory;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntryComment;

/**
 * Builds Lucene Documents from WeblogEntry domain objects.
 * Extracted from IndexOperation to reduce responsibility overload.
 */
class WeblogEntryDocumentBuilder {

    Document build(WeblogEntry data) {

        boolean indexComments =
                WebloggerConfig.getBooleanProperty(
                        "search.index.comments", true);

        String commentContent = "";
        String commentEmail = "";
        String commentName = "";

        if (indexComments && data.getComments() != null) {
            StringBuilder email = new StringBuilder();
            StringBuilder content = new StringBuilder();
            StringBuilder name = new StringBuilder();

            for (WeblogEntryComment c : data.getComments()) {
                if (c.getContent() != null) content.append(c.getContent()).append(",");
                if (c.getEmail() != null) email.append(c.getEmail()).append(",");
                if (c.getName() != null) name.append(c.getName()).append(",");
            }

            commentEmail = email.toString();
            commentContent = content.toString();
            commentName = name.toString();
        }

        Document doc = new Document();

        doc.add(new StringField(FieldConstants.ID, data.getId(), Field.Store.YES));
        doc.add(new StringField(
                FieldConstants.WEBSITE_HANDLE,
                data.getWebsite().getHandle(),
                Field.Store.YES));

        if (data.getCreator() != null) {
            doc.add(new TextField(
                    FieldConstants.USERNAME,
                    data.getCreator().getUserName().toLowerCase(),
                    Field.Store.YES));
        }

        doc.add(new TextField(FieldConstants.TITLE, data.getTitle(), Field.Store.YES));
        doc.add(new StringField(
                FieldConstants.LOCALE,
                data.getLocale().toLowerCase(),
                Field.Store.YES));

        doc.add(new TextField(FieldConstants.CONTENT, data.getText(), Field.Store.NO));
        doc.add(new StringField(
                FieldConstants.UPDATED,
                data.getUpdateTime().toString(),
                Field.Store.YES));

        if (data.getPubTime() != null) {
            doc.add(new SortedDocValuesField(
                    FieldConstants.PUBLISHED,
                    new BytesRef(data.getPubTime().toString())));
        }

        WeblogCategory cat = data.getCategory();
        if (cat != null) {
            doc.add(new StringField(
                    FieldConstants.CATEGORY,
                    cat.getName().toLowerCase(),
                    Field.Store.YES));
        }

        doc.add(new TextField(FieldConstants.C_CONTENT, commentContent, Field.Store.NO));
        doc.add(new StringField(FieldConstants.C_EMAIL, commentEmail, Field.Store.YES));
        doc.add(new StringField(FieldConstants.C_NAME, commentName, Field.Store.YES));

        return doc;
    }
}
