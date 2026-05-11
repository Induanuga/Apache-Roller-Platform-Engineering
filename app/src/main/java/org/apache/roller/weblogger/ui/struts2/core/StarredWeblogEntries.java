package org.apache.roller.weblogger.ui.struts2.core;

import java.util.Collections;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.business.UserManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;

public class StarredWeblogEntries extends UIAction {
    private static Log log = LogFactory.getLog(StarredWeblogEntries.class);
    
    private int page = 0;
    private List<WeblogEntry> entries;
    private boolean hasMore;
    
    public StarredWeblogEntries() {
        this.pageTitle = "Starred Blogposts";
    }
    
    @Override
    public boolean isWeblogRequired() {
        return false;
    }
    
    @Override
    public String execute() {
        int length = 10;
        int offset = page * length;
        try {
            UserManager umgr = WebloggerFactory.getWeblogger().getUserManager();
            List<WeblogEntry> allEntries = umgr.getStarredWeblogEntries(getAuthenticatedUser(), offset, length + 1);
            if (allEntries.size() > length) {
                hasMore = true;
                entries = allEntries.subList(0, length);
            } else {
                entries = allEntries;
            }
        } catch (Exception e) {
            log.error("Error fetching starred entries", e);
            entries = Collections.emptyList();
        }
        return SUCCESS;
    }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public List<WeblogEntry> getEntries() { return entries; }
    public boolean isHasMore() { return hasMore; }
}
