<%-- Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. The ASF licenses
    this file to You under the Apache License, Version 2.0 (the "License" ); you may not use this file except in
    compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
    specific language governing permissions and limitations under the License. For additional information regarding
    copyright in this work, please see the NOTICE file in the top level directory of this distribution. --%>
    <%@ include file="/WEB-INF/jsps/taglibs-struts2.jsp" %>

        <style>
            .cb-form-row {
                margin-bottom: 14px;
            }

            .cb-form-row label {
                display: block;
                font-weight: bold;
                margin-bottom: 4px;
            }

            .cb-form-row select {
                width: 100%;
                max-width: 480px;
                padding: 6px 8px;
                border: 1px solid #ccc;
                border-radius: 3px;
                font-size: 14px;
            }

            .cb-hint {
                font-size: 12px;
                color: #666;
                margin-top: 3px;
            }

            .cb-recap {
                background: #f0f7ff;
                border-left: 4px solid #337ab7;
                padding: 12px 16px;
                margin: 16px 0;
                border-radius: 2px;
            }

            .cb-recap h4 {
                margin: 0 0 6px 0;
                color: #337ab7;
                font-size: 15px;
            }

            .cb-recap p {
                margin: 0;
                font-size: 14px;
                line-height: 1.6;
            }

            .cb-themes-title {
                font-size: 16px;
                font-weight: bold;
                margin: 20px 0 10px 0;
                border-bottom: 2px solid #ddd;
                padding-bottom: 4px;
            }

            .cb-theme {
                background: #fff;
                border: 1px solid #ddd;
                border-radius: 4px;
                margin-bottom: 12px;
                overflow: hidden;
            }

            .cb-theme-header {
                background: #f5f5f5;
                padding: 8px 14px;
                border-bottom: 1px solid #ddd;
                display: flex;
                align-items: center;
            }

            .cb-theme-num {
                background: #337ab7;
                color: #fff;
                border-radius: 50%;
                width: 22px;
                height: 22px;
                display: inline-flex;
                align-items: center;
                justify-content: center;
                font-size: 12px;
                font-weight: bold;
                margin-right: 10px;
                flex-shrink: 0;
            }

            .cb-theme-label {
                font-weight: bold;
                font-size: 14px;
                color: #333;
            }

            .cb-theme-body {
                padding: 10px 14px;
            }

            .cb-rep {
                background: #fafafa;
                border-left: 3px solid #aaa;
                padding: 6px 10px;
                margin: 6px 0;
                font-size: 13px;
                color: #444;
                font-style: italic;
                border-radius: 0 2px 2px 0;
            }

            .cb-rep:before {
                content: "\201C";
                color: #999;
                margin-right: 2px;
            }

            .cb-rep:after {
                content: "\201D";
                color: #999;
                margin-left: 2px;
            }

            .cb-method-badge {
                display: inline-block;
                background: #e8f4fd;
                color: #31708f;
                border: 1px solid #bce8f1;
                border-radius: 3px;
                padding: 3px 10px;
                font-size: 12px;
                margin-bottom: 12px;
            }

            .cb-no-comments {
                background: #fcf8e3;
                border: 1px solid #faebcc;
                color: #8a6d3b;
                padding: 10px 14px;
                border-radius: 3px;
                margin-top: 14px;
            }
        </style>

        <p class="subtitle">
            <s:text name="conversationBreakdown.subtitle">
                <s:param value="%{actionWeblog.handle}" />
            </s:text>
        </p>
        <p class="pagetip">
            <s:text name="conversationBreakdown.prompt" />
        </p>

        <%--=====Selection Form=====--%>
            <s:form action="conversationBreakdown" cssStyle="max-width:600px;">
                <s:hidden name="salt" />
                <s:hidden name="weblog" value="%{actionWeblog.handle}" />

                <div class="cb-form-row">
                    <label for="entryId">
                        <s:text name="conversationBreakdown.selectEntry" />
                    </label>
                    <s:select name="entryId" list="weblogEntries" listKey="key" listValue="value"
                        cssClass="form-control" headerKey=""
                        headerValue="%{getText('conversationBreakdown.selectEntry.prompt')}" />
                </div>

                <div class="cb-form-row">
                    <label for="method">
                        <s:text name="conversationBreakdown.selectMethod" />
                    </label>
                    <select name="method" id="method" class="form-control" style="max-width:480px;">
                        <option value="lightweight" <s:if test="method == 'lightweight' || method == null">
                            selected="selected"</s:if>>
                            Lightweight — Keyword Clustering (fast, no API cost)
                        </option>
                        <option value="llm" <s:if test="method == 'llm'"> selected="selected"</s:if>>
                            LLM — GroqCloud AI (richer results, uses API credits)
                        </option>
                    </select>
                    <div class="cb-hint">
                        <s:text name="conversationBreakdown.method.hint" />
                    </div>
                </div>

                <s:submit value="%{getText('conversationBreakdown.analyze')}" cssClass="btn btn-primary" />
            </s:form>

            <br />

            <%--=====Results=====--%>
                <s:if test="selectedEntry != null && breakdown != null">

                    <hr />
                    <h3 style="margin-bottom:4px;">
                        <s:text name="conversationBreakdown.resultsFor" />:
                        <em>
                            <s:property value="selectedEntry.title" />
                        </em>
                    </h3>
                    <div class="cb-method-badge">
                        Method:
                        <s:property value="breakdown.methodUsed" />
                    </div>

                    <%--=====1. Overall Recap=====--%>
                        <div class="cb-recap">
                            <h4>&#128172;
                                <s:text name="conversationBreakdown.recap.title" />
                            </h4>
                            <p>
                                <s:property value="breakdown.overallRecap" />
                            </p>
                        </div>

                        <%--=====2. Major Themes + Representative Comments=====--%>
                            <s:if test="breakdown.themes != null && !breakdown.themes.isEmpty()">
                                <div class="cb-themes-title">
                                    &#128200;
                                    <s:text name="conversationBreakdown.themes.title" />
                                    (
                                    <s:property value="breakdown.themes.size()" /> theme<s:if
                                        test="breakdown.themes.size() != 1">s</s:if>)
                                </div>

                                <s:iterator value="breakdown.themes" var="theme" status="st">
                                    <div class="cb-theme">
                                        <div class="cb-theme-header">
                                            <span class="cb-theme-num">
                                                <s:property value="#st.index + 1" />
                                            </span>
                                            <span class="cb-theme-label">
                                                <s:property value="#theme.themeLabel" />
                                            </span>
                                        </div>
                                        <div class="cb-theme-body">
                                            <s:if
                                                test="#theme.representativeComments != null && !#theme.representativeComments.isEmpty()">
                                                <s:iterator value="#theme.representativeComments" var="rep">
                                                    <div class="cb-rep">
                                                        <s:property value="#rep" />
                                                    </div>
                                                </s:iterator>
                                            </s:if>
                                            <s:else>
                                                <span style="color:#999;font-size:13px;font-style:italic;">
                                                    <s:text name="conversationBreakdown.noRepresentative" />
                                                </span>
                                            </s:else>
                                        </div>
                                    </div>
                                </s:iterator>
                            </s:if>
                            <s:else>
                                <div class="cb-no-comments">
                                    <s:text name="conversationBreakdown.noThemes" />
                                </div>
                            </s:else>

                </s:if>
                <s:elseif test="selectedEntry != null && breakdown == null">
                    <div class="cb-no-comments">
                        <s:text name="conversationBreakdown.noComments" />
                    </div>
                </s:elseif>