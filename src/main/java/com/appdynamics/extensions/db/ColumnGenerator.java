/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.db;

import com.appdynamics.extensions.util.AssertUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

 
public class ColumnGenerator {

    public List<Column> getColumns(Map query) {
        AssertUtils.assertNotNull(query.get("columns"),"Queries need to have columns configured.");

        // Map<String, Map<String, String>> filter = Maps.newLinkedHashMap();
        Map<String, List<String>> filter = Maps.newLinkedHashMap();
        filter = filterMap(query, "columns");
        final ObjectMapper mapper = new ObjectMapper(); // jackson’s objectmapper
        final Columns columns = mapper.convertValue(filter, Columns.class);
        return columns.getColumns();
    }

    private Map<String, List<String>> filterMap( Map<String, List<String>> mapOfMaps, String filterKey) {
        Map<String, List<String>> filteredOnKeyMap = Maps.newLinkedHashMap();

        if (Strings.isNullOrEmpty(filterKey))
            return filteredOnKeyMap;

        if (mapOfMaps.containsKey(filterKey)) {
            filteredOnKeyMap.put(filterKey,mapOfMaps.get(filterKey));
        }

        return filteredOnKeyMap;
    }
}
