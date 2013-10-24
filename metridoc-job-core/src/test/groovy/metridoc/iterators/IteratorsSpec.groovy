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



package metridoc.iterators

import com.google.common.collect.Table
import com.google.common.collect.TreeBasedTable
import groovy.sql.Sql
import groovy.stream.Stream
import metridoc.utils.DataSourceConfigUtil
import metridoc.writers.DefaultIteratorWriter
import metridoc.writers.IteratorWriter
import metridoc.writers.WriteResponse
import metridoc.writers.WrittenRecordStat
import org.apache.commons.lang.ObjectUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.sql.ResultSet

/**
 * Created with IntelliJ IDEA on 7/5/13
 * @author Tommy Barker
 */
class IteratorsSpec extends Specification {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()
    File foo

    def setup() {

        foo = folder.newFile("foo")
        foo.withPrintWriter {
            it.println("a|b")
            it.println("b|c")
            return it
        }
    }

    class TestWriter extends DefaultIteratorWriter{
        @Override
        WriteResponse write(RecordIterator recordIterator) {
            assert recordIterator != null: "rowIterator cannot be null"
            recordIterator.recordHeaders.table = TreeBasedTable.create()
            def response = super.write(recordIterator)
            response.body.table = response.headers.table

            return response
        }

        @Override
        boolean doWrite(int lineNumber, Record record) {
            def headers = record.headers
            def table = headers.table
            if(!record.body.containsValue("a")){
                record.body.each { columnKey, value ->
                    table.put(lineNumber, columnKey, value ?: ObjectUtils.NULL)
                }

                return true
            }
            else return false
        }
    }

    void "iterators are created via lookup"() {
        when: "a delimited iterator is built"
        def iterator = Iterators.createIterator("delimited", delimiter: /\|/, file: foo)
        def next1 = iterator.next()
        def next2 = iterator.next()

        then: "we can iterate over the file contents"

        "a" == next1.body[0]
        "b" == next1.body[1]

        "b" == next2.body[0]
        "c" == next2.body[1]

        try {
            iterator.next()
            assert false: "exception should have occurred"
        }
        catch (NoSuchElementException ignored) {
            //do nothing
        }
    }

    void "iterators should also be able to write"() {
        when: "and is written to a guava table"
        def table = Iterators.fromDelimited(foo, /\|/).filter {!it.body.containsValue("a")}.toGuavaTable() as Table
        def set = table.columnKeySet()
        then:
        set.contains(0)
        set.contains(1)
        !table.containsValue("a")
        table.containsValue("b")
    }

    void "writeResponse's TOTAL field should include all responses"(){
        when: "writing to a generic table"
        def iterator = Iterators.createIterator("delimited", delimiter: /\|/, file: foo)
        def writer = Iterators.createWriter([:], TestWriter)
        def response = writer.write(iterator)

        then:
        !response.body.table.containsValue("a")
        response.body.table.containsValue("b")
        response.aggregateStats[WrittenRecordStat.Status.IGNORED]==1
        response.aggregateStats[WrittenRecordStat.Status.WRITTEN]==1
        response.aggregateStats[WrittenRecordStat.Status.TOTAL]==2

    }

    void "test sql iterator extension"() {
        given:
        def dataSource = DataSourceConfigUtil.embeddedDataSource
        def sql = new Sql(dataSource)
        try {
            //just in case dataSource was not closed from another test
            sql.execute("drop table foo")
        }
        catch (Throwable ignore) {

        }
        sql.execute("create table foo (bar varchar(5))")

        when: "fromSql is called with no arguments"
        Iterators.fromSql(null)

        then:
        thrown(AssertionError)

        when: "everything is fine when there is a dataSource"
        sql.query("select * from foo") { ResultSet resultSet ->
            Iterators.fromSql(resultSet)
        }

        then:
        noExceptionThrown()

        cleanup:
        try {
            dataSource.close()
        }
        catch (Throwable ignore) {

        }
    }
}
