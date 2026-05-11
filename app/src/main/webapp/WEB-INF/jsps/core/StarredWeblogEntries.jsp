<%@ include file="/WEB-INF/jsps/taglibs-struts2.jsp" %>

    <div class="row">
        <div class="col-md-12">
            <h2>
                <s:text name="Starred Blogposts" />
            </h2>
        </div>
    </div>

    <s:if test="!entries.isEmpty">
        <s:iterator var="entry" value="entries">
            <div class="well yourWeblogBox">
                <h3 class="mm_weblog_name">
                    <a href="<s:property value=" #entry.permalink" />">
                    <span class="glyphicon glyphicon-star" aria-hidden="true"></span>
                    <s:property value="#entry.title" />
                    </a>
                </h3>

                <p><strong>Blog:</strong>
                    <s:property value="#entry.website.name" />
                </p>
                <p><strong>Published:</strong>
                    <s:property value="#entry.formatPubTime('yyyy-MM-dd HH:mm:ss')" />
                </p>
                <p><a href="<s:url action='star' method='entry'><s:param name='id' value='#entry.id'/></s:url>">Remove
                        Star</a></p>
            </div>
        </s:iterator>

        <div class="row">
            <div class="col-md-12">
                <ul class="pager">
                    <s:if test="page > 0">
                        <li class="previous">
                            <a
                                href="<s:url action='starredWeblogEntries'><s:param name='page' value='page - 1'/></s:url>">&larr;
                                Previous</a>
                        </li>
                    </s:if>
                    <s:if test="hasMore">
                        <li class="next">
                            <a
                                href="<s:url action='starredWeblogEntries'><s:param name='page' value='page + 1'/></s:url>">Next
                                &rarr;</a>
                        </li>
                    </s:if>
                </ul>
            </div>
        </div>
    </s:if>
    <s:else>
        <div class="row">
            <div class="col-md-12">
                <p>You have no starred blogposts.</p>
            </div>
        </div>
    </s:else>