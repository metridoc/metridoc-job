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



package metridoc.core.tools

import groovy.sql.Sql

/**
 * A lotof this stuff is already handled but the config tool, really don't need this anymore
 *
 * @author Tommy Barker
 * @deprecated
 */
class SqlTool {
    String dataSourceKey = "dataSource"
    Binding binding

    void init() {
        def config = binding.config
        if (config && config instanceof ConfigObject) {
            config.putAll(binding.variables)
        }

        def dataSourceConfig = config."$dataSourceKey"
        assert dataSourceConfig != null: "dataSource config does not exist"
        def userName = dataSourceConfig.username
        def password = dataSourceConfig.password
        def url = dataSourceConfig.url
        def driver = dataSourceConfig.driverClassName

        assert userName: "user name cannot be null"
        assert password: "password cannot be null"
        assert url: "url cannot be null"
        assert driver: "driver cannot be null"
        binding.sql = Sql.newInstance(url, userName, password, driver)
    }
}
