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

import java.io.Serializable;
import java.security.Permission;
import java.util.List;
import java.util.Objects;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;


/**
 * Permission for one specific weblog.
 * 
 * FIXED: Improved cohesion, fixed implies() complexity, fixed equals() contract,
 * and ensured consistent initialization.
 * 
 * @author Dave Johnson
 */
public class WeblogPermission extends ObjectPermission implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Permission action constants
    public static final String EDIT_DRAFT = "edit_draft";
    public static final String POST = "post";
    public static final String ADMIN = "admin";
    public static final List<String> ALL_ACTIONS = List.of(EDIT_DRAFT, POST, ADMIN);
    
    // Action hierarchy for permission implication (higher index = more powerful)
    private static final List<String> ACTION_HIERARCHY = List.of(EDIT_DRAFT, POST, ADMIN);

    public WeblogPermission() {
        // Required by JPA
        super();
    }

    /**
     * Full constructor with all fields initialized.
     */
    public WeblogPermission(Weblog weblog, User user, String actions) {
        super("WeblogPermission user: " + (user != null ? user.getUserName() : "N/A"), actions);
        initializeFields(weblog, user);
    }
    
    /**
     * Constructor with list of actions.
     */
    public WeblogPermission(Weblog weblog, User user, List<String> actions) {
        super("WeblogPermission user: " + (user != null ? user.getUserName() : "N/A"));
        setActionsAsList(actions); 
        initializeFields(weblog, user);
    }
    
    /**
     * Constructor without user - for cases where user is determined later.
     * FIXED: Now properly initializes userName to avoid null invariant violations.
     */
    public WeblogPermission(Weblog weblog, List<String> actions) {
        super("WeblogPermission user: N/A");
        setActionsAsList(actions); 
        initializeFields(weblog, null);
    }
    
    /**
     * FIXED: Extracted common initialization logic to improve cohesion.
     * Centralizes field initialization for all constructors.
     */
    private void initializeFields(Weblog weblog, User user) {
        this.objectType = "Weblog";
        this.objectId = weblog != null ? weblog.getHandle() : null;
        this.userName = user != null ? user.getUserName() : "N/A";
    }
    
    /**
     * Lookup weblog from objectId.
     * NOTE: This method has external dependency on WebloggerFactory.
     */
    public Weblog getWeblog() throws WebloggerException {
        if (objectId != null) {
            return WebloggerFactory.getWeblogger().getWeblogManager().getWeblogByHandle(objectId, null);
        }
        return null;
    }

    /**
     * Lookup user from userName.
     * NOTE: This method has external dependency on WebloggerFactory.
     */
    public User getUser() throws WebloggerException {
        if (userName != null && !"N/A".equals(userName)) {
            return WebloggerFactory.getWeblogger().getUserManager().getUserByUserName(userName);
        }
        return null;
    }

    /**
     * FIXED: Simplified implies() method to reduce complexity and improve readability.
     * Uses action hierarchy instead of nested conditionals.
     */
    @Override
    public boolean implies(Permission perm) {
        if (!(perm instanceof WeblogPermission)) {
            return false;
        }
        
        WeblogPermission requested = (WeblogPermission) perm;
        
        // Check if this permission's actions imply the requested permission's actions
        for (String requestedAction : requested.getActionsAsList()) {
            if (!impliesAction(requestedAction)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * FIXED: Extracted method to check if a single action is implied.
     * Uses action hierarchy: ADMIN implies POST implies EDIT_DRAFT.
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
     * FIXED: Corrected toString() - was incorrectly showing "GlobalPermission".
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WeblogPermission: ");
        sb.append("user=").append(getUserName()).append(", ");
        sb.append("weblog=").append(getObjectId()).append(", ");
        sb.append("actions=[");
        List<String> actions = getActionsAsList();
        for (int i = 0; i < actions.size(); i++) {
            sb.append(actions.get(i));
            if (i < actions.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * FIXED: Simplified equals() to use standard Java Objects.equals().
     * Reduces complexity and removes dependency on EqualsBuilder.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WeblogPermission)) {
            return false;
        }
        WeblogPermission that = (WeblogPermission) other;
        return Objects.equals(getUserName(), that.getUserName()) &&
               Objects.equals(getObjectId(), that.getObjectId()) &&
               Objects.equals(getActions(), that.getActions());
    }

    /**
     * FIXED: Simplified hashCode() to use standard Java Objects.hash().
     * Reduces complexity and removes dependency on HashCodeBuilder.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getUserName(), getObjectId(), getActions());
    }
}