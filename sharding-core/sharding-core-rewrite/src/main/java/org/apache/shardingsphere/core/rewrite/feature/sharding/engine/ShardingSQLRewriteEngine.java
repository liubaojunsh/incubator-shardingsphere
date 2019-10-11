/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.rewrite.feature.sharding.engine;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.rewrite.context.SQLRewriteContext;
import org.apache.shardingsphere.core.rewrite.engine.SQLRewriteEngine;
import org.apache.shardingsphere.core.rewrite.engine.SQLRewriteResult;
import org.apache.shardingsphere.core.rewrite.parameter.builder.ParameterBuilder;
import org.apache.shardingsphere.core.rewrite.parameter.builder.impl.GroupedParameterBuilder;
import org.apache.shardingsphere.core.rewrite.parameter.builder.impl.StandardParameterBuilder;
import org.apache.shardingsphere.core.route.router.sharding.condition.ShardingCondition;
import org.apache.shardingsphere.core.route.router.sharding.condition.ShardingConditions;
import org.apache.shardingsphere.core.route.type.RoutingUnit;
import org.apache.shardingsphere.core.rule.DataNode;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * SQL rewrite engine for sharding.
 * 
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class ShardingSQLRewriteEngine implements SQLRewriteEngine {
    
    private final ShardingConditions shardingConditions;
    
    private final RoutingUnit routingUnit;
    
    private final Map<String, String> logicAndActualTables;
    
    @Override
    public SQLRewriteResult rewrite(final SQLRewriteContext sqlRewriteContext) {
        return new SQLRewriteResult(sqlRewriteContext.getSQLBuilder().toSQL(routingUnit, logicAndActualTables), getParameters(sqlRewriteContext.getParameterBuilder()));
    }
    
    private List<Object> getParameters(final ParameterBuilder parameterBuilder) {
        if (parameterBuilder instanceof StandardParameterBuilder || shardingConditions.getConditions().isEmpty()) {
            return parameterBuilder.getParameters();
        }
        if (parameterBuilder.getParameters().isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> result = new LinkedList<>();
        int count = 0;
        for (ShardingCondition each : shardingConditions.getConditions()) {
            if (isInSameDataNode(each)) {
                result.addAll(((GroupedParameterBuilder) parameterBuilder).getParameters(count));
            }
            count++;
        }
        return result;
    }

    private boolean isInSameDataNode(final ShardingCondition shardingCondition) {
        if (shardingCondition.getDataNodes().isEmpty()) {
            return true;
        }
        for (DataNode each : shardingCondition.getDataNodes()) {
            if (routingUnit.getTableUnit(each.getDataSourceName(), each.getTableName()).isPresent()) {
                return true;
            }
        }
        return false;
    }
}