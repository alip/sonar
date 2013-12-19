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
package org.sonar.plugins.core;

import com.google.common.collect.ImmutableList;
import org.sonar.api.*;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Qualifiers;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.core.technicaldebt.TechnicalDebtConverter;
import org.sonar.core.timemachine.Periods;
import org.sonar.plugins.core.batch.IndexProjectPostJob;
import org.sonar.plugins.core.charts.DistributionAreaChart;
import org.sonar.plugins.core.charts.DistributionBarChart;
import org.sonar.plugins.core.charts.XradarChart;
import org.sonar.plugins.core.colorizers.JavaColorizerFormat;
import org.sonar.plugins.core.dashboards.*;
import org.sonar.plugins.core.issue.*;
import org.sonar.plugins.core.issue.ignore.IgnoreIssuesPlugin;
import org.sonar.plugins.core.issue.notification.*;
import org.sonar.plugins.core.measurefilters.MyFavouritesFilter;
import org.sonar.plugins.core.measurefilters.ProjectFilter;
import org.sonar.plugins.core.notifications.alerts.NewAlerts;
import org.sonar.plugins.core.security.ApplyProjectRolesDecorator;
import org.sonar.plugins.core.sensors.*;
import org.sonar.plugins.core.technicaldebt.NewTechnicalDebtDecorator;
import org.sonar.plugins.core.technicaldebt.TechnicalDebtDecorator;
import org.sonar.plugins.core.timemachine.*;
import org.sonar.plugins.core.web.TestsViewer;
import org.sonar.plugins.core.widgets.*;
import org.sonar.plugins.core.widgets.issues.*;
import org.sonar.plugins.core.widgets.measures.MeasureFilterAsBubbleChartWidget;
import org.sonar.plugins.core.widgets.measures.MeasureFilterAsCloudWidget;
import org.sonar.plugins.core.widgets.measures.MeasureFilterAsHistogramWidget;
import org.sonar.plugins.core.widgets.measures.MeasureFilterAsPieChartWidget;

import java.util.Arrays;
import java.util.List;

@Properties({
  @Property(
    key = CoreProperties.TASK,
    name = "Task to be executed",
    defaultValue = CoreProperties.SCAN_TASK,
    module = false,
    project = false,
    global = false),
  @Property(
    key = CoreProperties.SERVER_BASE_URL,
    defaultValue = CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE,
    name = "Server base URL",
    description = "HTTP URL of this SonarQube server, such as <i>http://yourhost.yourdomain/sonar</i>. This value is used i.e. to create links in emails.",
    project = false,
    global = true,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_HOME_PAGE,
    defaultValue = "",
    name = "Project Home Page",
    description = "HTTP URL of the home page of the project.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_CI,
    defaultValue = "",
    name = "CI server",
    description = "HTTP URL of the continuous integration server.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_ISSUE_TRACKER,
    defaultValue = "",
    name = "Issue Tracker",
    description = "HTTP URL of the issue tracker.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_SOURCES,
    defaultValue = "",
    name = "SCM server",
    description = "HTTP URL of the server which hosts the sources of the project.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_SOURCES_DEV,
    defaultValue = "",
    name = "SCM connection for developers",
    description = "HTTP URL used by developers to connect to the SCM server for the project.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.PROJECT_LANGUAGE_PROPERTY,
    defaultValue = Java.KEY,
    name = "Default language",
    description = "Default language of the source code to analyse",
    project = false,
    global = true,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.ANALYSIS_MODE,
    defaultValue = CoreProperties.ANALYSIS_MODE_ANALYSIS,
    name = "Analysis mode",
    type = PropertyType.SINGLE_SELECT_LIST,
    options = {CoreProperties.ANALYSIS_MODE_ANALYSIS, CoreProperties.ANALYSIS_MODE_PREVIEW, CoreProperties.ANALYSIS_MODE_INCREMENTAL},
    global = false, project = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.PREVIEW_INCLUDE_PLUGINS,
    deprecatedKey = CoreProperties.DRY_RUN_INCLUDE_PLUGINS,
    name = "Plugins accepted for Preview and Incremental modes",
    description = "Comma-separated list of plugin keys. Those plugins will be used during preview or incremental analyses.",
    defaultValue = CoreProperties.PREVIEW_INCLUDE_PLUGINS_DEFAULT_VALUE,
    global = true, project = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.PREVIEW_EXCLUDE_PLUGINS,
    deprecatedKey = CoreProperties.DRY_RUN_EXCLUDE_PLUGINS,
    name = "Plugins excluded for Preview and Incremental modes",
    description = "Comma-separated list of plugin keys. Those plugins will not be used during preview or incremental analyses.",
    global = true, project = false,
    defaultValue = CoreProperties.PREVIEW_EXCLUDE_PLUGINS_DEFAULT_VALUE,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = "sonar.report.export.path",
    defaultValue = "sonar-report.json",
    name = "Report Results Export File",
    type = PropertyType.STRING,
    global = false, project = false),

  // SERVER-SIDE TECHNICAL PROPERTIES

  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_REALM,
    name = "Security Realm",
    project = false,
    global = false
  ),
  @Property(
    key = "sonar.security.savePassword",
    name = "Save external password",
    project = false,
    global = false
  ),
  @Property(
    key = "sonar.authenticator.downcase",
    name = "Downcase login",
    description = "Downcase login during user authentication, typically for Active Directory",
    project = false,
    global = false,
    defaultValue = "false",
    type = PropertyType.BOOLEAN),
  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_CREATE_USERS,
    name = "Create user accounts",
    description = "Create accounts when authenticating users via an external system",
    project = false,
    global = false,
    defaultValue = "true",
    type = PropertyType.BOOLEAN),
  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_UPDATE_USER_ATTRIBUTES,
    name = "Update user attributes",
    description = "When using the LDAP or OpenID plugin, at each login, the user attributes (name, email, ...) are re-synchronized",
    project = false,
    global = false,
    defaultValue = "true",
    type = PropertyType.BOOLEAN),
  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_IGNORE_STARTUP_FAILURE,
    name = "Ignore failures during authenticator startup",
    defaultValue = "false",
    project = false,
    global = false,
    type = PropertyType.BOOLEAN),
  @Property(
    key = "sonar.enableFileVariation",
    name = "Enable file variation",
    global = false,
    defaultValue = "false",
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_LOCAL_USERS,
    name = "Local/technical users",
    description = "Comma separated list of user logins that will always be authenticated using SonarQube database. When using the LDAP plugin, for these accounts, the user attributes (name, email, ...) are not re-synchronized",
    project = false,
    global = false,
    defaultValue = "admin",
    type = PropertyType.STRING,
    multiValues = true)
})
public final class CorePlugin extends SonarPlugin {

  @SuppressWarnings("rawtypes")
  public List getExtensions() {
    ImmutableList.Builder<Object> extensions = ImmutableList.builder();

    extensions.add(
      DefaultResourceTypes.class,
      UserManagedMetrics.class,
      Periods.class,

      // pages
      TestsViewer.class,

      // measure filters
      ProjectFilter.class,
      MyFavouritesFilter.class,

      // widgets
      AlertsWidget.class,
      CoverageWidget.class,
      ItCoverageWidget.class,
      DescriptionWidget.class,
      ComplexityWidget.class,
      IssuesWidget.class,
      SizeWidget.class,
      EventsWidget.class,
      CustomMeasuresWidget.class,
      TimelineWidget.class,
      BubbleChartWidget.class,
      TimeMachineWidget.class,
      HotspotMetricWidget.class,
      TreemapWidget.class,
      MeasureFilterListWidget.class,
      MeasureFilterTreemapWidget.class,
      WelcomeWidget.class,
      DocumentationCommentsWidget.class,
      DuplicationsWidget.class,
      TechnicalDebtPyramidWidget.class,
      MeasureFilterAsPieChartWidget.class,
      MeasureFilterAsCloudWidget.class,
      MeasureFilterAsHistogramWidget.class,
      MeasureFilterAsBubbleChartWidget.class,

      // dashboards
      ProjectDefaultDashboard.class,
      ProjectHotspotDashboard.class,
      ProjectIssuesDashboard.class,
      ProjectTimeMachineDashboard.class,
      GlobalDefaultDashboard.class,

      // chart
      XradarChart.class,
      DistributionBarChart.class,
      DistributionAreaChart.class,

      // colorizers
      JavaColorizerFormat.class,

      // issues
      IssueTrackingDecorator.class,
      IssueTracking.class,
      IssueHandlers.class,
      CountUnresolvedIssuesDecorator.class,
      CountFalsePositivesDecorator.class,
      WeightedIssuesDecorator.class,
      IssuesDensityDecorator.class,
      InitialOpenIssuesSensor.class,
      InitialOpenIssuesStack.class,
      HotspotMostViolatedResourcesWidget.class,
      HotspotMostViolatedRulesWidget.class,
      MyUnresolvedIssuesWidget.class,
      FalsePositiveIssuesWidget.class,
      ActionPlansWidget.class,
      UnresolvedIssuesPerAssigneeWidget.class,
      UnresolvedIssuesStatusesWidget.class,
      IssueFilterWidget.class,
      org.sonar.api.issue.NoSonarFilter.class,

      // issue notifications
      SendIssueNotificationsPostJob.class,
      NewIssuesEmailTemplate.class,
      IssueChangesEmailTemplate.class,
      ChangesOnMyIssueNotificationDispatcher.class,
      ChangesOnMyIssueNotificationDispatcher.newMetadata(),
      NewIssuesNotificationDispatcher.class,
      NewIssuesNotificationDispatcher.newMetadata(),
      NewFalsePositiveNotificationDispatcher.class,
      NewFalsePositiveNotificationDispatcher.newMetadata(),

      // technical debt
      TechnicalDebtConverter.class,
      TechnicalDebtDecorator.class,
      NewTechnicalDebtDecorator.class,

      // batch
      ProfileSensor.class,
      ProfileEventsSensor.class,
      ProjectLinksSensor.class,
      UnitTestDecorator.class,
      VersionEventsSensor.class,
      CheckAlertThresholds.class,
      GenerateAlertEvents.class,
      LineCoverageDecorator.class,
      CoverageDecorator.class,
      BranchCoverageDecorator.class,
      ItLineCoverageDecorator.class,
      ItCoverageDecorator.class,
      ItBranchCoverageDecorator.class,
      OverallLineCoverageDecorator.class,
      OverallCoverageDecorator.class,
      OverallBranchCoverageDecorator.class,
      CoverageMeasurementFilter.class,
      ApplyProjectRolesDecorator.class,
      CommentDensityDecorator.class,
      NoSonarFilter.class,
      DirectoriesDecorator.class,
      FilesDecorator.class,
      IndexProjectPostJob.class,
      ManualMeasureDecorator.class,
      FileHashSensor.class,

      // time machine
      TendencyDecorator.class,
      VariationDecorator.class,
      TimeMachineConfigurationPersister.class,
      NewCoverageFileAnalyzer.class,
      NewItCoverageFileAnalyzer.class,
      NewOverallCoverageFileAnalyzer.class,
      NewCoverageAggregator.class,

      // Notify alerts on my favourite projects
      NewAlerts.class,
      NewAlerts.newMetadata());

    extensions.addAll(ExclusionProperties.definitions());
    extensions.addAll(IgnoreIssuesPlugin.getExtensions());
    extensions.addAll(CoverageMeasurementFilter.getPropertyDefinitions());
    extensions.addAll(PastSnapshotFinder.getPropertyDefinitions());
    extensions.addAll(TechnicalDebtDecorator.definitions());
    extensions.addAll(propertyDefinitions());

    return extensions.build();
  }

  static List<PropertyDefinition> propertyDefinitions() {
    return Arrays.asList(
      PropertyDefinition.builder(CoreProperties.CORE_VIOLATION_LOCALE_PROPERTY)
        .defaultValue("en")
        .name("Locale used for issue messages")
        .description("Deprecated property. Keep default value for backward compatibility.")
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_PROPERTY)
        .defaultValue("" + CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_DEAULT_VALUE)
        .name("Allow users to sign up online")
        .description("Users can sign up online.")
        .type(PropertyType.BOOLEAN)
        .category(CoreProperties.CATEGORY_SECURITY)
        .build(),

      PropertyDefinition.builder(CoreProperties.CORE_DEFAULT_GROUP)
        .defaultValue(CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE)
        .name("Default user group")
        .description("Any new users will automatically join this group.")
        .category(CoreProperties.CATEGORY_SECURITY)
        .build(),

      PropertyDefinition.builder(CoreProperties.CORE_IMPORT_SOURCES_PROPERTY)
        .defaultValue("" + CoreProperties.CORE_IMPORT_SOURCES_DEFAULT_VALUE)
        .name("Import sources")
        .description("Set to false if sources should not be imported and therefore not available in the Web UI (e.g. for security reasons).")
        .type(PropertyType.BOOLEAN)
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .category(CoreProperties.CATEGORY_SECURITY)
        .build(),

      PropertyDefinition.builder(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY)
        .defaultValue("" + CoreProperties.CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE)
        .name("Force user authentication")
        .description("Forcing user authentication stops un-logged users to access SonarQube.")
        .type(PropertyType.BOOLEAN)
        .category(CoreProperties.CATEGORY_SECURITY)
        .build(),

      PropertyDefinition.builder(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION)
        .defaultValue(Boolean.toString(false))
        .name("Prevent automatic project creation")
        .description("Set to true to prevent automatic project creation at first analysis and force project provisioning.")
        .type(PropertyType.BOOLEAN)
        .category(CoreProperties.CATEGORY_SECURITY)
        .build()
      );
  }

}
