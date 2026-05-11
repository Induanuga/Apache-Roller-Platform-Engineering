package org.apache.roller.weblogger.pojos;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.roller.util.UUIDGenerator;

public class UserWeblogStar implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id = UUIDGenerator.generateUUID();
    private User user;
    private Weblog weblog;
    
    public UserWeblogStar() {}
    
    public UserWeblogStar(User user, Weblog weblog) {
        this.user = user;
        this.weblog = weblog;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Weblog getWeblog() { return weblog; }
    public void setWeblog(Weblog weblog) { this.weblog = weblog; }
    
    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof UserWeblogStar)) return false;
        UserWeblogStar o = (UserWeblogStar)other;
        return new EqualsBuilder()
            .append(getUser(), o.getUser())
            .append(getWeblog(), o.getWeblog())
            .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(getUser())
            .append(getWeblog())
            .toHashCode();
    }
}
