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

package org.apache.roller.weblogger.pojos;

import java.util.List;
import java.util.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.util.Utilities;


/**
 * Base permission class for Roller.
 * 
 * FIXED: Removed dependency on ObjectPermission to fix Broken Hierarchy.
 * Now uses RollerPermission abstraction for all permission operations.
 */
public abstract class RollerPermission extends java.security.Permission {
    private static final long serialVersionUID = 1L;
    private static Log log = LogFactory.getLog(RollerPermission.class);
    
    // FIXED: Added actions field here to avoid shadowing in subclasses
    protected String actions;

    public RollerPermission(String name) {
        super(name);
    }
    
    public RollerPermission(String name, String actions) {
        super(name);
        this.actions = actions;
    }
            
    /**
     * Set actions string (comma-separated).
     */
    public void setActions(String actions) {
        this.actions = actions;
    }

    @Override
    public String getActions() {
        return this.actions != null ? this.actions : "";
    }

    public List<String> getActionsAsList() {
        return Utilities.stringToStringList(getActions(), ",");
    }
    
    public void setActionsAsList(List<String> actionsList) {
        setActions(Utilities.stringListToString(actionsList, ","));
    }

    public boolean hasAction(String action) {
        List<String> actionList = getActionsAsList();
        return actionList.contains(action);
    }
    
    public boolean hasActions(List<String> actionsToCheck) {
        List<String> actionList = getActionsAsList();
        for (String actionToCheck : actionsToCheck) {
            if (!actionList.contains(actionToCheck)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Merge actions from another permission into this permission.
     * FIXED: Changed parameter from ObjectPermission to RollerPermission to remove
     * cyclic dependency and fix Broken Hierarchy.
     */
    public void addActions(RollerPermission perm) {
        if (perm == null || perm.isEmpty()) {
            return;
        }
        List<String> newActions = perm.getActionsAsList();
        addActions(newActions);
    }
    
    /**
     * Merge actions into this permission.
     */
    public void addActions(List<String> newActions) {
        if (newActions == null || newActions.isEmpty()) {
            return;
        }
        List<String> updatedActions = getActionsAsList();
        for (String newAction : newActions) {
            if (!updatedActions.contains(newAction)) {
                updatedActions.add(newAction);
            }
        }
        setActionsAsList(updatedActions);
    }
    
    /**
     * Merge actions into this permission.
     */
    public void removeActions(List<String> actionsToRemove) {
        if (actionsToRemove == null || actionsToRemove.isEmpty()) {
            return;
        }
        List<String> updatedActions = getActionsAsList();
        for (String actionToRemove : actionsToRemove) {
            updatedActions.remove(actionToRemove);
        }
        log.debug("updatedActions: " + updatedActions);
        setActionsAsList(updatedActions);
    }
    
    /**
     * True if permission specifies no actions
     */
    public boolean isEmpty() {
        return getActions() == null || getActions().isBlank();
    }
    
    // FIXED: Proper equals() implementation without calling super.equals()
    // java.security.Permission has abstract equals(), so we implement fully here
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RollerPermission that = (RollerPermission) obj;
        // Compare permission name (from superclass) and actions
        return Objects.equals(getName(), that.getName()) 
            && Objects.equals(actions, that.actions);
    }

    // FIXED: Proper hashCode() implementation without calling super.hashCode()
    // java.security.Permission has abstract hashCode(), so we implement fully here
    @Override
    public int hashCode() {
        return Objects.hash(getName(), actions);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getName() + ", " + getActions() + "]";
    }
}