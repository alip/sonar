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

package org.sonar.server.qualityprofile;

import com.google.common.base.Strings;
import org.sonar.api.ServerComponent;
import org.sonar.api.component.Component;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.ProfileRuleQuery;
import org.sonar.server.rule.ProfileRules;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

import javax.annotation.CheckForNull;

import java.util.List;
import java.util.Map;

public class QProfiles implements ServerComponent {

  private final QualityProfileDao qualityProfileDao;
  private final ActiveRuleDao activeRuleDao;
  private final ResourceDao resourceDao;

  private final QProfileProjectService projectService;

  private final QProfileSearch search;
  private final QProfileOperations operations;
  private final ProfileRules rules;

  public QProfiles(QualityProfileDao qualityProfileDao, ActiveRuleDao activeRuleDao, ResourceDao resourceDao, QProfileProjectService projectService, QProfileSearch search,
                   QProfileOperations operations, ProfileRules rules) {
    this.qualityProfileDao = qualityProfileDao;
    this.activeRuleDao = activeRuleDao;
    this.resourceDao = resourceDao;
    this.projectService = projectService;
    this.search = search;
    this.operations = operations;
    this.rules = rules;
  }

  public List<QProfile> searchProfiles() {
    return search.searchProfiles();
  }

  public void searchProfile(Integer profileId) {
    throw new UnsupportedOperationException();
  }

  public NewProfileResult newProfile(String name, String language, Map<String, String> xmlProfilesByPlugin) {
    validateNewProfile(name, language);
    return operations.newProfile(name, language, xmlProfilesByPlugin, UserSession.get());
  }

  public void deleteProfile() {
    // Delete alerts, activeRules, activeRuleParams, activeRuleNotes, Projects
    throw new UnsupportedOperationException();
  }

  public void renameProfile(Integer profileId, String newName) {
    QualityProfileDto qualityProfile = validateRenameProfile(profileId, newName);
    operations.renameProfile(qualityProfile, newName, UserSession.get());
  }

  public void setDefaultProfile(Integer profileId) {
    QualityProfileDto qualityProfile = findNotNull(profileId);
    operations.setDefaultProfile(qualityProfile, UserSession.get());
  }

  /**
   * Used by WS
   */
  public void setDefaultProfile(String name, String language) {
    QualityProfileDto qualityProfile = findNotNull(name, language);
    operations.setDefaultProfile(qualityProfile, UserSession.get());
  }

  public void copyProfile() {
    throw new UnsupportedOperationException();
  }

  public void exportProfile(Integer profileId) {
    throw new UnsupportedOperationException();
  }

  public void exportProfile(Integer profileId, String plugin) {
    throw new UnsupportedOperationException();
  }

  public void restoreProfile() {
    throw new UnsupportedOperationException();
  }

  // INHERITANCE

  public void inheritance() {
    throw new UnsupportedOperationException();
  }

  public void inherit(Integer profileId, Integer parentProfileId) {
    throw new UnsupportedOperationException();
  }

  // CHANGELOG

  public void changelog(Integer profileId) {
    throw new UnsupportedOperationException();
  }

  // PROJECTS

  public QProfileProjects projects(Integer profileId) {
    Validation.checkMandatoryParameter(profileId, "profile");
    QualityProfileDto qualityProfile = findNotNull(profileId);
    return projectService.projects(qualityProfile);
  }

  /**
   * Used in /project/profile
   */
  public QProfile profile(Integer projectId) {
    throw new UnsupportedOperationException();
  }

  public void addProject(Integer profileId, Long projectId) {
    Validation.checkMandatoryParameter(profileId, "profile");
    Validation.checkMandatoryParameter(projectId, "project");
    ComponentDto project = (ComponentDto) findNotNull(projectId);
    QualityProfileDto qualityProfile = findNotNull(profileId);

    projectService.addProject(qualityProfile, project, UserSession.get());
  }

  public void removeProject(Integer profileId, Long projectId) {
    Validation.checkMandatoryParameter(profileId, "profile");
    QualityProfileDto qualityProfile = findNotNull(profileId);
    ComponentDto project = (ComponentDto) findNotNull(projectId);

    projectService.removeProject(qualityProfile, project, UserSession.get());
  }

  public void removeProjectByLanguage(String language, Long projectId) {
    Validation.checkMandatoryParameter(language, "language");
    ComponentDto project = (ComponentDto) findNotNull(projectId);

    projectService.removeProject(language, project, UserSession.get());
  }

  public void removeAllProjects(Integer profileId) {
    throw new UnsupportedOperationException();
  }

  // ACTIVE RULES

  public QProfileRuleResult searchActiveRules(ProfileRuleQuery query, Paging paging) {
    return rules.searchActiveRules(query, paging);
  }

  public long countActiveRules(ProfileRuleQuery query) {
    return rules.countActiveRules(query);
  }

  public void searchInactiveRules(ProfileRuleQuery query, Paging paging) {
    throw new UnsupportedOperationException();
  }

  public long countInactiveRules(ProfileRuleQuery query) {
    return rules.countInactiveRules(query);
  }

  public void activeRule(Integer profileId, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void deactiveRule(Integer profileId, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void updateParameters(Integer profileId, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void activeNote(Integer profileId, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void editNote(Integer profileId, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void deleteNote(Integer profileId, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void extendDescription(Integer profileId, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  // TEMPLATE RULES

  public void createTemplateRule() {
    throw new UnsupportedOperationException();
  }

  public void editTemplateRule() {
    throw new UnsupportedOperationException();
  }

  public void deleteTemplateRule() {
    throw new UnsupportedOperationException();
  }

  private void validateNewProfile(String name, String language) {
    validateName(name);
    Validation.checkMandatoryParameter(language, "language");
    checkNotAlreadyExists(name, language);
  }

  private QualityProfileDto validateRenameProfile(Integer profileId, String newName) {
    validateName(newName);
    QualityProfileDto profileDto = findNotNull(profileId);
    if (!profileDto.getName().equals(newName)) {
      checkNotAlreadyExists(newName, profileDto.getLanguage());
    }
    return profileDto;
  }

  private void checkNotAlreadyExists(String name, String language) {
    if (find(name, language) != null) {
      throw BadRequestException.ofL10n("quality_profiles.already_exists");
    }
  }

  private QualityProfileDto findNotNull(Integer id) {
    QualityProfileDto qualityProfile = find(id);
    return checkNotNull(qualityProfile);
  }

  private QualityProfileDto findNotNull(String name, String language) {
    QualityProfileDto qualityProfile = find(name, language);
    return checkNotNull(qualityProfile);
  }

  private Component findNotNull(Long projectId) {
    Component component = resourceDao.findById(projectId);
    if (component == null) {
      throw new NotFoundException("This project does not exists.");
    }
    return component;
  }

  private QualityProfileDto checkNotNull(QualityProfileDto qualityProfile) {
    if (qualityProfile == null) {
      throw new NotFoundException("This quality profile does not exists.");
    }
    return qualityProfile;
  }

  @CheckForNull
  private QualityProfileDto find(String name, String language) {
    return qualityProfileDao.selectByNameAndLanguage(name, language);
  }

  @CheckForNull
  private QualityProfileDto find(Integer id) {
    return qualityProfileDao.selectById(id);
  }

  private void validateName(String name) {
    if (Strings.isNullOrEmpty(name)) {
      throw BadRequestException.ofL10n("quality_profiles.please_type_profile_name");
    }
  }

}
