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
package org.sonar.server.configuration;

import org.sonar.api.profiles.RulesProfile;

import org.junit.Before;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.collection.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

public class ProfilesBackupTest extends AbstractDbUnitTestCase {

  private SonarConfig sonarConfig;

  @Before
  public void setup() {
    sonarConfig = new SonarConfig();
  }

  @Test
  public void shouldExportClonedProfiles() {

    RulesProfile profileProvided = new RulesProfile();
    profileProvided.setProvided(false);
    profileProvided.setLanguage("test");
    profileProvided.setName("provided");

    ProfilesBackup profilesBackup = new ProfilesBackup(Arrays.asList(profileProvided));
    profilesBackup.exportXml(sonarConfig);

    assertThat(sonarConfig.getProfiles().iterator().next()).isNotSameAs(profileProvided);
    assertThat(sonarConfig.getProfiles().iterator().next().getName()).isEqualTo("provided");
  }

  @Test
  public void shouldExportAllProfiles() {

    RulesProfile profileProvided = new RulesProfile();
    profileProvided.setProvided(true);
    profileProvided.setLanguage("test");
    profileProvided.setName("provided");

    RulesProfile profileNotProvided = new RulesProfile();
    profileNotProvided.setProvided(false);
    profileNotProvided.setLanguage("test");
    profileNotProvided.setName("not provided");

    ProfilesBackup profilesBackup = new ProfilesBackup(Arrays.asList(profileProvided, profileNotProvided));
    profilesBackup.exportXml(sonarConfig);

    assertThat(sonarConfig.getProfiles(), hasItem(profileNotProvided));
    assertThat(sonarConfig.getProfiles(), hasItem(profileProvided));
  }

  @Test
  public void shouldImportProfiles() {

  }
}
