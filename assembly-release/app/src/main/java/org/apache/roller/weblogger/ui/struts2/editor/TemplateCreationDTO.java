package org.apache.roller.weblogger.ui.struts2.editor;

import java.util.Date;

import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.pojos.CustomTemplateRendition;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogTemplate;
import org.apache.roller.weblogger.pojos.TemplateRendition.RenditionType;
import org.apache.roller.weblogger.pojos.TemplateRendition.TemplateLanguage;
import org.apache.roller.weblogger.pojos.ThemeTemplate.ComponentType;

/**
 * Data Transfer Object for Template Creation operations.
 * Encapsulates template creation logic and persistence, abstracting
 * POJO instantiation and business manager calls from the UI action layer.
 * Removes layer violation where Struts2 action directly created and
 * persisted WeblogTemplate POJOs.
 */
public class TemplateCreationDTO {

    private Weblog weblog;
    private String templateName;
    private ComponentType templateAction;
    private String newTemplateContent;

    public TemplateCreationDTO(Weblog weblog, String templateName, ComponentType templateAction) {
        this.weblog = weblog;
        this.templateName = templateName;
        this.templateAction = templateAction;
    }

    /**
     * Create and save new template with associated rendition.
     * Encapsulates all POJO creation and business manager persistence logic.
     */
    public void createAndSaveTemplate() throws WebloggerException {
        // Create new WeblogTemplate POJO (POJO instantiation abstracted to DTO)
        WeblogTemplate newTemplate = new WeblogTemplate();
        newTemplate.setWeblog(weblog);
        newTemplate.setAction(templateAction);
        newTemplate.setName(templateName);
        newTemplate.setHidden(false);
        newTemplate.setNavbar(false);
        newTemplate.setLastModified(new Date());

        // Set link for custom templates
        if (ComponentType.CUSTOM.equals(templateAction)) {
            newTemplate.setLink(templateName);
        }

        // Ensure weblog main page has correct name
        if (ComponentType.WEBLOG.equals(templateAction)) {
            newTemplate.setName(WeblogTemplate.DEFAULT_PAGE);
        }

        // Save template via business manager (abstracted to DTO)
        WebloggerFactory.getWeblogger().getWeblogManager().saveTemplate(newTemplate);

        // Create template rendition (abstracted to DTO)
        CustomTemplateRendition standardRendition =
            new CustomTemplateRendition(newTemplate, RenditionType.STANDARD);
        standardRendition.setTemplate(newTemplateContent);
        standardRendition.setTemplateLanguage(TemplateLanguage.VELOCITY);
        WebloggerFactory.getWeblogger().getWeblogManager().saveTemplateRendition(standardRendition);

        // Update weblog if this is the default page
        if (WeblogTemplate.DEFAULT_PAGE.equals(newTemplate.getName())) {
            WebloggerFactory.getWeblogger().getWeblogManager().saveWeblog(weblog);
        }

        // Flush changes
        WebloggerFactory.getWeblogger().flush();
    }

    /**
     * Set the template content for the new rendition.
     */
    public void setTemplateContent(String content) {
        this.newTemplateContent = content;
    }

    // Getters
    public Weblog getWeblog() {
        return weblog;
    }

    public String getTemplateName() {
        return templateName;
    }

    public ComponentType getTemplateAction() {
        return templateAction;
    }
}
