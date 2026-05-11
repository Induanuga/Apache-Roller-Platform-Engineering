package org.apache.roller.weblogger.pojos;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.roller.util.UUIDGenerator;

public class UserWeblogEntryStar implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id = UUIDGenerator.generateUUID();
    private User user;
    private WeblogEntry weblogEntry;
    
    public UserWeblogEntryStar() {}
    
    public UserWeblogEntryStar(User user, WeblogEntry weblogEntry) {
        this.user = user;
        this.weblogEntry = weblogEntry;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public WeblogEntry getWeblogEntry() { return weblogEntry; }
    public void setWeblogEntry(WeblogEntry weblogEntry) { this.weblogEntry = weblogEntry; }
    
    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof UserWeblogEntryStar)) return false;
        UserWeblogEntryStar o = (UserWeblogEntryStar)other;
        return new EqualsBuilder()
            .append(getUser(), o.getUser())
            .append(getWeblogEntry(), o.getWeblogEntry())
            .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(getUser())
            .append(getWeblogEntry())
            .toHashCode();
    }
}
