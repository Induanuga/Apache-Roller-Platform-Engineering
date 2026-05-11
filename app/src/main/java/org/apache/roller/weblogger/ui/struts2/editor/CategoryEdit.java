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

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.WeblogCategory;
import org.apache.roller.weblogger.pojos.WeblogPermission;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;
import org.apache.struts2.interceptor.validation.SkipValidation;


/**
 * Edit a new or existing weblog category.
 */
// TODO: make this work @AllowedMethods({"execute","save"})
public class CategoryEdit extends UIAction {
    
    private static Log log = LogFactory.getLog(CategoryEdit.class);

    // bean for managing form data
    private CategoryBean bean = new CategoryBean();

    // the (new or already existing) category we are editing
    private WeblogCategory category = null;

    public CategoryEdit() {
        this.desiredMenu = "editor";
    }

    @Override
    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    @Override
    public List<String> requiredWeblogPermissionActions() {
        return Collections.singletonList(WeblogPermission.POST);
    }
    
    
    @Override
    public void myPrepare() {
        // Use DTO to encapsulate category loading/creation (abstracts business manager access)
        CategoryManagementDTO categoryMgmt;
        
        if (isAdd()) {
            // Create new category via DTO
            categoryMgmt = new CategoryManagementDTO(getActionWeblog());
        } else {
            // Load existing category via DTO (abstracts business manager access)
            categoryMgmt = new CategoryManagementDTO(getActionWeblog(), getBean().getId());
            if (!categoryMgmt.loadCategory()) {
                log.error("Error looking up category with ID: " + getBean().getId());
            }
        }
        
        // Get category POJO from DTO
        category = categoryMgmt.getCategory();
    }
    
    
    /**
     * Show category form.
     */
    @SkipValidation
    @Override
    public String execute() {
        if (!isAdd()) {
            // make sure bean is properly loaded from pojo data
            getBean().copyFrom(category);
        }
        return INPUT;
    }

    private boolean isAdd() {
        return StringUtils.isEmpty( bean.getId() );
    }

    /**
     * Save new category.
     */
    public String save() {
        myValidate();
        
        if(!hasActionErrors()) {
            try {
                // copy updated attributes
                getBean().copyTo(category);

                // Use DTO to save category (abstracts all business manager access and cache invalidation)
                CategoryManagementDTO categoryMgmt = new CategoryManagementDTO(getActionWeblog());
                categoryMgmt.setCategory(category);
                
                if (categoryMgmt.saveCategory()) {
                    addMessage(isAdd()? "categoryForm.created"
                            : "categoryForm.changesSaved",
                            category.getName());
                    return SUCCESS;
                } else {
                    addError("generic.error.check.logs");
                    return INPUT;
                }
            } catch(Exception ex) {
                log.error("Error saving category", ex);
                addError("generic.error.check.logs");
            }
        }
        
        return INPUT;
    }

    public void myValidate() {
        if (bean.getName() == null || !bean.getName().equals(StringEscapeUtils.escapeHtml4(bean.getName()))) {
            addError("categoryForm.error.invalidName");
        } else {
            // Use DTO to check for duplicate category names (abstracts POJO and weblog access)
            CategoryManagementDTO categoryMgmt = new CategoryManagementDTO(getActionWeblog());
            
            // For new categories or when name changed on existing category
            String categoryIdToExclude = isAdd() ? null : getBean().getId();
            if (categoryMgmt.categoryNameExists(bean.getName(), categoryIdToExclude)) {
                addError("categoryForm.error.duplicateName", bean.getName());
            }
        }
    }

    public CategoryBean getBean() {
        return bean;
    }

    public void setBean(CategoryBean bean) {
        this.bean = bean;
    }
    
}
