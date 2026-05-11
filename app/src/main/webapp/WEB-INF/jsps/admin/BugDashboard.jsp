<%-- Admin Bug Dashboard --%>
<%@ include file="/WEB-INF/jsps/taglibs-struts2.jsp" %>

<style>
.dashboard-header { background: #2c3e50; color: white; padding: 15px 20px; border-radius: 6px; margin-bottom: 20px; }
.dashboard-header h2 { margin: 0; }
.dashboard-header p { margin: 5px 0 0; opacity: 0.8; font-size: 0.9em; }
.stats-bar { display: flex; gap: 15px; margin-bottom: 20px; }
.stat-box { background: #f9f9f9; border: 1px solid #ddd; border-radius: 6px; padding: 10px 20px; text-align: center; }
.stat-box .num { font-size: 1.8em; font-weight: bold; }
.stat-box .lbl { font-size: 0.8em; color: #888; }
.bug-table th { background: #2c3e50; color: white; padding: 10px; text-align: left; }
.bug-table td { padding: 10px; border-bottom: 1px solid #eee; vertical-align: top; }
.bug-table tr:hover { background: #f9f9f9; }
.status-OPEN { background: #e67e22; color: white; padding: 2px 8px; border-radius: 10px; font-size: 0.85em; }
.status-RESOLVED { background: #27ae60; color: white; padding: 2px 8px; border-radius: 10px; font-size: 0.85em; }
.resolve-form input[type=text] { padding: 4px; border: 1px solid #ccc; border-radius: 3px; width: 150px; }
.btn-resolve { background: #27ae60; color: white; border: none; padding: 4px 10px; border-radius: 3px; cursor: pointer; }
.btn-delete { background: #e74c3c; color: white; border: none; padding: 4px 10px; border-radius: 3px; cursor: pointer; }
</style>

<div class="dashboard-header">
    <h2>🛠 Bug Report Dashboard</h2>
    <p>Review, resolve, or dismiss user-reported issues from across the site.</p>
</div>

<c:choose>
    <c:when test="${empty allBugReports}">
        <p style="color:#888;font-style:italic;">🎉 No bug reports have been submitted yet.</p>
    </c:when>
    <c:otherwise>
        <table class="rollertable bug-table" width="100%">
            <tr>
                <th>#</th>
                <th>Title &amp; Description</th>
                <th>Type</th>
                <th>Status</th>
                <th>Reporter</th>
                <th>Date</th>
                <th>Page</th>
                <th>Actions</th>
            </tr>
            <c:forEach var="bug" items="${allBugReports}" varStatus="loop">
                <tr>
                    <td>${loop.index + 1}</td>
                    <td>
                        <strong>${bug.title}</strong><br/>
                        <small style="color:#555;">${bug.description}</small>
                        <c:if test="${not empty bug.adminNotes}">
                            <br/><small style="color:#27ae60;"><em>Note: ${bug.adminNotes}</em></small>
                        </c:if>
                    </td>
                    <td><span style="font-size:0.85em;">${bug.bugType}</span></td>
                    <td><span class="status-${bug.status}">${bug.status}</span></td>
                    <td>${bug.submittedBy}</td>
                    <td><fmt:formatDate value="${bug.submittedDate}" pattern="MMM dd, yyyy"/></td>
                    <td>
                        <c:if test="${not empty bug.pageUrl}">
                            <a href="${bug.pageUrl}" target="_blank" style="font-size:0.85em;">View ↗</a>
                        </c:if>
                    </td>
                    <td>
                        <c:if test="${bug.status == 'OPEN'}">
                            <s:form action="bugDashboard!resolve" method="post" namespace="/roller-ui/admin" cssClass="resolve-form">
                                <input type="hidden" name="bugId" value="${bug.id}" />
                                <input type="text" name="adminNotes" placeholder="Add notes..." />
                                <button type="submit" class="btn-resolve">✔ Resolve</button>
                            </s:form>
                        </c:if>
                        <s:form action="bugDashboard!delete" method="post" namespace="/roller-ui/admin">
                            <input type="hidden" name="bugId" value="${bug.id}" />
                            <button type="submit" class="btn-delete"
                                onclick="return confirm('Delete this report permanently?');">🗑 Delete</button>
                        </s:form>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </c:otherwise>
</c:choose>
