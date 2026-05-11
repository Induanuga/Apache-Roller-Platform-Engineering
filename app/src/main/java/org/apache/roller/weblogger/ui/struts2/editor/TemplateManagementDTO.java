package org.apache.roller.weblogger.ui.struts2.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.themes.ThemeManager;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogTemplate;
import org.apache.roller.weblogger.pojos.WeblogTheme;
import org.apache.roller.weblogger.pojos.ThemeTemplate.ComponentType;

/**
 * Data Transfer Object for Template Management operations.
 * Encapsulates template retrieval and filtering logic, abstracting
 * business manager calls from the UI action layer.
 * Removes layer violation where Struts2 action directly accessed
 * WeblogManager for template operations.
 */
public class TemplateManagementDTO {

    private Weblog weblog;
    private List<WeblogTemplate> templates;
    private Map<ComponentType, String> availableActions;

    public TemplateManagementDTO(Weblog weblog) {
        this.weblog = weblog;
        this.templates = new ArrayList<>();
        this.availableActions = Collections.emptyMap();
    }

    /**
     * Load templates for the weblog, filtering out custom stylesheet
     * if using a shared theme.
     */
    public void loadAndFilterTemplates() throws WebloggerException {
        // Get current list of templates from business manager
        List<WeblogTemplate> raw = WebloggerFactory.getWeblogger()
            .getWeblogManager().getTemplates(weblog);
        List<WeblogTemplate> pages = new ArrayList<>(raw);

        // Remove style sheet from list so not to show when theme is
        // selected in shared theme mode
        if (weblog.getTheme().getStylesheet() != null) {
            pages.remove(WebloggerFactory.getWeblogger().getWeblogManager()
                .getTemplateByLink(weblog, weblog.getTheme().getStylesheet().getLink()));
        }

        this.templates = pages;
    }

    /**
     * Build list of action types that may be added based on theme and
     * existing templates.
     */
    public void buildAvailableActions() {
        Map<ComponentType, String> actionsMap = new EnumMap<>(ComponentType.class);

        // Always allow custom component type
        ComponentType customType = ComponentType.CUSTOM;
        actionsMap.put(customType, customType.getReadableName());

        if (WeblogTheme.CUSTOM.equals(weblog.getEditorTheme())) {
            // Custom theme - determine which action templates are still available
            addAvailableComponentType(actionsMap, ComponentType.PERMALINK);
            addAvailableComponentType(actionsMap, ComponentType.SEARCH);
            addAvailableComponentType(actionsMap, ComponentType.WEBLOG);
            addAvailableComponentType(actionsMap, ComponentType.TAGSINDEX);

            // Remove component types that are already used
            for (WeblogTemplate tmpPage : templates) {
                if (!ComponentType.CUSTOM.equals(tmpPage.getAction())) {
                    actionsMap.remove(tmpPage.getAction());
                }
            }
        } else {
            // Non-custom theme - ensure we have default web page option
            addAvailableComponentType(actionsMap, ComponentType.WEBLOG);

            // Check if weblog page already exists
            boolean hasWeblogPage = false;
            for (WeblogTemplate tmpPage : templates) {
                if (ComponentType.WEBLOG.equals(tmpPage.getAction())) {
                    hasWeblogPage = true;
                    actionsMap.remove(ComponentType.WEBLOG);
                    break;
                }
            }
        }

        this.availableActions = actionsMap;
    }

    /**
     * Add a component type to the available actions map if it exists.
     */
    private void addAvailableComponentType(Map<ComponentType, String> actionsMap,
        ComponentType type) {
        actionsMap.put(type, type.getReadableName());
    }

    // Getters
    public List<WeblogTemplate> getTemplates() {
        return templates;
    }

    public Map<ComponentType, String> getAvailableActions() {
        return availableActions;
    }

    public Weblog getWeblog() {
        return weblog;
    }
}
