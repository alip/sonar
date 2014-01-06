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
package org.sonar.batch;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;

public class ProfileProvider extends ProviderAdapter {

  private RulesProfile profile;

  /**
   * @param languages This parameter is here to ensure Languages is started before this provider in order to display available languages.
   */
  public RulesProfile provide(Project project, ProfileLoader profileLoader, Settings settings, Languages languages) {
    if (profile == null) {
      profile = profileLoader.load(project, settings);
    }
    return profile;
  }
}
