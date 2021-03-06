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

package org.sonar.server.source.ws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.WsTester;
import org.sonar.server.source.SourceService;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SourcesShowWsHandlerTest {

  @Mock
  SourceService sourceService;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    tester = new WsTester(new SourcesWs(new SourcesShowWsHandler(sourceService)));
  }

  @Test
  public void show_source() throws Exception {
    String componentKey = "org.apache.struts:struts:Dispatcher";
    when(sourceService.sourcesFromComponent(componentKey)).thenReturn(newArrayList(
      "/*",
      " * Header",
      " */",
      "",
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {",
      "}"
    ));

    WsTester.TestRequest request = tester.newRequest("show").setParam("key", componentKey);
    request.execute().assertJson(getClass(), "show_source.json");
  }
}
