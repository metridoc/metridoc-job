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



package metridoc.sql

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import spock.lang.Specification
import java.sql.SQLException

class SqlPlusSpec extends Specification {

    def dataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build()

    def cleanup() {
        dataSource.shutdown()
    }

    def "test corner cases"() {
        setup:
        def sql = new SqlPlus(dataSource)
        sql.execute("create table FOO(NAME varchar(50), AGE int)")
        sql.execute("create table BAR(NAME varchar(50), AGE int)")
        sql.execute("insert into FOO values ('joe', 50)")
        sql.execute("insert into FOO values ('jack', 70)")

        when: "running a batch insert with an empty list"
        sql.runBatch("FOO", [])

        then: "then there will be no error"
        notThrown Throwable

        when: "running a batch insert with an empty hash"
        sql.runBatch("FOO", [:])

        then: "then there will be no error"
        notThrown Throwable
    }

    def "test regular batch run"(){
        setup:
        def sql = new SqlPlus(dataSource)
        sql.execute("create table FOO(NAME varchar(50), AGE int)")
        sql.execute("insert into FOO values ('joe', 50)")
        sql.execute("insert into FOO values ('jack', 70)")
        def batch = [['NAME':'Jane', 'AGE':30],['NAME':'Joan', 'AGE':20],['NAME':'Jean', 'AGE':40]]
        def badBatch = [['NAME':'Igor', 'AGE':30],['NAME':'Frank', 'AGE':20, 'HEIGHT':7],['NAME':'Vlad', 'AGE':400]]

        when: "running a successful batch of inserts"
        sql.runBatch("FOO", batch)

        then: "then there will be no error"
        notThrown Throwable
        assert 5 == sql.firstRow("select count(NAME) as total from FOO").total

        when: "running a batch of inserts with a bad entry"
        sql.runBatch("FOO", badBatch)

        then: "there will be an error"
        thrown SQLException
    }

    void "test bestEffortOrdering"() {
        given:
        def record = [
                bar: 1,
                FOO: 2,
                foobar: 3
        ]
        def params = ["BAR", "foo", "foobar"] as SortedSet

        when:
        def recordKeys = SqlPlus.bestEffortOrdering(record, params).keySet() as SortedSet

        then:
        recordKeys == params
    }
}
