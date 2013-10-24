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

import spock.lang.Specification

/**
 * @author Tommy Barker
 */
class DataSourceConfigUtilSpec extends Specification {
    private ConfigObject config = new ConfigObject()

    void "hibernate properties spec"() {
        setup:
        config.hibernate.jdbc.batch_size = 5
        config.dataSource.dbCreate = "create-drop"
        config.dataSource.logSql = true
        config.dataSource.formatSql = true
        config.dataSource.dialect = "foo.bar"
        def value
        def hibernateProperties = DataSourceConfigUtil.getHibernatePoperties(config)

        when:
        value = hibernateProperties["hibernate.jdbc.batch_size"]

        then:
        noExceptionThrown()
        5 == value

        when:
        value = hibernateProperties["hibernate.hbm2ddl.auto"]

        then:
        noExceptionThrown()
        "create-drop" == value

        when:
        value = hibernateProperties["hibernate.show_sql"]

        then:
        noExceptionThrown()
        "true" == value

        when:
        value = hibernateProperties["hibernate.format_sql"]

        then:
        noExceptionThrown()
        "true" == value

        when:
        value = hibernateProperties["hibernate.dialect"]

        then:
        noExceptionThrown()
        "foo.bar" == value

        when:
        value = hibernateProperties["hibernate.current_session_context_class"]

        then:
        noExceptionThrown()
        "thread" == value

        when:
        value = hibernateProperties["hibernate.cache.provider_class"]

        then:
        noExceptionThrown()
        "org.hibernate.cache.NoCacheProvider" == value
    }

    void "hibernate only will convert the connection properties to hibernate properties"() {
        setup:
        config.dataSource.username = "foo"
        config.dataSource.password = "bar"
        config.dataSource.url = "foo://foo.com"
        def value

        when:
        value = DataSourceConfigUtil.getHibernateOnlyProperties(config)["hibernate.connection.username"]

        then:
        "foo" == value

        when:
        value = DataSourceConfigUtil.getHibernateOnlyProperties(config)["hibernate.connection.password"]

        then:
        "bar" == value

        when:
        value = DataSourceConfigUtil.getHibernateOnlyProperties(config)["hibernate.connection.url"]

        then:
        "foo://foo.com" == value
    }

    void "DataSource spec"() {
        setup:
        def dataSource
        def properties = [
                username: "sa",
                password: "",
                url: "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000",
                driverClassName: "org.hibernate.dialect.H2Dialect",
                properties: [
                        maxActive: 50
                ]
        ]
        def config = new ConfigObject()
        config.dataSource = properties

        when:
        dataSource = DataSourceConfigUtil.getDataSource(config)
        dataSource.getConnection()

        then:
        noExceptionThrown()
        50 == dataSource.maxActive

        when: "using a different dataSource name"
        config.dataSource_alt = properties
        config.dataSource_alt.properties.maxActive = 5
        dataSource = DataSourceConfigUtil.getDataSource(config, "dataSource_alt")
        dataSource.getConnection()

        then:
        noExceptionThrown()
        5 == dataSource.maxActive
    }

    void "test adding a dataSource to a config"() {
        given:
        def config = new ConfigObject()

        when:
        DataSourceConfigUtil.addEmbeddedDataSource(config)

        then:
        "" == config.dataSource.password
        "sa" == config.dataSource.username
        "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000" == config.dataSource.url
        "org.h2.Driver" == config.dataSource.driverClassName

        when:
        DataSourceConfigUtil.addEmbeddedDataSource(config, "foo")

        then: "the name of the database in the url is foo"
        "" == config.dataSource.password
        "sa" == config.dataSource.username
        "jdbc:h2:mem:foo;MVCC=TRUE;LOCK_TIMEOUT=10000" == config.dataSource.url
        "org.h2.Driver" == config.dataSource.driverClassName

        when:
        DataSourceConfigUtil.addEmbeddedDataSource(config, "foo", "bar")

        then: "the name of the database in the url is foo"
        "" == config.dataSource_bar.password
        "sa" == config.dataSource_bar.username
        "jdbc:h2:mem:foo;MVCC=TRUE;LOCK_TIMEOUT=10000" == config.dataSource_bar.url
        "org.h2.Driver" == config.dataSource_bar.driverClassName
    }

    void "should be able to get all the dataSource names"() {
        given:
        def config = new ConfigObject()
        config.dataSource = [:]
        config.dataSource_foo = [:]

        when:
        def result = DataSourceConfigUtil.getDataSourcesNames(config)

        then:
        ["dataSource", "dataSource_foo"] == result
    }
}
