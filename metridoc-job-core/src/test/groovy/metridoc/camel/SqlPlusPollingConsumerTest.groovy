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



package metridoc.camel

import groovy.sql.Sql
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.impl.SimpleRegistry
import org.apache.camel.test.junit4.CamelTestSupport
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase

import java.sql.ResultSet

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 9/7/12
 * Time: 2:01 PM
 * To change this template use File | Settings | File Templates.
 */
class SqlPlusPollingConsumerTest extends CamelTestSupport {

    def simpleRegistry = new SimpleRegistry()
    EmbeddedDatabase embeddedDataSource

    @Override
    protected CamelContext createCamelContext() {
        CamelContext context = new DefaultCamelContext(simpleRegistry)
        context.setLazyLoadTypeConverters(isLazyLoadingTypeConverter())
        return context;
    }

    @Before
    void addEmbeddedDataSource() {
        embeddedDataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build()
        simpleRegistry.put("dataSource", embeddedDataSource)
        def sql = new Sql(embeddedDataSource)
        sql.execute("create table foo(name varchar(50), age int)")
        sql.execute("insert into foo values ('joe', 50)")
        sql.execute("insert into foo values ('jack', 70)")
    }

    @After
    void shutdownEmbeddedDatabase() {
        embeddedDataSource.shutdown()
    }

    @Test
    void "test resultset handling"() {
        Exchange exchange = consumer.receive("sqlplus:foo?dataSource=dataSource")
        ResultSet resultSet = exchange.in.body
        assert !resultSet.isClosed()
    }
}
