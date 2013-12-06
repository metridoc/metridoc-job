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

import groovy.util.logging.Slf4j
import metridoc.iterators.Record
import metridoc.iterators.RecordIterator
import metridoc.sql.InsertMetaData
import metridoc.sql.InsertableRecord
import metridoc.sql.PreparedHandler
import metridoc.sql.SqlPlus

import javax.sql.DataSource
import java.sql.Connection
import java.sql.PreparedStatement

/**
 * @deprecated
 */
@Slf4j
class DataSourceIteratorWriter extends DefaultIteratorWriter {
    public static final String DATASOURCE_MESSAGE = "dataSource cannot be null"
    public static final String TABLE_NAME_ERROR = "tableName cannot be null"
    public static final String ROW_ITERATOR_ERROR = "record Iterator cannot be null"
    DataSource dataSource
    String tableName

    @Lazy
    SqlPlus sql = {
        assert dataSource != null: "dataSource must not be null"
        new SqlPlus(dataSource)
    }()

    @Override
    WriteResponse write(RecordIterator recordIterator) {
        assert dataSource != null: DATASOURCE_MESSAGE
        assert tableName != null: TABLE_NAME_ERROR
        assert recordIterator != null: ROW_ITERATOR_ERROR
        def headers = recordIterator.recordHeaders
        def handler = new PreparedHandler()
        headers.preparedHandler = handler

        try {
            def totals = null
            try {
                sql.withTransaction { Connection connection ->
                    def insertMetaData = new InsertMetaData(
                            destination: tableName,
                            connection: connection,
                            sqlPlus: sql
                    )
                    headers.insertMetaData = insertMetaData

                    totals = super.write(recordIterator)
                    if (totals.fatalErrors) {
                        //throw the first one
                        throw totals.fatalErrors[0]
                    }
                    totals.body.batchResponse = handler.executeBatches()
                }
            }
            catch (Throwable throwable) {
                //no records were written... start from scratch
                totals = new WriteResponse()
                totals.aggregateStats[WrittenRecordStat.Status.ERROR] = 1
                totals.fatalErrors << throwable
            }
            return totals
        }
        finally {
            sql.close()
        }
    }

    @Override
    boolean doWrite(int lineNumber, Record record) {
        validateState(sql, "sqlPlus service cannot be null")
        def headers = record.headers
        InsertMetaData metaData = headers.insertMetaData
        def insertableRecord = new InsertableRecord(insertMetaData: metaData, originalRecord: record.body)
        PreparedHandler handler = headers.preparedHandler
        handler.addToStatement(insertableRecord)

        return true
    }
}
