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
package org.sonar.server.rule;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.MatchQueryBuilder.Operator;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.sonar.api.ServerExtension;
import org.sonar.server.qualityprofile.Paging;
import org.sonar.server.qualityprofile.PagingResult;
import org.sonar.server.qualityprofile.QProfileRule;
import org.sonar.server.qualityprofile.QProfileRuleResult;
import org.sonar.server.search.SearchIndex;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.sonar.server.rule.RuleRegistry.INDEX_RULES;
import static org.sonar.server.rule.RuleRegistry.TYPE_ACTIVE_RULE;
import static org.sonar.server.rule.RuleRegistry.TYPE_RULE;

public class ProfileRules implements ServerExtension {

  private static final String FIELD_PARENT = "_parent";
  private static final String FIELD_SOURCE = "_source";

  private final SearchIndex index;

  public ProfileRules(SearchIndex index) {
    this.index = index;
  }

  public QProfileRuleResult searchActiveRules(ProfileRuleQuery query, Paging paging) {
    BoolFilterBuilder filter = activeRuleFilter(query);

    SearchRequestBuilder builder = index.client().prepareSearch(INDEX_RULES).setTypes(TYPE_ACTIVE_RULE)
      .setFilter(filter)
      .addFields(FIELD_SOURCE, FIELD_PARENT)
      .setSize(paging.pageSize())
      .setFrom(paging.offset());
    SearchHits hits = index.executeRequest(builder);

    List<Map<String, Object>> activeRuleSources = Lists.newArrayList();
    String[] parentIds = new String[hits.getHits().length];
    int hitCounter = 0;

    List<QProfileRule> result = Lists.newArrayList();
    for (SearchHit hit: hits.getHits()) {
      activeRuleSources.add(hit.sourceAsMap());
      parentIds[hitCounter] = hit.field(FIELD_PARENT).value();
      hitCounter ++;
    }

    if (hitCounter > 0) {
      MultiGetItemResponse[] responses = index.client().prepareMultiGet().add(INDEX_RULES, TYPE_RULE, parentIds)
        .execute().actionGet().getResponses();

      for (int i = 0; i < hitCounter; i ++) {
        result.add(new QProfileRule(responses[i].getResponse().getSourceAsMap(), activeRuleSources.get(i)));
      }
    }

    return new QProfileRuleResult(result, PagingResult.create(paging.pageSize(), paging.pageIndex(), hits.getTotalHits()));
  }

  protected BoolFilterBuilder activeRuleFilter(ProfileRuleQuery query) {
    BoolFilterBuilder filter = boolFilter().must(
            termFilter(ActiveRuleDocument.FIELD_PROFILE_ID, query.profileId()),
            hasParentFilter(TYPE_RULE, parentRuleFilter(query))
        );
    addMustTermOrTerms(filter, ActiveRuleDocument.FIELD_SEVERITY, query.severities());
    return filter;
  }

  public long countActiveRules(ProfileRuleQuery query) {
    return index.executeCount(index.client().prepareCount(INDEX_RULES).setTypes(TYPE_ACTIVE_RULE)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), activeRuleFilter(query))));
  }

  public long countInactiveRules(ProfileRuleQuery query) {
    return index.executeCount(index.client().prepareCount(INDEX_RULES).setTypes(TYPE_RULE)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
        boolFilter()
          .must(parentRuleFilter(query))
          .mustNot(hasChildFilter(TYPE_ACTIVE_RULE, termFilter(ActiveRuleDocument.FIELD_PROFILE_ID, query.profileId()))))));
  }

  private FilterBuilder parentRuleFilter(ProfileRuleQuery query) {
    if (! query.hasParentRuleCriteria()) {
      return FilterBuilders.matchAllFilter();
    }

    BoolFilterBuilder result = boolFilter();

    addMustTermOrTerms(result, RuleDocument.FIELD_REPOSITORY_KEY, query.repositoryKeys());
    addMustTermOrTerms(result, RuleDocument.FIELD_STATUS, query.statuses());

    if (StringUtils.isNotBlank(query.nameOrKey())) {
      result.must(
        queryFilter(
          multiMatchQuery(query.nameOrKey(), RuleDocument.FIELD_NAME, RuleDocument.FIELD_KEY)
            .operator(Operator.AND)));
    }

    return result;
  }

  private void addMustTermOrTerms(BoolFilterBuilder filter, String field, Collection<String> terms) {
    FilterBuilder termOrTerms = getTermOrTerms(field, terms);
    if (termOrTerms != null) {
      filter.must(termOrTerms);
    }
  }

  private FilterBuilder getTermOrTerms(String field, Collection<String> terms) {
    if (terms.isEmpty()) {
      return null;
    } else {
      if (terms.size() == 1) {
        return termFilter(field, terms.iterator().next());
      } else {
        return termsFilter(field, terms.toArray());
      }
    }
  }
}
