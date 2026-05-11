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

package org.apache.roller.weblogger.ui.rendering.util;

import org.apache.roller.weblogger.pojos.Weblog;

/**
 * Data Transfer Object for weblog preview operations in the rendering layer.
 * 
 * This DTO acts as a presentation model that encapsulates weblog data needed
 * for preview rendering without exposing the full POJO structure to the UI layer.
 * This refactoring addresses the layer violation where the rendering servlet
 * was directly creating and manipulating Weblog POJO objects.
 *
 * @since Refactoring Phase 1
 */
public class PreviewWeblogDTO {
    
    private Weblog originalWeblog;
    private String editorThemeId;
    private boolean isThemePreview;
    
    
    /**
     * Create a PreviewWeblogDTO from an existing Weblog POJO.
     * 
     * @param weblog The original weblog being previewed
     */
    public PreviewWeblogDTO(Weblog weblog) {
        this.originalWeblog = weblog;
        this.editorThemeId = weblog.getEditorTheme();
        this.isThemePreview = false;
    }
    
    
    /**
     * Get the original weblog POJO.
     * @return The Weblog POJO object
     */
    public Weblog getOriginalWeblog() {
        return originalWeblog;
    }
    
    
    /**
     * Set the editor theme ID for this preview.
     * @param themeId The theme ID to preview
     */
    public void setEditorThemeId(String themeId) {
        this.editorThemeId = themeId;
        this.isThemePreview = true;
    }
    
    
    /**
     * Get the editor theme ID for preview.
     * @return The editor theme ID
     */
    public String getEditorThemeId() {
        return editorThemeId;
    }
    
    
    /**
     * Check if this is a theme preview.
     * @return true if previewing a theme
     */
    public boolean isThemePreview() {
        return isThemePreview;
    }
    
    
    /**
     * Create a Weblog POJO suitable for rendering preview.
     * This method handles the POJO creation logic that was previously
     * scattered in the PreviewServlet, keeping it contained in this DTO.
     *
     * @return A Weblog POJO configured for preview rendering
     */
    public Weblog buildPreviewWeblog() {
        if (!isThemePreview) {
            // If not previewing a theme, return the original weblog
            return originalWeblog;
        }
        
        // Create temporary weblog object for theme preview
        // This encapsulates the POJO creation logic that was a layer violation
        Weblog tmpWeblog = new Weblog();
        tmpWeblog.setData(originalWeblog);
        tmpWeblog.setEditorTheme(editorThemeId);
        return tmpWeblog;
    }
    
}
