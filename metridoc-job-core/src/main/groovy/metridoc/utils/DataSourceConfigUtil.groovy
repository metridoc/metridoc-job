/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */



package metridoc.utils

import org.apache.commons.dbcp.BasicDataSource

import javax.sql.DataSource

/**
 * @author Tommy Barker
 */
class DataSourceConfigUtil {

    public static final String DEFAULT_DATASOURCE = "dataSource"

    static Properties getHibernatePoperties(ConfigObject config, String dataSourceName) {
        String dataSourceNameUsed = dataSourceName ?: DEFAULT_DATASOURCE
        def result = [:]
        result."hibernate.current_session_context_class" = "thread"
        result."hibernate.hbm2ddl.auto" = "update"
        result."hibernate.cache.provider_class" = "org.hibernate.cache.NoCacheProvider"
        result.putAll(config.flatten().findAll { String key, value -> key.startsWith("hibernate") })
        config."${dataSourceNameUsed}".with {
            if (dbCreate) {
                result."hibernate.hbm2ddl.auto" = dbCreate.toString()
            }

            if (logSql) {
                result."hibernate.show_sql" = logSql.toString()
            }

            if (formatSql) {
                result."hibernate.format_sql" = formatSql.toString()
            }

            if (dialect) {
                if (dialect instanceof Class) {
                    result."hibernate.dialect" = dialect.name
                }
                else {
                    result."hibernate.dialect" = dialect.toString()
                }
            }
        }

        return result as Properties
    }

    static Properties getHibernatePoperties(ConfigObject config) {
        getHibernatePoperties(config, DEFAULT_DATASOURCE)
    }

    static Map getHibernateOnlyProperties(ConfigObject config, String dataSourceName) {
        def dataSourceNameUsed = dataSourceName ?: DEFAULT_DATASOURCE
        def properties = getHibernatePoperties(config, dataSourceNameUsed)
        config."${dataSourceNameUsed}".with {
            if (url) {
                properties."hibernate.connection.url" = url.toString()
            }
            if (username) {
                properties."hibernate.connection.username" = username.toString()
            }
            if (password) {
                properties."hibernate.connection.password" = password.toString()
            }
            if (driverClassName) {
                if (driverClassName instanceof Class) {
                    properties."hibernate.connection.driver_class" = driverClassName.name
                }
                else {
                    properties."hibernate.connection.driver_class" = driverClassName.toString()
                }
            }
        }

        return properties
    }

    static Map getHibernateOnlyProperties(ConfigObject config) {
        getHibernateOnlyProperties(config, DEFAULT_DATASOURCE)
    }

    static DataSource getDataSource(ConfigObject config, String dataSourceName) {
        def dataSourceNameUsed = dataSourceName ?: DEFAULT_DATASOURCE
        def dataSourceProperties = getDataSourceProperties(config, dataSourceNameUsed)
        def dataSource = new BasicDataSource(dataSourceProperties)

        addShutdownHook {
            try {
                dataSource.close()
            }
            catch (Exception e) {
                //do nothing
            }
        }

        return dataSource
    }

    static List getDataSourcesNames(ConfigObject configObject) {
        configObject.keySet().findAll { it.startsWith("dataSource") } as List
    }

    static DataSource getDataSource(ConfigObject config) {
        getDataSource(config, DEFAULT_DATASOURCE)
    }

    static Map getDataSourceProperties(ConfigObject config, String dataSourceName) {
        def dataSourceNameUsed = dataSourceName ?: DEFAULT_DATASOURCE
        def result = [:]
        def dataSourceConfig = config."${dataSourceNameUsed}"
        result.username = dataSourceConfig.username
        result.password = dataSourceConfig.password
        result.url = dataSourceConfig.url
        result.driverClassName = dataSourceConfig.driverClassName

        def dataSourceProperties = config."${dataSourceNameUsed}".properties
        if (dataSourceProperties) {
            result.putAll(dataSourceProperties)
        }
        return result
    }

    static getEmbeddedDataSource() {
        def config = new ConfigObject()
        def dataSourceConfig = config.dataSource

        dataSourceConfig.username = "sa"
        dataSourceConfig.password = ""
        dataSourceConfig.url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000"
        dataSourceConfig.driverClassName = "org.h2.Driver"

        return getDataSource(config)
    }

    static getLocalMysqlDataSource() {
        def config = new ConfigObject()
        def dataSourceConfig = config.dataSource

        dataSourceConfig.url = "jdbc:mysql://localhost:3306/test"
        dataSourceConfig.username = "root"
        dataSourceConfig.password = ""
        dataSourceConfig.driverClassName = "com.mysql.jdbc.Driver"

        return getDataSource(config)
    }

    static Map getDataSourceProperties(ConfigObject config) {
        getDataSourceProperties(config, DEFAULT_DATASOURCE)
    }

    static void addEmbeddedDataSource(ConfigObject configObject) {
        addEmbeddedDataSource(configObject, "devDb")
    }

    static void addEmbeddedDataSource(ConfigObject configObject, String name) {
        addEmbeddedDataSource(configObject, name, "dataSource")
    }

    static void addEmbeddedDataSource(ConfigObject configObject, String name, String dataSourceName) {
        def dataSourceNameToUse = dataSourceName.startsWith("dataSource") ? dataSourceName : "dataSource_${dataSourceName}"

        configObject."$dataSourceNameToUse".username = "sa"
        configObject."$dataSourceNameToUse".password = ""
        configObject."$dataSourceNameToUse".url = "jdbc:h2:mem:${name};MVCC=TRUE;LOCK_TIMEOUT=10000"
        configObject."$dataSourceNameToUse".driverClassName = "org.h2.Driver"
    }
}
