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

package org.sonar.server.source;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.web.UserRole;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.source.HtmlSourceDecorator;
import org.sonar.server.user.MockUserSession;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SourceServiceTest {

  @Mock
  HtmlSourceDecorator sourceDecorator;

  @Mock
  ResourceDao resourceDao;

  SourceService service;

  @Before
  public void setUp() throws Exception {
    service = new SourceService(sourceDecorator, resourceDao);
  }

  @Test
  public void sources_from_component() throws Exception {
    String projectKey = "org.sonar.sample";
    String componentKey = "org.sonar.sample:Sample";
    MockUserSession.set().addProjectPermissions(UserRole.CODEVIEWER, projectKey);
    when(resourceDao.getRootProjectByComponentKey(componentKey)).thenReturn(new ResourceDto().setKey(projectKey));

    service.sourcesFromComponent(componentKey);

    verify(sourceDecorator).getDecoratedSourceAsHtml(componentKey);
  }
}
