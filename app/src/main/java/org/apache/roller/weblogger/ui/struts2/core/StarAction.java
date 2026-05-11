package org.apache.roller.weblogger.ui.struts2.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.business.UserManager;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.business.WeblogManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;
import org.apache.roller.weblogger.util.cache.CacheManager;
import org.apache.struts2.ServletActionContext;

public class StarAction extends UIAction {
    private static Log log = LogFactory.getLog(StarAction.class);
    
    private String id; // weblog id or weblog entry id
    
    public StarAction() {
        this.pageTitle = "Star Action";
    }
    
    @Override
    public boolean isUserRequired() {
        return false;
    }

    public String weblog() {
        try {
            if (getAuthenticatedUser() == null) {
                if ("XMLHttpRequest".equals(ServletActionContext.getRequest().getHeader("X-Requested-With"))) {
                    ServletActionContext.getResponse().setContentType("text/plain");
                    ServletActionContext.getResponse().getWriter().write("login_required");
                    return null;
                }
                return LOGIN;
            }
            UserManager umgr = WebloggerFactory.getWeblogger().getUserManager();
            WeblogManager wmgr = WebloggerFactory.getWeblogger().getWeblogManager();
            Weblog weblog = wmgr.getWeblog(getId());
            if (weblog != null) {
                if (umgr.isWeblogStarred(getAuthenticatedUser(), weblog)) {
                    umgr.unstarWeblog(getAuthenticatedUser(), weblog);
                } else {
                    umgr.starWeblog(getAuthenticatedUser(), weblog);
                }
                WebloggerFactory.getWeblogger().flush();
                CacheManager.invalidate(weblog);
                
                if ("XMLHttpRequest".equals(ServletActionContext.getRequest().getHeader("X-Requested-With"))) {
                    long count = umgr.getWeblogStarCountFresh(weblog);
                    ServletActionContext.getResponse().setContentType("text/plain");
                    ServletActionContext.getResponse().getWriter().write(String.valueOf(count));
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Error starring weblog", e);
        }
        
        // redirect back
        String referer = ServletActionContext.getRequest().getHeader("Referer");
        if (referer != null) {
            try { ServletActionContext.getResponse().sendRedirect(referer); } catch (Exception e) {}
        }
        return null;
    }
    
    public String entry() {
        try {
            if (getAuthenticatedUser() == null) {
                if ("XMLHttpRequest".equals(ServletActionContext.getRequest().getHeader("X-Requested-With"))) {
                    ServletActionContext.getResponse().setContentType("text/plain");
                    ServletActionContext.getResponse().getWriter().write("login_required");
                    return null;
                }
                return LOGIN;
            }
            UserManager umgr = WebloggerFactory.getWeblogger().getUserManager();
            WeblogEntryManager wemgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();
            WeblogEntry entry = wemgr.getWeblogEntry(getId());
            if (entry != null) {
                if (umgr.isWeblogEntryStarred(getAuthenticatedUser(), entry)) {
                    umgr.unstarWeblogEntry(getAuthenticatedUser(), entry);
                } else {
                    umgr.starWeblogEntry(getAuthenticatedUser(), entry);
                }
                WebloggerFactory.getWeblogger().flush();
                CacheManager.invalidate(entry);
                
                if ("XMLHttpRequest".equals(ServletActionContext.getRequest().getHeader("X-Requested-With"))) {
                    long count = umgr.getWeblogEntryStarCountFresh(entry);
                    ServletActionContext.getResponse().setContentType("text/plain");
                    ServletActionContext.getResponse().getWriter().write(String.valueOf(count));
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Error starring weblog entry", e);
        }
        
        // redirect back
        String referer = ServletActionContext.getRequest().getHeader("Referer");
        if (referer != null) {
            try { ServletActionContext.getResponse().sendRedirect(referer); } catch (Exception e) {}
        }
        return null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}