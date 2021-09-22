/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.db;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.json.JSONObject;
import org.slf4j.Logger;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DBtoCustomSchemaTask implements AMonitorTaskRunnable {

    private long previousTimestamp;
    private long currentTimestamp;
    private String metricPrefix;
    private MetricWriteHelper metricWriter;
    private JDBCConnectionAdapter jdbcAdapter;
    private Map server;
    private Boolean status = true;
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(DBtoCustomSchemaTask.class);
    // private final Map<String, String> analyticsEventPublishApi;

    public void run() {
        List<Map> queries = (List<Map>) server.get("queries");
        Connection connection = null;
        if (queries != null && !queries.isEmpty()) {
            String dbServerDisplayName = (String) server.get("displayName");
            try {
                long timeBeforeConnection = System.currentTimeMillis();
                connection = getConnection();
                long timeAfterConnection = System.currentTimeMillis();
                logger.debug("Time taken to get Connection: " + (timeAfterConnection - timeBeforeConnection));

                logger.debug("Time taken to get Connection for " + dbServerDisplayName + " : " + (timeAfterConnection - timeBeforeConnection));

                if (connection != null) {
                    logger.debug(" Connection successful for server: " + dbServerDisplayName);
                    for (Map query : queries)
                        executeQuery(connection, query);
                } else {

                    logger.debug("Null Connection returned for server: " + dbServerDisplayName);
                }

            } catch (SQLException e) {
                logger.error("Error Opening connection", e);
                status = false;
            } catch (ClassNotFoundException e) {
                logger.error("Class not found while opening connection", e);
                status = false;
            } catch (Exception e) {
                logger.error("Error collecting metrics for "+dbServerDisplayName, e);
                status = false;
            }
            finally {
                try {
                    if (connection != null) {
                        closeConnection(connection);
                    }
                } catch (Exception e) {
                    logger.error("Issue closing the connection", e);
                }
            }
        }
    }

    private void executeQuery(Connection connection, Map query) {
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = getStatement(connection);
            resultSet = getResultSet(query, statement);

            getMetricsFromResultSet(query, resultSet);
            String[] columns = statement.toString().split(",");

            // while (resultSet.next()) {
            //     StringBuffer data = new StringBuffer();
            //     for (int i = 1; i <= columns.length; i++) {
            //         System.out.println("columns count"+ columns.length);
            //         System.out.println("\"" + columns[i - 1] + "\"" + ":" + "\"" + resultSet.getString(i) + "\"" + ",");
            //         data.append("\"" + columns[i - 1] + "\"" + ":" + "\"" + resultSet.getString(i) + "\"" + ",");
            //     }
            //     publishData(data.toString().substring(0, (data.length() - 1)),"https://fra-ana-api.saas.appdynamics.com/events/publish/dccih_commissioning_transactions", "centricapreprod_65f907d1-1478-4a78-b8e1-0b91a3ae2707", "2522bdd9-e5ec-43b2-88c8-00644f3a1d4c");
            // }
            

        } catch (SQLException e) {
            logger.error("Error in connecting the result. ", e);
        } finally {

            if (statement != null) try {
                jdbcAdapter.closeStatement(statement);
            } catch (SQLException e) {
                logger.error("Unable to close the Statement", e);
            }

            if (resultSet != null) try {
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Unable to close the ResultSet", e);
            }
        }
    }

    static long rowInserted = 0; 
    static void publishData(String data, String url, String controllerGlobalAccountName, String eventsAPIKey) {
        Unirest.setTimeouts(0, 0);
        try {
            HttpResponse < String > response = Unirest.post(url).header("X-Events-API-AccountName", controllerGlobalAccountName)
                .header("X-Events-API-Key", eventsAPIKey).header("Content-type", "application/vnd.appd.events+json;v=2")
                .body("[" + data + "]").asString();
            System.out.print("Response " + response.getStatus() + "\t");
            System.out.print("Total rows inserted " + ++rowInserted + "\n");
        } catch (UnirestException e) {
            System.out.println(e);
        }
    }
    // private Map<String, String> getAnalyticsEventPublishApi(Map<String, ?> server) {
    //     Map<String, String> connectionProperties = (Map<String, String>) server.get("analyticsEventPublishApi");
    //     String url = connectionProperties.get("url");
    //     if (Strings.isNullOrEmpty(password))
    //         password = getPassword(connectionProperties);

    //     connectionProperties.put("password", password);
    //     return connectionProperties;
    // }
    private void getMetricsFromResultSet(Map query, ResultSet resultSet) throws SQLException {
        String dbServerDisplayName = (String) server.get("displayName");
        String queryDisplayName = (String) query.get("displayName");
        logger.debug("Received ResultSet and now extracting metrics for query {}", queryDisplayName);
      
        ColumnGenerator columnGenerator = new ColumnGenerator();
        List<Column> columns = columnGenerator.getColumns(query);
        JSONObject json = null;
        // analyticsEventPublishApi 

        Map<String, String> analyticsEventPublishApi = (Map<String, String>) server.get("analyticsEventPublishApi");
        String url = analyticsEventPublishApi.get("url");
        String controllerGlobalAccountName = analyticsEventPublishApi.get("controllerGlobalAccountName");
        String eventsAPIKey = analyticsEventPublishApi.get("eventsAPIKey");
        
        System.out.println("-----------------------" ) ;
        System.out.println("url" +url) ;
        System.out.println("controllerGlobalAccountName" + controllerGlobalAccountName) ;
        System.out.println("eventsAPIKey" +eventsAPIKey) ;
        System.out.println("-----------------------" ) ;
        while (resultSet.next()) {
            StringBuffer data = new StringBuffer();
            int columnIndex = 0;
            json = new JSONObject();

            for(Column column : columns) {
                columnIndex++;
                
                json.put(column.getAnalyticsColName(),  resultSet.getString(columnIndex));
                // System.out.println("column name " + column.getName() + "column analytic name " + column.getAnalyticsColName() + "column value " + resultSet.getString(columnIndex));
                // columnIndex++;
            }
            // for (int i = 1; i <= columns.length; i++) {
            //     System.out.println("column name" + columns[i].getName + "column name" + columns[i].getAnalyticsColName);
            //     data.append("\"" + columns[i - 1] + "\"" + ":" + "\"" + resultSet.getString(i) + "\"" + ",");
            // }
            System.out.println("-----------------------" ) ;
            System.out.println("data" + json.toString()) ;
            System.out.println("-----------------------" ) ;
            publishData(json.toString(),url , controllerGlobalAccountName, eventsAPIKey);
        }



        // ---------------------------------------------------
        // List<Map<String, String>> metricReplacer = getMetricReplacer();

        // MetricCollector metricCollector = new MetricCollector(metricPrefix, dbServerDisplayName, queryDisplayName, metricReplacer);

        // Map<String, Metric> metricMap = metricCollector.goingThroughResultSet(resultSet, columns);
        // List<Metric> metricList = getListMetrics(metricMap);
        // metricWriter.transformAndPrintMetrics(metricList);
        // ---------------------------------------------------

    }


    private List<Metric> getListMetrics(Map<String, Metric> metricMap) {
        List<Metric> metricList = new ArrayList<Metric>();
        for (String path : metricMap.keySet()) {
            metricList.add(metricMap.get(path));
        }
        return metricList;

    }


    private Statement getStatement(Connection connection) throws SQLException {
        return connection.createStatement();
    }

    private ResultSet getResultSet(Map query, Statement statement) throws SQLException {
        String queryStmt = (String) query.get("queryStmt");
        queryStmt = substitute(queryStmt);
        long timeBeforeQuery = System.currentTimeMillis();
        ResultSet resultSet = jdbcAdapter.queryDatabase(queryStmt, statement);
        long timeAfterQuery = System.currentTimeMillis();

        logger.debug("Queried the database in :" + (timeAfterQuery - timeBeforeQuery) + " ms for query: \n " + queryStmt);

        return resultSet;
    }


    private List<Map<String, String>> getMetricReplacer() {
        List<Map<String, String>> metricReplace = (List<Map<String, String>>) server.get("metricCharacterReplacer");
        return metricReplace;
    }

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Connection connection = jdbcAdapter.open((String) server.get("driver"));
        return connection;
    }

    private void closeConnection(Connection connection) throws Exception {
        jdbcAdapter.closeConnection(connection);
    }

    private String substitute(String statement) {
        String stmt = statement;
        stmt = stmt.replace("{{previousTimestamp}}", Long.toString(previousTimestamp));
        stmt = stmt.replace("{{currentTimestamp}}", Long.toString(currentTimestamp));
        return stmt;
    }

    public void onTaskComplete() {
        logger.debug("Task Complete");
        if (status == true) {
            metricWriter.printMetric(metricPrefix + "|" + server.get("displayName") + "|" + "HeartBeat", "1", "AVERAGE", "AVERAGE", "INDIVIDUAL");
        } else {
            metricWriter.printMetric(metricPrefix + "|" + server.get("displayName") + "|" + "HeartBeat", "0", "AVERAGE", "AVERAGE", "INDIVIDUAL");
        }
    }

    public static class Builder {
        private DBtoCustomSchemaTask task = new DBtoCustomSchemaTask();

        Builder metricPrefix(String metricPrefix) {
            task.metricPrefix = metricPrefix;
            return this;
        }

        Builder metricWriter(MetricWriteHelper metricWriter) {
            task.metricWriter = metricWriter;
            return this;
        }

        Builder server(Map server) {
            task.server = server;
            return this;
        }

        Builder jdbcAdapter(JDBCConnectionAdapter adapter) {
            task.jdbcAdapter = adapter;
            return this;
        }

        Builder previousTimestamp(long timestamp) {
            task.previousTimestamp = timestamp;
            return this;
        }

        Builder currentTimestamp(long timestamp) {
            task.currentTimestamp = timestamp;
            return this;
        }

        DBtoCustomSchemaTask build() {
            return task;
        }
    }
}