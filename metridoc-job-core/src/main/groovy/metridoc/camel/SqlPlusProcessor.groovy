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

import groovy.stream.Stream
import metridoc.utils.ColumnConstrainedList
import metridoc.utils.ColumnConstrainedMap
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

import java.sql.ResultSet

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 8/5/11
 * Time: 9:46 AM
 */

class SqlPlusProcessor extends SqlPlusMixin implements Processor {

    SqlPlusEndpoint endpoint
    static final log = LoggerFactory.getLogger(SqlPlusProcessor)

    void process(Exchange exchange) {
        def body = exchange.in.body

        if (body instanceof Stream) {
            handleBatchStream(query, body)
            return;
        }

        def isResultSet = exchange.in.getBody(ResultSet.class)
        def isMap = exchange.in.getBody(Map.class)
        def isList = exchange.in.getBody(List.class)
        def tableOrInsert = query
        def columns = endpoint.columns

        if (isList) {
            def listToInsert = isList
            if (columns) {
                listToInsert = new ColumnConstrainedList(isList, columns)
            }
            handleListOrMap(tableOrInsert, listToInsert)
            return;
        }

        if (isMap) {
            def mapToInsert = isMap
            if (columns) {
                mapToInsert = new ColumnConstrainedMap(isMap, columns)
            }
            handleListOrMap(tableOrInsert, mapToInsert)
            return;
        }

        if (isResultSet) {
            handleResultSet(tableOrInsert, isResultSet)
            return;
        }

        throw new IllegalArgumentException("Sqlplus can only process a Map, List, ResultSet or Iterator")
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    private void handleListOrMap(String tableOrInsert, Object records) {
        try {
            sql.runBatch(tableOrInsert, records, logBatches)
        }
        finally {
            sql.close()
        }
    }

    private void handleResultSet(String tableOrInsert, ResultSet resultSet) {
        handleBatchResultSet(tableOrInsert, resultSet)
    }

    private void handleBatchResultSet(String tableOrInsert, ResultSet resultSet) {
        handleBatchStream(tableOrInsert, Stream.fromResultSet(resultSet))
    }

    private void handleBatchStream(String tableOrInsert, Stream stream) {

        def sql = getSql()

        try {
            int count = 0
            def collated = stream.map{count++; return it}.collate(getBatchSize())

            def streamToUse = collated
            if(logBatches) {
                streamToUse = collated.map {
                    log.info "inserted [$count] records"
                    return it
                }
            }

            streamToUse.each {List<Map<String, Object>> batch ->
                sql.runBatch(tableOrInsert, batch)
            }
        }
        finally {
            sql.close()
        }
    }
}
