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

import com.mysql.jdbc.Driver
import org.hibernate.dialect.MySQL5InnoDBDialect
import org.junit.Test

/**
 * @author Tommy Barker
 *
 */
class HibernateServiceTest {

    @Test
    void "set Binding should not fail if config property does not exist"() {
        def tool = new HibernateService()
        tool.binding = new Binding()
    }

    @Test
    void "test converting to hibernate properties from dataSource properties"() {
        def configObject = new ConfigObject()
        configObject.dataSource.dialect = "org.hibernate.dialect.MySQL5InnoDBDialect"
        configObject.dataSource.driverClassName = "com.mysql.jdbc.Driver"
        configObject.dataSource.dbCreate = "create-drop"

        def properties = new HibernateService().convertDataSourcePropsToHibernate(configObject)
        assert "org.hibernate.dialect.MySQL5InnoDBDialect" == properties.get("hibernate.dialect")
        assert "com.mysql.jdbc.Driver" == properties.get("hibernate.connection.driver_class")
        assert "create-drop" == properties.get("hibernate.hbm2ddl.auto")

        //what if we use classes instead of strings?
        configObject.dataSource.dialect = MySQL5InnoDBDialect
        configObject.dataSource.driverClassName = Driver
        configObject.dataSource.dbCreate = "create-drop"

        properties = new HibernateService().convertDataSourcePropsToHibernate(configObject)
        assert "org.hibernate.dialect.MySQL5InnoDBDialect" == properties.get("hibernate.dialect")
        assert "com.mysql.jdbc.Driver" == properties.get("hibernate.connection.driver_class")
        assert "create-drop" == properties.get("hibernate.hbm2ddl.auto")
    }

    @Test
    void "test adding straight hibernate properties"() {
        def configObject = new ConfigObject()
        configObject.hibernate.hbm2ddl.auto = "create-drop"

        def properties = new HibernateService().convertDataSourcePropsToHibernate(configObject)
        assert "create-drop" == properties.get("hibernate.hbm2ddl.auto")
    }

    @Test
    void "test configuring a basic embedded database"() {
        def service = new HibernateService(embeddedDataSource: true)
        service.init()
        assert "org.hibernate.dialect.H2Dialect" == service.hibernateProperties."hibernate.dialect"
        assert "org.h2.Driver" == service.hibernateProperties."hibernate.connection.driver_class"
    }

    @Test
    void "test injection params"() {
        def binding = new Binding()
        binding.args = [
                "--mergeMetridocConfig=false",
                "--localMysql"
        ] as String[]

        binding.includeService(ConfigService)
        def service = binding.includeService(HibernateService)
        assert service.localMysql
        def config = binding.config
        assert "jdbc:mysql://localhost:3306/test" == config.dataSource.url
    }
}

