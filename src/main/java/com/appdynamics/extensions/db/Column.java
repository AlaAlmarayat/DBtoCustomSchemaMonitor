/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.db;

import com.singularity.ee.agent.systemagent.api.MetricWriter;


import java.util.Map;
import java.util.Properties;


public class Column {
    private String name;
    private String type; 
    private String analyticsColName;
    private String aggregationType = MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE;
    private String timeRollupType  = MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE;
    private String clusterRollupType = MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL;
    private Map properties;

    //
    public Map convert;
    //
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAnalyticsColName() {
        return analyticsColName;
    }

    public void setAnalyticsColName(String analyticsColName) {
        this.analyticsColName = analyticsColName;
    }

    public Map getProperties() {
        if(properties == null){
            properties = new Properties();
        }
        return properties;
    }

    public void setProperties(Map properties) {
        this.properties = properties;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }

    public String getTimeRollupType() {
        return timeRollupType;
    }

    public void setTimeRollupType(String timeRollupType) {
        this.timeRollupType = timeRollupType;
    }

    public String getClusterRollupType() {
        return clusterRollupType;
    }

    public void setClusterRollupType(String clusterRollupType) {
        this.clusterRollupType = clusterRollupType;
    }

    public Map getConvertMap(){
        return convert;
    }

    public void setConvertMap(Map convert){
        this.convert = convert;
    }
}
