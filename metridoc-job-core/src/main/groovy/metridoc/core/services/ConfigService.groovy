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



package metridoc.core.services

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import metridoc.utils.DataSourceConfigUtil
import org.apache.commons.lang.text.StrBuilder

import java.sql.SQLException

import static org.apache.commons.lang.SystemUtils.FILE_SEPARATOR
import static org.apache.commons.lang.SystemUtils.USER_HOME

/**
 * @author Tommy Barker
 */
@Slf4j
class ConfigService extends DefaultService {
    private static final String METRIDOC_CONFIG = "${USER_HOME}${FILE_SEPARATOR}.metridoc${FILE_SEPARATOR}MetridocConfig.groovy"
    def metridocConfigLocation = METRIDOC_CONFIG
    boolean mergeMetridocConfig = true

    void setBinding(Binding binding) {
        super.setBinding(binding)
        if (!binding.hasVariable("config")) {
            binding.includeService(ParseArgsService)
            if (binding.hasVariable("argsMap")) {
                setDataFromFlags(binding.argsMap)
            }

            String env = getVariable("env", String)
            if ("prod" == env) {
                env = "production"
            }

            if ("dev" == env) {
                env = "development"
            }

            def configSlurper = env ? new ConfigSlurper(env) : new ConfigSlurper()
            File cliConfigLocation = getVariable("config", File)
            def cliConfigObject = new ConfigObject()
            if (cliConfigLocation) {
                cliConfigObject = configureFromFile(cliConfigLocation, configSlurper)
            }

            def metridocConfigFile = new File(metridocConfigLocation)
            if (metridocConfigFile.exists() && mergeMetridocConfig) {
                def metridocConfigObject = configureFromFile(metridocConfigFile, configSlurper)
                metridocConfigObject.merge(cliConfigObject)
                cliConfigObject = metridocConfigObject
            }

            addCliConfigArgs(configSlurper, cliConfigObject)

            binding.config = cliConfigObject
            initiateDataSources(cliConfigObject)
        }
    }

    protected void setDataFromFlags(Map argsMap) {
        //if directly set to false (probably in testing) we skip this
        if (mergeMetridocConfig) {
            mergeMetridocConfig = argsMap.containsKey("mergeMetridocConfig") ?
                Boolean.valueOf(argsMap.mergeMetridocConfig as String) : true
        }

        //if directly set (probably in testing), let's ignore all other settings
        if (metridocConfigLocation == METRIDOC_CONFIG) {
            metridocConfigLocation = argsMap.metridocConfigLocation ?: METRIDOC_CONFIG
        }
    }

    void addCliConfigArgs(ConfigSlurper slurper, ConfigObject configObject) {
        if (binding.hasVariable("args")) {
            String[] args = binding.args
            StrBuilder builder = new StrBuilder()

            args.each {
                if (it.startsWith("-config.") || it.startsWith("--config.")) {
                    def propertyToWrite = it.replaceFirst(/-?-config\./, "")
                    builder.appendln(propertyToWrite)
                }
            }

            String configScript = builder.toString().trim()
            if (configScript) {
                configObject.merge(slurper.parse(configScript))
            }
        }
    }

    private static ConfigObject configureFromFile(File file, ConfigSlurper slurper) {
        if (file.exists()) {
            try {
                return slurper.parse(file.toURI().toURL())
            }
            catch (Throwable throwable) {
                throw new IOException("Could not parse the configuration in [$file]", throwable)
            }
        }

        return null
    }

    ConfigService addConfig(Closure closure) {
        ConfigObject config = binding.variables.config ?: new ConfigObject()
        binding.config = config
        def slurper = new ConfigSlurper()
        config.merge(slurper.parse(new ConfigScript(configClosure: closure)))

        return this
    }

    ConfigService addConfig(File file) {
        ConfigObject config = binding.variables.config ?: new ConfigObject()
        binding.config = config
        def slurper = new ConfigSlurper()
        config.merge(slurper.parse(file.toURI().toURL()))

        return this
    }

    void initiateDataSources(ConfigObject configObject) {
        def localMysql = null
        def embeddedDataSource = null
        if (binding.hasVariable("args")) {
            localMysql = binding.args.find { it.contains("-localMysql") || it.contains("-localMySql") }
            if (localMysql) {
                def dataSource = DataSourceConfigUtil.localMysqlDataSource
                binding.dataSource = dataSource
                binding.sql = new Sql(dataSource)
            }

            embeddedDataSource = binding.args.find { it.contains("-embeddedDataSource") }
            if (embeddedDataSource) {
                def dataSource = DataSourceConfigUtil.embeddedDataSource
                binding.dataSource = dataSource
                binding.sql = new Sql(dataSource)
            }
        }

        DataSourceConfigUtil.getDataSourcesNames(configObject).each { String dataSourceName ->
            if (configObject[dataSourceName] instanceof ConfigObject) {
                def dataSource = DataSourceConfigUtil.getDataSource(configObject, dataSourceName)
                //just calling to test a conneciton
                try {
                    dataSource.getConnection()
                }
                catch (SQLException ex) {
                    try {
                        if(!configObject[dataSourceName].driverClassName) {
                            throw new SQLException("[driverClassName] has not been set for data source [$dataSourceName]")
                        }
                        if(!configObject[dataSourceName].url) {
                            throw new SQLException("[url] has not been set for data source [$dataSourceName]")
                        }
                        if(!configObject[dataSourceName].username) {
                            throw new SQLException("[username] has not been set for data source [$dataSourceName]")
                        }
                        if(!configObject[dataSourceName].password) {
                            throw new SQLException("[password] has not been set for data source [$dataSourceName]")
                        }

                        //don't know the cause, just throw it
                        throw ex
                    }
                    catch (SQLException formatted) {
                        String url = configObject[dataSourceName].url
                        String message = "Could not connect to [${url ?: dataSourceName}] during configuration, if " +
                                "used in job it will likely fail: $formatted.message"
                        log.warn message
                    }
                }
                def m = dataSourceName =~ /dataSource_(\w+)$/
                def sqlName = "sql"
                if (m.matches()) {
                    sqlName += "_${m.group(1)}"
                }

                if (sqlName == "sql" && (embeddedDataSource || localMysql)) return

                binding."$dataSourceName" = dataSource
                binding."$sqlName" = new Sql(dataSource)
            }
        }
    }
}

class ConfigScript extends Script {
    Closure configClosure

    @Override
    def run() {
        Closure clone = configClosure.clone() as Closure
        clone.delegate = this
        clone.run()
    }
}