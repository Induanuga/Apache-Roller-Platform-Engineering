<%--
  Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  The ASF licenses this file to You
  under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.  For additional information regarding
  copyright in this work, please see the NOTICE file in the top level
  directory of this distribution.
--%>
<%@ include file="/WEB-INF/jsps/taglibs-struts2.jsp" %>

<p class="subtitle">
    <s:text name="discussionOverview.subtitle">
        <s:param value="%{actionWeblog.handle}"/>
    </s:text>
</p>

<p class="pagetip">
    <s:text name="discussionOverview.prompt"/>
</p>

<%-- Entry Selection Form --%>
<s:form action="discussionOverview">
    <s:hidden name="salt"/>
    <s:hidden name="weblog" value="%{actionWeblog.handle}"/>
    
    <div class="form-group">
        <label for="entryId"><s:text name="discussionOverview.selectEntry"/></label>
        <s:select 
            name="entryId" 
            list="weblogEntries" 
            listKey="key" 
            listValue="value"
            cssClass="form-control"
            headerKey="" 
            headerValue="%{getText('discussionOverview.selectEntry.prompt')}"
        />
    </div>
    
    <s:submit value="%{getText('discussionOverview.analyze')}" cssClass="btn btn-primary"/>
</s:form>

<br/>

<%-- Display Results if an entry is selected --%>
<s:if test="selectedEntry != null">
    <h3><s:text name="discussionOverview.resultsFor"/>: <s:property value="selectedEntry.title"/></h3>
    
    <s:if test="indicatorResults != null && !indicatorResults.isEmpty">
        <div class="panel panel-default">
            <div class="panel-heading">
                <s:text name="discussionOverview.indicators.title"/>
            </div>
            <div class="panel-body">
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <th><s:text name="discussionOverview.indicator.label"/></th>
                            <th><s:text name="discussionOverview.indicator.value"/></th>
                        </tr>
                    </thead>
                    <tbody>
                        <s:iterator value="indicatorResults" var="result">
                            <tr>
                                <td><strong><s:property value="#result.label"/></strong></td>
                                <td><s:property value="#result.value"/></td>
                            </tr>
                        </s:iterator>
                    </tbody>
                </table>
            </div>
        </div>
        
        <div class="alert alert-info" role="alert">
            <h4><s:text name="discussionOverview.legend.title"/></h4>
            <ul>
                <li><strong><s:text name="discussionOverview.legend.activity.label"/>:</strong> 
                    <s:text name="discussionOverview.legend.activity.desc"/>
                </li>
                <li><strong><s:text name="discussionOverview.legend.responseTypes.label"/>:</strong> 
                    <s:text name="discussionOverview.legend.responseTypes.desc"/>
                </li>
                <li><strong><s:text name="discussionOverview.legend.keywords.label"/>:</strong> 
                    <s:text name="discussionOverview.legend.keywords.desc"/>
                </li>
            </ul>
        </div>
    </s:if>
    <s:else>
        <div class="alert alert-warning" role="alert">
            <s:text name="discussionOverview.noComments"/>
        </div>
    </s:else>
</s:if>
