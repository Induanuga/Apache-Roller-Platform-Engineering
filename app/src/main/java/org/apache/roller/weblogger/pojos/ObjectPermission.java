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

import java.util.Date;
import java.util.Objects;
import org.apache.roller.util.UUIDGenerator;


/**
 * Base permission class for object-level permissions in Roller.
 * 
 * FIXED: Removed shadowed 'actions' field and methods - now inherited from RollerPermission.
 * This fixes the Broken Hierarchy smell by ensuring proper inheritance contract.
 */
public abstract class ObjectPermission extends RollerPermission {
    private static final long serialVersionUID = 1L;
    
    protected String  id = UUIDGenerator.generateUUID();
    protected String  userName;
    protected String  objectType;
    protected String  objectId;
    protected boolean pending = false;
    protected Date    dateCreated = new Date();
    
    // REMOVED: protected String actions; - This was shadowing RollerPermission.actions!
    
    
    public ObjectPermission() {
        super("");
    }
    
    public ObjectPermission(String name) {
        super(name);
    }
    
    public ObjectPermission(String name, String actions) {
        super(name, actions);
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // REMOVED: setActions() override - now properly inherited from RollerPermission
    // REMOVED: getActions() override - now properly inherited from RollerPermission

    public String getUserName() {
        return userName;
    }

    public void setUserName(String username) {
        this.userName = username;
    }

    /*public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }*/

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }
    
    // FIXED: Added proper equals() that includes ObjectPermission fields
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        // super.equals() already checked class equality, so we can cast safely
        ObjectPermission that = (ObjectPermission) obj;
        return pending == that.pending &&
               Objects.equals(id, that.id) &&
               Objects.equals(userName, that.userName) &&
               Objects.equals(objectType, that.objectType) &&
               Objects.equals(objectId, that.objectId);
    }

    // FIXED: Added proper hashCode() that includes ObjectPermission fields
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, userName, objectType, objectId, pending);
    }
}