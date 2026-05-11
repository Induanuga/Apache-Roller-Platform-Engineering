package org.apache.roller.weblogger.ui.struts2.editor;

import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogTemplate;
import org.apache.roller.weblogger.pojos.WeblogTheme;
import org.apache.roller.weblogger.pojos.ThemeTemplate;
import org.apache.roller.weblogger.util.cache.CacheManager;

/**
 * Data Transfer Object for Template Deletion operations.
 * Encapsulates template retrieval and deletion logic, abstracting
 * business manager calls and POJO checks from the UI action layer.
 * Removes layer violation where Struts2 action directly accessed
 * business manager for template deletion and manipulation.
 */
public class TemplateDeletionDTO {

    private Weblog weblog;
    private String templateId;
    private WeblogTemplate template;

    public TemplateDeletionDTO(Weblog weblog, String templateId) {
        this.weblog = weblog;
        this.templateId = templateId;
    }

    /**
     * Retrieve template by ID from business manager.
     * Abstracts business manager access from action layer.
     */
    public boolean loadTemplate() throws WebloggerException {
        template = WebloggerFactory.getWeblogger().getWeblogManager().getTemplate(templateId);
        return template != null;
    }

    /**
     * Check if template can be deleted.
     * Encapsulates business logic for deletion eligibility.
     */
    public boolean canDelete() {
        if (template == null) {
            return false;
        }
        // Template can be deleted if it's not required OR weblog is using custom theme
        return !template.isRequired() || !WeblogTheme.CUSTOM.equals(weblog.getEditorTheme());
    }

    /**
     * Delete template and associated resources.
     * Encapsulates all deletion logic including stylesheet cleanup.
     */
    public void deleteTemplate() throws WebloggerException {
        if (template == null) {
            return;
        }

        // Handle stylesheet removal if this is the default page
        if (template.getName().equals(WeblogTemplate.DEFAULT_PAGE)) {
            deleteAssociatedStylesheet();
        }

        // Remove the template
        WebloggerFactory.getWeblogger().getWeblogManager().removeTemplate(template);

        // Invalidate cache
        CacheManager.invalidate(template);

        // Flush changes
        WebloggerFactory.getWeblogger().flush();
    }

    /**
     * Delete the stylesheet associated with default page template.
     * Encapsulates theme stylesheet cleanup logic.
     */
    private void deleteAssociatedStylesheet() throws WebloggerException {
        ThemeTemplate stylesheet = weblog.getTheme().getStylesheet();

        // Check if stylesheet exists and matches expected link
        if (stylesheet != null && weblog.getTheme().getStylesheet() != null
            && stylesheet.getLink().equals(weblog.getTheme().getStylesheet().getLink())) {

            // Retrieve and delete the stylesheet template
            WeblogTemplate css = WebloggerFactory.getWeblogger().getWeblogManager()
                .getTemplateByLink(weblog, stylesheet.getLink());

            if (css != null) {
                WebloggerFactory.getWeblogger().getWeblogManager().removeTemplate(css);
            }
        }
    }

    /**
     * Get the loaded template.
     */
    public WeblogTemplate getTemplate() {
        return template;
    }

    /**
     * Get template ID.
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * Get weblog.
     */
    public Weblog getWeblog() {
        return weblog;
    }
}
