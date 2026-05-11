/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.roller.weblogger.ui.struts2.editor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.util.RollerConstants;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WeblogManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.pojos.*;
import org.apache.roller.weblogger.pojos.TemplateRendition.RenditionType;
import org.apache.roller.weblogger.pojos.TemplateRendition.TemplateLanguage;
import org.apache.roller.weblogger.pojos.ThemeTemplate.ComponentType;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;
import org.apache.roller.weblogger.util.cache.CacheManager;
import org.apache.struts2.convention.annotation.AllowedMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Templates listing page.
 */
// TODO: make this work @AllowedMethods({"execute","add"})
public class Templates extends UIAction {

    private static final Log log = LogFactory.getLog(Templates.class);

    // list of templates to display
    private List<WeblogTemplate> templates = Collections.emptyList();

    // list of template action types user is allowed to create
    private Map<ComponentType, String> availableActions = Collections.emptyMap();

    // name and action of new template if we are adding a template
    private String newTmplName = null;
    private ComponentType newTmplAction = null;

    // id of template to remove
    private String removeId = null;

    public Templates() {
        this.actionName = "templates";
        this.desiredMenu = "editor";
        this.pageTitle = "pagesForm.title";
    }

    @Override
    public String execute() {

        try {
            // Use DTO to encapsulate template retrieval and filtering logic
            TemplateManagementDTO templateMgmt = new TemplateManagementDTO(getActionWeblog());
            templateMgmt.loadAndFilterTemplates();
            templateMgmt.buildAvailableActions();

            // Set templates and available actions from DTO
            setTemplates(templateMgmt.getTemplates());
            setAvailableActions(templateMgmt.getAvailableActions());

            // Determine default action for non-custom themes
            if (!WeblogTheme.CUSTOM.equals(getActionWeblog().getEditorTheme())) {
                if (getNewTmplAction() == null) {
                    setNewTmplAction(ComponentType.WEBLOG);
                }
                // Check if weblog page already exists
                for (WeblogTemplate tmpPage : getTemplates()) {
                    if (ComponentType.WEBLOG.equals(tmpPage.getAction())) {
                        setNewTmplAction(null);
                        break;
                    }
                }
            }

        } catch (WebloggerException ex) {
            log.error("Error getting templates for weblog - "
                + getActionWeblog().getHandle(), ex);
            addError("Error getting template list - check Roller logs");
        }

        return LIST;
    }

    private void addComponentTypeToMap(Map<ComponentType, String> map, ComponentType component) {
        map.put(component, component.getReadableName());
    }

    /**
     * Save a new template.
     */
    public String add() {

        // validation
        myValidate();

        if (!hasActionErrors()) {
            try {
                // Use DTO to encapsulate template creation and persistence logic
                TemplateCreationDTO templateCreator = new TemplateCreationDTO(
                    getActionWeblog(), getNewTmplName(), getNewTmplAction());
                templateCreator.setTemplateContent(getText("pageForm.newTemplateContent"));
                templateCreator.createAndSaveTemplate();

                // reset form fields
                setNewTmplName(null);
                setNewTmplAction(null);

            } catch (WebloggerException ex) {
                log.error("Error adding new template for weblog - " + getActionWeblog().getHandle(), ex);
                addError("Error adding new template - check Roller logs");
            }
        }

        return execute();
    }

    /**
     * Remove a template.
     */
    public String remove() {

        try {
            // Use DTO to encapsulate template deletion logic
            TemplateDeletionDTO templateDeletor = new TemplateDeletionDTO(getActionWeblog(), getRemoveId());

            // Load template via business manager (abstracted to DTO)
            if (!templateDeletor.loadTemplate()) {
                addError("editPages.remove.error");
                return execute();
            }

            // Check if template can be deleted (business logic abstracted to DTO)
            if (!templateDeletor.canDelete()) {
                addError("editPages.remove.requiredTemplate");
                return execute();
            }

            // Delete template and associated resources (all business logic abstracted to DTO)
            templateDeletor.deleteTemplate();

        } catch (Exception ex) {
            log.error("Error removing page - " + getRemoveId(), ex);
            addError("editPages.remove.error");
        }

        return execute();
    }

    // validation when adding a new template
    private void myValidate() {

        // make sure name is non-null and within proper size
        if (StringUtils.isEmpty(getNewTmplName())) {
            addError("Template.error.nameNull");
        } else if (getNewTmplName().length() > RollerConstants.TEXTWIDTH_255) {
            addError("Template.error.nameSize");
        }

        // make sure action is a valid
        if (getNewTmplAction() == null) {
            addError("Template.error.actionNull");
        }

        // check if template by that name already exists
        try {
            WeblogTemplate existingPage = WebloggerFactory.getWeblogger().getWeblogManager()
                .getTemplateByName(getActionWeblog(), getNewTmplName());
            if (existingPage != null) {
                addError("pagesForm.error.alreadyExists", getNewTmplName());
            }
        } catch (WebloggerException ex) {
            log.error("Error checking for existing template", ex);
        }

    }

    /**
     * Checks if is custom theme.
     *
     * @return true, if is custom theme
     */
    public boolean isCustomTheme() {
        return (WeblogTheme.CUSTOM.equals(getActionWeblog().getEditorTheme()));
    }

    public List<WeblogTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(List<WeblogTemplate> templates) {
        this.templates = templates;
    }

    public Map<ComponentType, String> getAvailableActions() {
        return availableActions;
    }

    public void setAvailableActions(Map<ComponentType, String> availableActions) {
        this.availableActions = availableActions;
    }

    public String getNewTmplName() {
        return newTmplName;
    }

    public void setNewTmplName(String newTmplName) {
        this.newTmplName = newTmplName;
    }

    public ComponentType getNewTmplAction() {
        return newTmplAction;
    }

    public void setNewTmplAction(ComponentType newTmplAction) {
        this.newTmplAction = newTmplAction;
    }

    public String getRemoveId() {
        return removeId;
    }

    public void setRemoveId(String removeId) {
        this.removeId = removeId;
    }
}
