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

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.util.Utilities;


/**
 * Represents a permission that applies globally to the entire web application.
 * 
 * FIXED: Removed shadowed 'actions' field and methods - now inherited from RollerPermission.
 * Simplified implies() method. Removed unused actionImplies() method.
 */
public class GlobalPermission extends RollerPermission {
    private static final long serialVersionUID = 1L;
    
    // REMOVED: protected String actions; - This was shadowing RollerPermission.actions!
    
    /** Allowed to login and edit profile */
    public static final String LOGIN  = "login";
    
    /** Allowed to login and do weblogging */
    public static final String WEBLOG = "weblog";

    /** Allowed to login and do everything, including site-wide admin */
    public static final String ADMIN  = "admin";
    
    /** Action hierarchy for permission implication (higher index = more powerful) */
    private static final List<String> ACTION_HIERARCHY = List.of(LOGIN, WEBLOG, ADMIN);

    /**
     * Create global permission for one specific user initialized with the 
     * actions that are implied by the user's roles.
     * @param user User of permission.
     * @throws org.apache.roller.weblogger.WebloggerException
     */
    public GlobalPermission(User user) throws WebloggerException {
        super("GlobalPermission user: " + user.getUserName());
        
        // Loop through user's roles, adding actions implied by each
        List<String> roles = WebloggerFactory.getWeblogger().getUserManager().getRoles(user);
        List<String> actionsList = new ArrayList<>();
        for (String role : roles) {
            String impliedActions = WebloggerConfig.getProperty("role.action." + role);
            if (impliedActions != null) {
                List<String> toAdds = Utilities.stringToStringList(impliedActions, ",");
                for (String toAdd : toAdds) {
                    if (!actionsList.contains(toAdd)) {
                        actionsList.add(toAdd);
                    }
                }
            }
        }
        setActionsAsList(actionsList);
    }
        
    /** 
     * Create global permission with the actions specified by array.
     * @param actions actions to add to permission
     * @throws org.apache.roller.weblogger.WebloggerException
     */
    public GlobalPermission(List<String> actions) throws WebloggerException {
        super("GlobalPermission user: N/A");
        setActionsAsList(actions);
    }
        
    /** 
     * Create global permission for one specific user initialized with the 
     * actions specified by array.
     * @param user User of permission.
     * @throws org.apache.roller.weblogger.WebloggerException
     */
    public GlobalPermission(User user, List<String> actions) throws WebloggerException {
        super("GlobalPermission user: " + user.getUserName());
        setActionsAsList(actions);
    }
        
    /**
     * FIXED: Simplified implies() method to reduce complexity.
     * Uses action hierarchy instead of nested conditionals.
     */
    @Override
    public boolean implies(Permission perm) {
        if (getActionsAsList().isEmpty()) {
            // New, unsaved user - no permissions implied
            return false;
        }
        
        // GlobalPermission with ADMIN implies everything
        if (hasAction(ADMIN)) {
            return true;
        }
        
        if (perm instanceof WeblogPermission) {
            // GlobalPermission with WEBLOG or LOGIN does NOT imply WeblogPermission
            // Only ADMIN implies WeblogPermission
            return false;
        } else if (perm instanceof RollerPermission) {
            // Check if our actions imply the requested permission's actions
            RollerPermission rperm = (RollerPermission) perm;
            for (String requestedAction : rperm.getActionsAsList()) {
                if (!impliesAction(requestedAction)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * FIXED: Extracted method to check if a single action is implied.
     * Uses action hierarchy: ADMIN implies WEBLOG implies LOGIN.
     */
    private boolean impliesAction(String requestedAction) {
        int requestedLevel = ACTION_HIERARCHY.indexOf(requestedAction);
        if (requestedLevel == -1) {
            return false; // Unknown action
        }
        
        // Check if we have any action at or above the requested level
        for (String ownedAction : getActionsAsList()) {
            int ownedLevel = ACTION_HIERARCHY.indexOf(ownedAction);
            if (ownedLevel >= requestedLevel) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * REMOVED: actionImplies() method - was unused dead code.
     */
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GlobalPermission: ");
        List<String> actions = getActionsAsList();
        for (int i = 0; i < actions.size(); i++) {
            sb.append(actions.get(i));
            if (i < actions.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    // REMOVED: setActions() override - now properly inherited from RollerPermission
    // REMOVED: getActions() override - now properly inherited from RollerPermission

    /**
     * FIXED: Simplified equals() to use standard Java Objects.equals().
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GlobalPermission)) {
            return false;
        }
        GlobalPermission that = (GlobalPermission) other;
        // GlobalPermission is identified by its actions only (no user-specific state)
        return Objects.equals(getActions(), that.getActions());
    }

    /**
     * FIXED: Simplified hashCode() to use standard Java Objects.hash().
     */
    @Override
    public int hashCode() {
        return Objects.hash(getActions());
    }
}