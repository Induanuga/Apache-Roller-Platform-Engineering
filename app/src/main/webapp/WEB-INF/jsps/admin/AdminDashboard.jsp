
<%--
  Admin Dashboard JSP — View layer only.
  Renders metrics whose keys appear in visibleMetrics (set by DashboardViewStrategy).
  Contains NO data fetching logic.
--%>
<%@ include file="/WEB-INF/jsps/taglibs-struts2.jsp" %>

<div class="container-fluid">

    <%-- Page Header with view toggle buttons --%>
    <div class="row" style="margin-bottom: 20px;">
        <div class="col-md-8">
            <h2>
                <span class="glyphicon glyphicon-dashboard" aria-hidden="true"></span>
                Admin Dashboard
                <small><s:property value="viewLabel"/></small>
            </h2>
        </div>
        <div class="col-md-4 text-right">
            <a href="<s:url action='adminDashboard' namespace='/roller-ui/admin'>
                         <s:param name='viewMode'>minimal</s:param>
                     </s:url>"
               class="btn <s:if test='viewMode == \"minimal\"'>btn-primary</s:if><s:else>btn-default</s:else>">
                Minimal View
            </a>
            &nbsp;
            <a href="<s:url action='adminDashboard' namespace='/roller-ui/admin'>
                         <s:param name='viewMode'>full</s:param>
                     </s:url>"
               class="btn <s:if test='viewMode != \"minimal\"'>btn-primary</s:if><s:else>btn-default</s:else>">
                Full View
            </a>
        </div>
    </div>

    <%-- Row 1: Site-wide counts — shown in full view only --%>
    <s:if test="visibleMetrics.contains('totalWeblogs')">
    <div class="row" style="margin-bottom: 20px;">
        <div class="col-md-3">
            <div class="panel panel-info">
                <div class="panel-heading">Total Weblogs</div>
                <div class="panel-body text-center">
                    <h2><s:property value="dashboardData.totalWeblogs"/></h2>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="panel panel-success">
                <div class="panel-heading">Total Entries</div>
                <div class="panel-body text-center">
                    <h2><s:property value="dashboardData.totalEntries"/></h2>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="panel panel-warning">
                <div class="panel-heading">Total Comments</div>
                <div class="panel-body text-center">
                    <h2><s:property value="dashboardData.totalComments"/></h2>
                </div>
            </div>
        </div>
    </div>
    </s:if>

    <%-- Metric: Total Users — shown in BOTH minimal and full view --%>
    <s:if test="visibleMetrics.contains('totalUsers')">
    <div class="row" style="margin-bottom: 20px;">
        <div class="col-md-3">
            <div class="panel panel-primary">
                <div class="panel-heading">
                    <span class="glyphicon glyphicon-user"></span> Total Users
                </div>
                <div class="panel-body text-center">
                    <h2><s:property value="dashboardData.totalUsers"/></h2>
                </div>
            </div>
        </div>
    </div>
    </s:if>

    <%-- Metric: Top Category — shown in BOTH minimal and full view --%>
    <s:if test="visibleMetrics.contains('topCategory')">
    <div class="row" style="margin-bottom: 20px;">
        <div class="col-md-6">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <span class="glyphicon glyphicon-tags"></span> Top Performing Category
                </div>
                <div class="panel-body">
                    <strong><s:property value="dashboardData.topCategory"/></strong>
                    &nbsp;&mdash;&nbsp;
                    <s:property value="dashboardData.topCategoryCount"/> entries
                </div>
            </div>
        </div>
    </div>
    </s:if>

    <%-- Metric: Most Starred Blog — full view only --%>
    <s:if test="visibleMetrics.contains('mostStarredBlog')">
    <div class="row" style="margin-bottom: 20px;">
        <div class="col-md-6">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <span class="glyphicon glyphicon-star"></span> Most Starred Blog
                </div>
                <div class="panel-body">
                    <s:if test="dashboardData.mostStarredBlog != null">
                        <strong><s:property value="dashboardData.mostStarredBlog.name"/></strong>
                        &nbsp;&mdash;&nbsp;
                        <s:property value="dashboardData.mostStarredBlogCount"/> stars
                        <br/>
                        <small>
                            <a href="<s:property value='dashboardData.mostStarredBlog.absoluteURL'/>">
                                <s:property value="dashboardData.mostStarredBlog.absoluteURL"/>
                            </a>
                        </small>
                    </s:if>
                    <s:else>No starred blogs yet.</s:else>
                </div>
            </div>
        </div>
    </div>
    </s:if>

    <%-- Metric: Most Starred Entry — full view only --%>
    <s:if test="visibleMetrics.contains('mostStarredEntry')">
    <div class="row" style="margin-bottom: 20px;">
        <div class="col-md-6">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <span class="glyphicon glyphicon-star-empty"></span> Most Starred Entry
                </div>
                <div class="panel-body">
                    <s:if test="dashboardData.mostStarredEntry != null">
                        <strong><s:property value="dashboardData.mostStarredEntry.title"/></strong>
                        &nbsp;&mdash;&nbsp;
                        <s:property value="dashboardData.mostStarredEntryCount"/> stars
                        <br/>
                        <small>in blog:
                            <s:property value="dashboardData.mostStarredEntry.website.name"/>
                        </small>
                    </s:if>
                    <s:else>No starred entries yet.</s:else>
                </div>
            </div>
        </div>
    </div>
    </s:if>

    <%-- Metric: Top 3 Most Active Users — full view only --%>
    <s:if test="visibleMetrics.contains('topActiveUsers')">
    <div class="row" style="margin-bottom: 20px;">
        <div class="col-md-6">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <span class="glyphicon glyphicon-fire"></span> Top 3 Most Active Users
                </div>
                <div class="panel-body">
                    <s:if test="dashboardData.topActiveUsers != null && !dashboardData.topActiveUsers.isEmpty()">
                        <table class="table table-condensed">
                            <thead>
                                <tr>
                                    <th>#</th>
                                    <th>Username</th>
                                    <th>Total Entries</th>
                                </tr>
                            </thead>
                            <tbody>
                                <s:iterator value="dashboardData.topActiveUsers" var="row" status="st">
                                <tr>
                                    <td><s:property value="#st.index + 1"/></td>
                                    <td><s:property value="#row[0]"/></td>
                                    <td><s:property value="#row[1]"/></td>
                                </tr>
                                </s:iterator>
                            </tbody>
                        </table>
                    </s:if>
                    <s:else>No user activity data available.</s:else>
                </div>
            </div>
        </div>
    </div>
    </s:if>

    <%-- Metric: Most Commented Blog — full view only --%>
    <s:if test="visibleMetrics.contains('mostCommentedBlog')">
    <div class="row" style="margin-bottom: 20px;">
        <div class="col-md-6">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <span class="glyphicon glyphicon-comment"></span> Most Commented Blog
                </div>
                <div class="panel-body">
                    <s:if test="dashboardData.mostCommentedBlogName != null">
                        <strong><s:property value="dashboardData.mostCommentedBlogName"/></strong>
                        &nbsp;&mdash;&nbsp;
                        <s:property value="dashboardData.mostCommentedBlogCount"/> comments
                    </s:if>
                    <s:else>No comment data available.</s:else>
                </div>
            </div>
        </div>
    </div>
    </s:if>

</div>
