package org.apache.roller.weblogger.ui.struts2.core;

import java.util.Collections;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.business.UserManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;

/**
 * Action for the Trending Blogs page (Feature 1B).
 *
 * Fetches the top 5 most-starred blog pages and blog entries using
 * efficient GROUP BY aggregate queries — no per-article iteration.
 */
public class TrendingBlogs extends UIAction {

    private static final Log log = LogFactory.getLog(TrendingBlogs.class);

    /** Each element is Object[]{WeblogEntry, Long starCount} */
    private List<Object[]> trendingEntries = Collections.emptyList();

    /** Each element is Object[]{Weblog, Long starCount} */
    private List<Object[]> trendingWeblogs = Collections.emptyList();

    public TrendingBlogs() {
        this.pageTitle = "Trending Blogs";
    }

    @Override
    public boolean isUserRequired() {
        return false;
    }

    @Override
    public boolean isWeblogRequired() {
        return false;
    }

    @Override
    public String execute() {
        try {
            UserManager umgr = WebloggerFactory.getWeblogger().getUserManager();
            trendingEntries = umgr.getTopStarredWeblogEntries(5);
            trendingWeblogs = umgr.getTopStarredWeblogs(5);
        } catch (Exception e) {
            log.error("Error fetching trending blogs", e);
        }
        return SUCCESS;
    }

    public List<Object[]> getTrendingEntries() {
        return trendingEntries;
    }

    public List<Object[]> getTrendingWeblogs() {
        return trendingWeblogs;
    }
}
