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



package metridoc.writers

import groovy.sql.Sql
import metridoc.iterators.Iterators
import metridoc.utils.DataSourceConfigUtil
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.BatchUpdateException
import java.sql.SQLException

import static metridoc.writers.WrittenRecordStat.Status.WRITTEN

class DataSourceIteratorWriterSpec extends Specification {

    def dataSource = DataSourceConfigUtil.embeddedDataSource
    def sql = new Sql(dataSource)
    def iterator = Iterators.fromMaps(
            [foo: "bar", bar: 5],
            [foo: "baz", bar: 7]
    )

    def setup() {
        try {
            //in case we have a lingering connection and an existing foo table
            sql.execute("drop table foo")
        }
        catch (Throwable ignore) {
        }
        sql.execute("create table foo(foo varchar(3), bar int)")
    }

    def cleanup() {
        dataSource.close()
    }

    def "test basic dataSource writing"() {
        when: "data is written to the foo table"
        def results = iterator.toDataSource(dataSource, "FOO")
        def exception = results.fatalErrors ? results.fatalErrors[0] : null
        if(exception) {
            throw exception
        }

        then: "foo will have data from the iterator stored"
        noExceptionThrown()
        2 == sql.firstRow("select count(*) as total from foo").total
        2 == results.aggregateStats[WRITTEN]
        int[] batchResults = results as int[]
        2 == batchResults.size()
        1 == batchResults[0]
        1 == batchResults[1]
    }

    def "dataSource and tableName must be set"() {
        when: "a dataSource writer is created without a dataSource"
        iterator.toDataSource(null, "FOO")

        then: "an AssertionError is thrown"
        AssertionError error = thrown()
        error.message.startsWith(DataSourceIteratorWriter.DATASOURCE_MESSAGE)

        when: "a dataSource writer writes against null"
        new DataSourceIteratorWriter(dataSource: [:] as DataSource).write(null)

        then: "an AssertionError is thrown"
        error = thrown()
        error.message.startsWith(DataSourceIteratorWriter.TABLE_NAME_ERROR)


    }

    def "test errors when insert fails"() {
        given:

        def iterator1 = Iterators.fromMaps(
                [foo: "bar", bar: 5],
                [foo: "baz", bar: 7],
                [foo: "foobar", bar: 7]
        )

        def iterator2 = Iterators.fromMaps(
                [foo: "bar", bar: 5],
                [foo: "baz", bar: 7],
                [foo: "foobar", bar: 7]
        )

        when: "a record in a batch is too long"
        def response = iterator1.toDataSource(dataSource, "FOO")
        def throwables = response.fatalErrors

        then: "a batch error occurs"
        1 == response.total
        1 == throwables.size()
        throwables[0] instanceof BatchUpdateException

        when: "a the table name does not exist"
        iterator2.toDataSource(dataSource, "fooBar")
        throwables = response.fatalErrors

        then: "an response exception occurs"
        1 == response.total
        1 == throwables.size()
        throwables[0] instanceof SQLException
    }
}
