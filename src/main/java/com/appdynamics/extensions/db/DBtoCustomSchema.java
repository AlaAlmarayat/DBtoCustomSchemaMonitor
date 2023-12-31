/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.db;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.extensions.util.CryptoUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.unix4j.variable.Arg;

import static com.appdynamics.extensions.db.Constant.METRIC_PREFIX;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DBtoCustomSchema extends ABaseMonitor {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(DBtoCustomSchema.class);
    private long previousTimestamp = 0;
    private long currentTimestamp = System.currentTimeMillis();

    @Override
    protected String getDefaultMetricPrefix() {
        return METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return "DB to Custom Schema Monitor";
    }
    public static void main(String[] args) throws IOException   {

        


        // logger.getRootLogger().addAppender(ca);

        final DBtoCustomSchema monitor = new DBtoCustomSchema();

        final Map<String, String> taskArgs = new HashMap<String, String>();
        String config = Paths.get("config.yml")
        .toAbsolutePath()
        .toString(); 
        String metrics = Paths.get("metrics.xml")
        .toAbsolutePath()
        .toString(); 
        taskArgs.put("config-file", config);
        taskArgs.put("metric-file", metrics);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    monitor.execute(taskArgs, null);
                } catch (Exception e) {
                    logger.error("Error while running the task", e);
                }
            }
        }, 2, 60, TimeUnit.SECONDS);
    }

  
    @Override
    protected void doRun(TasksExecutionServiceProvider serviceProvider) {

        List<Map<String, ?>> servers = (List<Map<String, ?>>) getContextConfiguration().getConfigYml().get("dbServers");

        previousTimestamp = currentTimestamp;
        currentTimestamp = System.currentTimeMillis();
        if (previousTimestamp != 0) {
            for (Map<String, ?> server : servers) {
                try {
                    DBtoCustomSchemaTask task = createTask(server, serviceProvider);
                    serviceProvider.submit((String) server.get("displayName"), task);
                } catch (Exception e) {
                    logger.error("Error while creating task for {}", Util.convertToString(server.get("displayName"), ""),e);
                }
            }
        }
    }

    @Override
    protected List<Map<String, ?>> getServers() {
        return (List<Map<String, ?>>) getContextConfiguration().getConfigYml().get("dbServers");
    }

    private DBtoCustomSchemaTask createTask(Map<String, ?> server, TasksExecutionServiceProvider serviceProvider) {
        String connUrl = createConnectionUrl(server);

        //Fix for ACE-1001
        if(Strings.isNullOrEmpty(serverName(server))){
            throw new IllegalArgumentException("The 'displayName' field under the 'dbServers' section in config.yml is not initialised");
        }
        if(Strings.isNullOrEmpty(createConnectionUrl(server))){
            throw new IllegalArgumentException("The 'connectionUrl' field under the 'dbServers' section in config.yml is not initialised");
        }
        if(Strings.isNullOrEmpty(driverName(server))){
            throw new IllegalArgumentException("The 'driver' field under the 'dbServers' section in config.yml is not initialised");
        }

        //AssertUtils.assertNotNull(serverName(server), "The 'displayName' field under the 'dbServers' section in config.yml is not initialised");
        //AssertUtils.assertNotNull(createConnectionUrl(server), "The 'connectionUrl' field under the 'dbServers' section in config.yml is not initialised");
        //AssertUtils.assertNotNull(driverName(server), "The 'driver' field under the 'dbServers' section in config.yml is not initialised");

        Map<String, String> connectionProperties = getConnectionProperties(server);
        JDBCConnectionAdapter jdbcAdapter = JDBCConnectionAdapter.create(connUrl, connectionProperties);

        logger.debug("Task Created for "+server.get("displayName"));

        return new DBtoCustomSchemaTask.Builder()
                .metricWriter(serviceProvider.getMetricWriteHelper())
                .metricPrefix(getContextConfiguration().getMetricPrefix())
                .jdbcAdapter(jdbcAdapter)
                .previousTimestamp(previousTimestamp)
                .currentTimestamp(currentTimestamp)
                .server(server).build();

    }

    private String serverName(Map<String, ?> server) {
        String name = Util.convertToString(server.get("displayName"), "");
        return name;
    }

    private String driverName(Map<String, ?> server) {
        String name = Util.convertToString(server.get("driver"), "");
        return name;
    }

    private String createConnectionUrl(Map<String, ?> server) {
        String url = Util.convertToString(server.get("connectionUrl"), "");
        return url;
    }

    private Map<String, String> getConnectionProperties(Map<String, ?> server) {
        Map<String, String> connectionProperties = (Map<String, String>) server.get("connectionProperties");
        String password = connectionProperties.get("password");
        if (Strings.isNullOrEmpty(password))
            password = getPassword(connectionProperties);

        connectionProperties.put("password", password);
        return connectionProperties;
    }


    private String getPassword(Map<String, String> server) {
        String encryptedPassword =  server.get(Constant.ENCRYPTED_PASSWORD);
        Map<String, ?> configMap = getContextConfiguration().getConfigYml();
        String encryptionKey = (String) configMap.get(Constant.ENCRYPTION_KEY);
        if (!Strings.isNullOrEmpty(encryptedPassword)) {
            Map<String, String> cryptoMap = Maps.newHashMap();
            cryptoMap.put("encryptedPassword", encryptedPassword);
            cryptoMap.put("encryptionKey", encryptionKey);
            logger.debug("Decrypting the encrypted password........");
            return CryptoUtils.getPassword(cryptoMap);
        }
        return "";
    }

//    public static void main(String[] args) throws TaskExecutionException {
//
//        SQLMonitor monitor = new SQLMonitor();
//
//        final Map<String, String> taskArgs = new HashMap<String, String>();
//
//        taskArgs.put("config-file", "src/main/resources/conf/config.yml");
//        monitor.execute(taskArgs, null);
//    }

}