
package org.apache.roller.weblogger.ui.struts2.admin;

import java.util.List;

/**
 * STRATEGY PATTERN — DashboardViewStrategy interface
 *
 * Defines the contract for what metric keys a given view mode displays.
 * The JSP only renders metrics whose keys are returned here — it has
 * no knowledge of why a metric is or isn't shown.
 *
 * Adding a new view mode = implement this interface. Zero changes to
 * existing code (Open/Closed Principle).
 */
public interface DashboardViewStrategy {

    /**
     * Returns the list of metric keys this view mode should display.
     * The JSP uses these keys to decide which sections to render.
     */
    List<String> getMetricsToDisplay();

    /**
     * Returns a human-readable label for this view mode.
     */
    String getViewLabel();
}
