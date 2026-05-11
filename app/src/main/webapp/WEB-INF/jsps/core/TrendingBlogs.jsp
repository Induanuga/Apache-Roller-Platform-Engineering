<%@ include file="/WEB-INF/jsps/taglibs-struts2.jsp" %>

    <div class="row">
        <div class="col-md-12">
            <h2>&#x1F525; Trending Blogs</h2>
            <p class="text-muted">Top 5 most-starred blog pages and blogposts across the site.</p>
        </div>
    </div>

    <%-- Top 5 Trending Blog Pages --%>
        <div class="row">
            <div class="col-md-12">
                <h3><span class="glyphicon glyphicon-star" aria-hidden="true"></span> Top 5 Trending Blog Pages</h3>
            </div>
        </div>

        <s:if test="!trendingWeblogs.isEmpty">
            <s:iterator var="row" value="trendingWeblogs" status="st">
                <div class="well yourWeblogBox">
                    <span class="badge pull-right" style="font-size:1em; background-color:#e8a838;">
                        <span class="glyphicon glyphicon-star" aria-hidden="true"></span>&nbsp;
                        <s:property value="#row[1]" /> stars
                    </span>
                    <h4 class="mm_weblog_name">
                        <span class="glyphicon glyphicon-folder-open" aria-hidden="true"></span>&nbsp;
                        <a href="<s:property value=" #row[0].absoluteURL" />">
                        <s:property value="#row[0].name" />
                        </a>
                    </h4>
                    <p class="text-muted">
                        <s:property value="#row[0].about" />
                    </p>
                    <p>
                        <a href="<s:property value=" #row[0].absoluteURL" />" class="btn btn-xs btn-default">Visit
                        Blog</a>
                    </p>
                </div>
            </s:iterator>
        </s:if>
        <s:else>
            <div class="row">
                <div class="col-md-12">
                    <p class="text-muted">No blog pages have been starred yet.</p>
                </div>
            </div>
        </s:else>

        <hr />

        <%-- Top 5 Trending Blogposts --%>
            <div class="row">
                <div class="col-md-12">
                    <h3><span class="glyphicon glyphicon-star" aria-hidden="true"></span> Top 5 Trending Blogposts</h3>
                </div>
            </div>

            <s:if test="!trendingEntries.isEmpty">
                <s:iterator var="row" value="trendingEntries" status="st">
                    <div class="well yourWeblogBox">
                        <span class="badge pull-right" style="font-size:1em; background-color:#e8a838;">
                            <span class="glyphicon glyphicon-star" aria-hidden="true"></span>&nbsp;
                            <s:property value="#row[1]" /> stars
                        </span>
                        <h4 class="mm_weblog_name">
                            <span class="glyphicon glyphicon-pencil" aria-hidden="true"></span>&nbsp;
                            <a href="<s:property value=" #row[0].permalink" />">
                            <s:property value="#row[0].title" />
                            </a>
                        </h4>
                        <p>
                            <strong>Blog:</strong>
                            <s:property value="#row[0].website.name" />&nbsp;&nbsp;
                            <strong>Published:</strong>
                            <s:property value="#row[0].formatPubTime('yyyy-MM-dd HH:mm')" />
                        </p>
                        <p>
                            <a href="<s:property value=" #row[0].permalink" />" class="btn btn-xs btn-default">Read
                            Post</a>
                        </p>
                    </div>
                </s:iterator>
            </s:if>
            <s:else>
                <div class="row">
                    <div class="col-md-12">
                        <p class="text-muted">No blogposts have been starred yet.</p>
                    </div>
                </div>
            </s:else>

            <div class="row" style="margin-top:20px;">
                <div class="col-md-12">
                    <a href="<s:url action='menu'/>" class="btn btn-default">&larr; Back to Main Menu</a>
                </div>
            </div>