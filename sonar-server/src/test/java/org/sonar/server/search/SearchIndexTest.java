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

package org.sonar.server.search;

import com.github.tlrx.elasticsearch.test.EsSetup;
import org.elasticsearch.common.io.BytesStream;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.mapper.StrictDynamicMappingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchIndexTest {

  private EsSetup esSetup;
  private SearchNode searchNode;

  private SearchIndex searchIndex;

  @Before
  public void setUp() {
    esSetup = new EsSetup();
    esSetup.execute(EsSetup.deleteAll());

    searchNode = mock(SearchNode.class);
    when(searchNode.client()).thenReturn(esSetup.client());

    Settings settings = new Settings();
    settings.setProperty("sonar.log.profilingLevel", "BASIC");
    searchIndex = new SearchIndex(searchNode, new Profiling(settings));
    searchIndex.start();
  }

  @After
  public void tearDown() {
    esSetup.terminate();
  }

  @Test
  public void should_start_and_stop_properly() {
    verify(searchNode).client();
    searchIndex.stop();
  }

  @Test
  public void should_create_index_when_loading_mapping_from_classpath() {
    String index = "index";
    String type = "type";
    String resourcePath = "/org/sonar/server/search/SearchIndexTest/correct_mapping1.json";

    searchIndex.start();
    searchIndex.addMappingFromClasspath(index, type, resourcePath);

    assertThat(esSetup.exists(index)).isTrue();
  }

  @Test
  public void should_reuse_index_when_loading_mapping_from_classpath() {
    String index = "index";
    String type1 = "type1";
    String type2 = "type2";
    String resourcePath1 = "/org/sonar/server/search/SearchIndexTest/correct_mapping1.json";
    String resourcePath2 = "/org/sonar/server/search/SearchIndexTest/correct_mapping2.json";

    searchIndex.start();
    searchIndex.addMappingFromClasspath(index, type1, resourcePath1);
    searchIndex.addMappingFromClasspath(index, type2, resourcePath2);

    assertThat(esSetup.exists(index)).isTrue();
  }


  @Test(expected = IllegalArgumentException.class)
  public void should_fail_to_load_inexistent_mapping() {
    String resourcePath = "/org/sonar/server/search/SearchIndexTest/inexistent.json";

    searchIndex.start();
    searchIndex.addMappingFromClasspath("unchecked", "unchecked", resourcePath);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_to_load_malformed_mapping() {
    String resourcePath = "/org/sonar/server/search/SearchIndexTest/malformed.json";

    searchIndex.start();
    searchIndex.addMappingFromClasspath("unchecked", "unchecked", resourcePath);
  }

  @Test
  public void should_iterate_over_big_dataset() throws Exception {
    final int numberOfDocuments = 10000;

    searchIndex.addMappingFromClasspath("index", "type1", "/org/sonar/server/search/SearchIndexTest/correct_mapping1.json");
    String[] ids = new String[numberOfDocuments];
    BytesStream[] sources = new BytesStream[numberOfDocuments];
    for (int i=0; i<numberOfDocuments; i++) {
      ids[i] = Integer.toString(i);
      sources[i] = XContentFactory.jsonBuilder().startObject().field("value", Integer.toString(i)).endObject();
    }
    searchIndex.bulkIndex("index", "type1", ids, sources);

    List<String> docIds = searchIndex.findDocumentIds(SearchQuery.create());
    assertThat(docIds).hasSize(numberOfDocuments);
  }

  @Test(expected = StrictDynamicMappingException.class)
  public void should_forbid_dynamic_mapping() throws Exception {
    searchIndex.addMappingFromClasspath("index", "type1", "/org/sonar/server/search/SearchIndexTest/correct_mapping1.json");
    searchIndex.putSynchronous("index", "type1", "666",
      XContentFactory.jsonBuilder().startObject().field("unknown", "plouf").endObject()
    );
  }
}
