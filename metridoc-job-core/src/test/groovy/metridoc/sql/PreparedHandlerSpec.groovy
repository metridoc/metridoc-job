package metridoc.sql

import spock.lang.Specification

import java.sql.PreparedStatement

/**
 * Created by tbarker on 12/6/13.
 */
class PreparedHandlerSpec extends Specification {

    def record = [
            foo: "bar",
            bar: "blam"
    ]

    def columnsWithDefaults = [
            "foo", "foobar", "baz"
    ] as Set

    def ignoredColumnsWithDefaults = [
            "foobar", "baz"
    ] as Set

    void "test creating the ignored default values"() {
        when:
        def result = PreparedHandler.getIgnoredColumnsWithDefaults(record, columnsWithDefaults)

        then:
        ignoredColumnsWithDefaults == result
    }


    void "test creating a prepared statement"() {
        given:
        def handler = new PreparedHandler()
        def statement = [:] as PreparedStatement
        handler.preparedStatements[ignoredColumnsWithDefaults] = statement

        when:
        def result = handler.getPreparedStatement(
                new InsertableRecord(insertMetaData: new InsertMetaData(columnsWithDefaults: columnsWithDefaults), originalRecord: record)
        )

        then:
        statement == result
    }

}
