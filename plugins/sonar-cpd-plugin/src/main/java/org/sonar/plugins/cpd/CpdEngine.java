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
package org.sonar.plugins.cpd;

import org.slf4j.Logger;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;

public abstract class CpdEngine implements BatchExtension {

  abstract boolean isLanguageSupported(String language);

  abstract void analyse(Project project, SensorContext context);

  protected void logExclusions(String[] exclusions, Logger logger) {
    if (exclusions.length > 0) {
      StringBuilder message = new StringBuilder("Copy-paste detection exclusions:");
      for (String exclusion : exclusions) {
        message.append("\n  ");
        message.append(exclusion);
      }

      logger.info(message.toString());
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
