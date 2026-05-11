<%-- Bug Report submission page for users --%>
<%@ include file="/WEB-INF/jsps/taglibs-struts2.jsp" %>

<style>
.bug-form-container { background: #f9f9f9; border: 1px solid #ddd; border-radius: 6px; padding: 20px; margin-bottom: 20px; }
.bug-form-container h3 { color: #c0392b; margin-top: 0; }
.bug-info { background: #eaf4fb; border-left: 4px solid #3498db; padding: 10px 15px; margin-bottom: 15px; font-size: 0.9em; }
.formtable td.label { font-weight: bold; color: #333; padding-right: 15px; vertical-align: top; padding-top: 8px; }
.formtable td.field { padding-bottom: 10px; }
.formtable input[type=text], .formtable textarea, .formtable select { width: 400px; padding: 6px; border: 1px solid #ccc; border-radius: 4px; }
.formtable textarea { height: 100px; }
.status-OPEN { color: #e67e22; font-weight: bold; }
.status-RESOLVED { color: #27ae60; font-weight: bold; }
.bug-table th { background: #34495e; color: white; padding: 8px; }
.bug-table td { padding: 8px; border-bottom: 1px solid #eee; vertical-align: top; }
.bug-table tr:hover { background: #f5f5f5; }
.severity-HIGH { color: #e74c3c; font-weight: bold; }
.severity-MEDIUM { color: #e67e22; }
.severity-LOW { color: #27ae60; }
</style>

<div class="bug-form-container">
    <h3>🐛 Report an Issue</h3>
    <div class="bug-info">
        <strong>What counts as an issue?</strong> Anything that affects your experience — 
        a broken button, a confusing UI element, slow page loads, incorrect behavior, 
        missing features, or even a typo. Your feedback helps improve Roller for everyone!
    </div>

    <s:form action="bugReport!submit" method="post" namespace="/roller-ui/authoring">
        <s:hidden name="weblog" value="%{actionWeblog.handle}" />
        <table class="formtable">
            <tr>
                <td class="label"><label for="title">Issue Title *</label></td>
                <td class="field">
                    <s:textfield name="title" size="50" maxlength="255" required="true"
                        placeholder="Short, descriptive title of the issue" />
                    <div style="font-size:0.8em;color:#888;">e.g. "Save button does nothing on Entry Edit page"</div>
                </td>
            </tr>
            <tr>
                <td class="label"><label for="bugType">Issue Type *</label></td>
                <td class="field">
                    <s:select name="bugType" list="#{'BROKEN_BUTTON':'🔴 Broken Button','BROKEN_LINK':'🔗 Broken Link','UI_ISSUE':'🎨 UI / Display Issue','PERFORMANCE':'⚡ Performance Problem','DATA_ISSUE':'📊 Wrong / Missing Data','SECURITY':'🔒 Security Concern','OTHER':'💬 Other'}" />
                </td>
            </tr>
            <tr>
                <td class="label"><label for="pageUrl">Where did it happen?</label></td>
                <td class="field">
                    <s:textfield name="pageUrl" size="50" maxlength="255"
                        placeholder="e.g. http://localhost:8080/roller-ui/authoring/entryEdit" />
                    <div style="font-size:0.8em;color:#888;">Paste the URL of the page where you found the issue</div>
                </td>
            </tr>
            <tr>
                <td class="label"><label for="description">Description *</label></td>
                <td class="field">
                    <s:textarea name="description" rows="6" cols="50" required="true"
                        placeholder="Describe the issue in detail. What did you expect? What happened instead? Steps to reproduce?" />
                </td>
            </tr>
            <tr>
                <td></td>
                <td class="field">
                    <input type="submit" value="🚀 Submit Issue Report"
                        style="background:#c0392b;color:white;padding:8px 16px;border:none;border-radius:4px;cursor:pointer;font-size:1em;" />
                </td>
            </tr>
        </table>
    </s:form>
</div>

<h3>📋 My Submitted Issues</h3>

<c:choose>
    <c:when test="${empty myBugReports}">
        <p style="color:#888;font-style:italic;">You have not submitted any issue reports yet. Use the form above to report your first issue!</p>
    </c:when>
    <c:otherwise>
        <table class="rollertable bug-table" width="100%">
            <tr>
                <th>#</th>
                <th>Title</th>
                <th>Type</th>
                <th>Status</th>
                <th>Submitted</th>
                <th>Admin Response</th>
                <th>Action</th>
            </tr>
            <c:forEach var="bug" items="${myBugReports}" varStatus="loop">
                <tr>
                    <td>${loop.index + 1}</td>
                    <td><strong>${bug.title}</strong>
                        <c:if test="${not empty bug.pageUrl}">
                            <br/><small><a href="${bug.pageUrl}" target="_blank">View page ↗</a></small>
                        </c:if>
                    </td>
                    <td>${bug.bugType}</td>
                    <td>
                        <span class="status-${bug.status}">${bug.status}</span>
                    </td>
                    <td><fmt:formatDate value="${bug.submittedDate}" pattern="MMM dd, yyyy"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${not empty bug.adminNotes}">
                                <em>${bug.adminNotes}</em>
                            </c:when>
                            <c:otherwise>
                                <span style="color:#aaa;">Pending review</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:if test="${bug.status == 'OPEN'}">
                            <s:form action="bugReport!delete" method="post" namespace="/roller-ui/authoring">
                                <s:hidden name="weblog" value="%{actionWeblog.handle}" />
                                <input type="hidden" name="bugId" value="${bug.id}" />
                                <input type="submit" value="🗑 Delete"
                                    style="background:#e74c3c;color:white;border:none;padding:4px 10px;border-radius:3px;cursor:pointer;"
                                    onclick="return confirm('Are you sure you want to delete this issue report?');" />
                            </s:form>
                        </c:if>
                        <c:if test="${bug.status == 'RESOLVED'}">
                            <span style="color:#27ae60;">✅ Resolved</span>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </c:otherwise>
</c:choose>
