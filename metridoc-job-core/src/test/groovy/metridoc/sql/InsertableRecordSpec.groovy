package metridoc.sql

import spock.lang.Specification

/**
 * Created by tbarker on 12/6/13.
 */
class InsertableRecordSpec extends Specification {

    void "test transforming the record"() {
        given:
        def metaData = new InsertMetaData(
                sortedParams: [
                        "foo_bar", "BAR", "baz"
                ] as SortedSet<String> ,
                columnsWithDefaults: []
        )

        def insertableRecordParams = [
                originalRecord: [
                        FOO_bar: "bar",
                        bar: "foo"
                ],
                insertMetaData: metaData,
        ]

        when:
        def transformed = createInsertableRecord(insertableRecordParams).transformedRecord

        then:
        [foo_bar: "bar", BAR: "foo", baz: null] == transformed

        when: "insert has columns with default records"
        def insert = createInsertableRecord(insertableRecordParams)

        insert.insertMetaData.columnsWithDefaults = ["baz"]
        transformed = insert.transformedRecord

        then: "the transformed record does not have them IF they are not in original record"
        [foo_bar: "bar", BAR: "foo"] == transformed
    }

    InsertableRecord createInsertableRecord(LinkedHashMap params) {
        new InsertableRecord(params)
    }
}
