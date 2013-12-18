/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.issue;

import org.sonar.api.ServerComponent;

import java.util.Map;

/**
 * Facade for JRuby on Rails extensions to request issues.
 * <p>
 * Reference from Ruby code : <code>Api.issues</code>
 * </p>
 *
 * @since 3.6
 */
public interface RubyIssueService extends ServerComponent {

  /**
   * Search for an issue by its key.
   * <p/>
   * Ruby example: <code>result = Api.issues.find('ABCDE-12345')</code>
   */
  IssueQueryResult find(String issueKey);

  /**
   * Search for issues.
   * <p/>
   * Ruby example: <code>Api.issues.find({'statuses' => ['OPEN', 'RESOLVED'], 'assignees' => 'john,carla')}</code>
   * <p/>
   * <b>Keys of parameters must be Ruby strings but not symbols</b>. Multi-value parameters can be arrays (<code>['OPEN', 'RESOLVED']</code>) or
   * comma-separated list of strings (<code>'OPEN,RESOLVED'</code>).
   * <p/>
   * Optional parameters are:
   * <ul>
   *   <li>'issues': list of issue keys</li>
   *   <li>'severities': list of severity to match. See constants in {@link org.sonar.api.rule.Severity}</li>
   *   <li>'statuses': list of status to match. See constants in {@link Issue}</li>
   *   <li>'resolutions': list of resolutions to match. See constants in {@link Issue}</li>
   *   <li>'resolved': true to match only resolved issues, false to match only unresolved issues. By default no filtering is done.</li>
   *   <li>'components': list of component keys to match, for example 'org.apache.struts:struts:org.apache.struts.Action'</li>
   *   <li>'componentRoots': list of keys of root components. All the issues related to descendants of these roots are returned.</li>
   *   <li>'rules': list of keys of rules to match. Format is &lt;repository&gt;:&lt;rule&gt;, for example 'squid:AvoidCycles'</li>
   *   <li>'actionPlans': list of keys of the action plans to match. Note that plan names are not accepted.</li>
   *   <li>'planned': true to get only issues associated to an action plan, false to get only non associated issues. By default no filtering is done.</li>
   *   <li>'reporters': list of reporter logins. Note that reporters are defined only on "manual" issues.</li>
   *   <li>'assignees': list of assignee logins.</li>
   *   <li>'assigned': true to get only assigned issues, false to get only not assigned issues. By default no filtering is done.</li>
   *   <li>'createdAfter': match all the issues created after the given date (strictly).
   *   Both date and datetime ISO formats are supported: 2013-05-18 or 2010-05-18T15:50:45+0100</li>
   *   <li>'createdAt': match all the issues created at the given date (require second precision).
   *   Both date and datetime ISO formats are supported: 2013-05-18 or 2010-05-18T15:50:45+0100</li>
   *   <li>'createdBefore': match all the issues created before the given date (exclusive).
   *   Both date and datetime ISO formats are supported: 2013-05-18 or 2010-05-18T15:50:45+0100</li>
   *   <li>'pageSize': maximum number of results per page. Default is {@link org.sonar.api.issue.IssueQuery#DEFAULT_PAGE_SIZE},
   *   except when the parameter 'components' is set. In this case there's no limit by default (all results in the same page).</li>
   *   <li>'pageIndex': index of the selected page. Default is 1.</li>
   *   <li>'sort': field to sort on. See supported values in {@link IssueQuery}</li>
   *   <li>'asc': ascending or descending sort? Value can be a boolean or strings 'true'/'false'</li>
   * </ul>
   */
  IssueQueryResult find(Map<String, Object> parameters);

}
