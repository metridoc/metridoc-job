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
import metridoc.core.services.CamelService
import org.apache.camel.Exchange
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType

import javax.sql.DataSource
import java.sql.ResultSet

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 4/1/13
 * Time: 10:00 AM
 * To change this template use File | Settings | File Templates.
 */
class SqlPlusRouteTest {

    DataSource embeddedDataSource

    @Before
    void addEmbeddedDataSource() {
        embeddedDataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build()
        def sql = new Sql(embeddedDataSource)
        sql.execute("create table FOO(name varchar(50), age int)")
        sql.execute("create table BAR(name varchar(50), age int)")
        sql.execute("insert into FOO values ('joe', 50)")
        sql.execute("insert into FOO values ('jack', 70)")
    }

    @After
    void shutdownEmbeddedDatabase() {
        embeddedDataSource.shutdown()
    }

    @Test
    void "test sql routing using the camel tool"() {
        def service = new Binding().includeService(CamelService)
        service.bind("dataSource", embeddedDataSource)

        service.with {
            consumeNoWait("sqletl:FOO?dataSource=dataSource") { ResultSet resultSet ->
                send("sqletl:BAR?dataSource=dataSource&logBatches=true", resultSet)
            }
        }

        def sql = new Sql(embeddedDataSource)
        assert 2 == sql.firstRow("select count(*) as total from BAR").total
    }
}
