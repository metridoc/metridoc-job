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

import groovy.sql.Sql
import org.slf4j.LoggerFactory

import javax.sql.DataSource
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 7/18/11
 * Time: 3:38 PM
 */
class SqlPlus extends Sql {
    static final slfLog = LoggerFactory.getLogger(SqlPlus)
    static final PHASE_NAMES = "phaseName"
    static final Set IGNORED_KEYS = [PHASE_NAMES, "order"]

    def bulkSqlCalls
    boolean validate = false
    boolean truncate = false

    SqlPlus(DataSource dataSource) {
        super(dataSource)
    }

    /**
     * Supports the sqlplus camel component on to endpoints
     *
     * @param insertOrTable
     * @param batch
     * @param logEachBatch should each batch be logged at info level
     * @return
     */
    int[] runBatch(String insertOrTable, Map<String, Object> batch, boolean logEachBatch) {
        if (batch == null) {
            throw new IllegalArgumentException("a record must be a none null Map to use batch inserting")
        }

        if (!(batch instanceof Map)) {
            throw new IllegalArgumentException("record ${batch} must be of type Map to use batch inserting")
        }

        if (batch.isEmpty()) {
            slfLog.info "there is no data to insert"
            return
        }
        runListBatch([batch], insertOrTable, logEachBatch)
    }

    int[] runBatch(String insertOrTable, Map<String, Object> batch) {
        runBatch(insertOrTable, batch, false)
    }

    int[] runBatch(String insertOrTable, List<Map<String, Object>> batch, boolean logEachBatch) {
        return runListBatch(batch, insertOrTable, logEachBatch)
    }

    int[] runBatch(String insertOrTable, List<Map<String, Object>> batch) {
        runBatch(insertOrTable, batch, false)
    }

    /**
     *
     * @param insertOrTable insert statement or table name
     * @param batch the batch to insert, must be a {@link List} or {@link Map}
     * @param logEachBatch if true, batch progress is logged at info level, otherwise debug
     * @return an array of integers that indicate the number of updates for each statement
     */
    private static logBatch(int[] result, boolean logEachBatch) {

        if (logEachBatch) {
            GString message = getMessage(result)
            slfLog.info(message)
        }

        if (slfLog.isDebugEnabled()) {
            GString message = getMessage(result)
            slfLog.debug(message)
        }
    }

    private static GString getMessage(int[] result) {
        int recordCount = result.size()
        int totalUpdates = 0
        result.each {
            totalUpdates += it
        }
        String message = "processed ${recordCount} records with ${totalUpdates} updates"
        message
    }

    private def runMapBatch(String insertOrTable, Map batch) {

        if (batch == null) {
            throw new IllegalArgumentException("a record must be a none null Map to use batch inserting")
        }

        if (!(batch instanceof Map)) {
            throw new IllegalArgumentException("record ${batch} must be of type Map to use batch inserting")
        }

        runListBatch([batch], insertOrTable)
    }

    private static List<String> orderedParamsFromInsert(String insert) {
        def m = (
        insert =~ /:(\w+)|#(\w+)|\$(\w+)/
        )
        def results = []

        if (m.find()) {
            m.each {
                //colon
                if (it[1] != null) {
                    results.add(it[1])
                }
                //hash
                if (it[2] != null) {
                    results.add(it[2])
                }
                //dollar
                if (it[3] != null) {
                    results.add(it[3])
                }
            }
        }

        return results
    }

    private runListBatch(List batch, String insertOrTable) {
        return runListBatch(batch, insertOrTable, false)
    }

    private runListBatch(List batch, String insertOrTable, boolean logEachBatch) {

        PreparedStatement preparedStatement
        int[] result
        try {
            if (!batch) {
                slfLog.info "there is no data to insert"
                return
            }

            withTransaction { Connection connection ->
                def colNames = []
                DatabaseMetaData myDatabaseMetaData = connection.getMetaData();
                ResultSet crs = myDatabaseMetaData.getColumns(null, null, insertOrTable, null)
                while (crs.next()) {colNames.add(crs.getString("COLUMN_NAME")); }

                def sortedParams = new TreeSet(colNames)
                def sql = getInsertStatement(insertOrTable, sortedParams)
                preparedStatement = connection.prepareStatement(sql)

                if (insertOrTable.split().size() > 1) {
                    sortedParams = orderedParamsFromInsert(insertOrTable)
                }

                batch.each { record ->
                    processRecord(preparedStatement, record, sortedParams)
                }

                slfLog.debug("finished adding {} records to batch, now the batch will be executed", batch.size())
                result = preparedStatement.executeBatch()
                logBatch(result, logEachBatch)
            }
        }
        finally {
            closeResources(null, preparedStatement)
        }

        return result
    }

    void processRecord(PreparedStatement preparedStatement, record, sortedParams) {

        logRecordInsert(record)
        if (record == null) {
            throw new IllegalArgumentException("a record must be a none null Map to use batch inserting")
        }

        if (!(record instanceof Map)) {
            throw new IllegalArgumentException("record ${record} must be of type Map to use batch inserting")
        }

        def params = []
        def recordParams = new TreeSet(record.keySet())
        if (recordParams != sortedParams){
            throw new SQLException("Record keys ${recordParams} do not match column names ${sortedParams}")
        }
        sortedParams.each {
            params.add(record[it])
        }

        setParameters(params, preparedStatement)
        preparedStatement.addBatch()
    }

    private static void logRecordInsert(record) {
        slfLog.debug("adding {} to batch inserts", record)
    }

    /**
     * @deprecated
     */
    static String getInsertStatement(String tableOrInsert, LinkedHashMap record) {
        getInsertStatement(tableOrInsert, record.keySet() as TreeSet)
    }

    static String getInsertStatement(String tableOrInsert, TreeSet sortedParams) {
        def words = tableOrInsert.split()

        //must be an update statement of some sort (insert, update, etc.)
        if (words.size() > 1) {
            return getInsertStatementFromParamInsert(tableOrInsert)
        }

        return getInsertStatementForTable(tableOrInsert, sortedParams)
    }

    private static getInsertStatementForTable(String table, TreeSet sortedParams) {

        slfLog.debug("retrieving insert statement for table {} using record {}", table, sortedParams)
        StringBuffer insert = new StringBuffer("insert into ")
        StringBuffer valuesToInsert = new StringBuffer("values (")
        insert.append(table)
        insert.append(" (")
        sortedParams.each { key ->
            insert.append(key)
            insert.append(", ")
            valuesToInsert.append("?")
            valuesToInsert.append(", ")
        }
        insert.delete(insert.length() - 2, insert.length())
        insert.append(") ")
        valuesToInsert.delete(valuesToInsert.length() - 2, valuesToInsert.length())
        valuesToInsert.append(")")

        return insert.append(valuesToInsert).toString()
    }

    private static String getInsertStatementFromParamInsert(String insert) {
        String result = insert

        //remove colon
        result = result.replaceAll(':\\w+', "?")
        //remove hash
        result = result.replaceAll("#\\w+", "?")
        //remove dollar
        result = result.replaceAll('\\$\\w+', "?")

        return result

    }

    private static String regexReplace(String varPrefix, String text, boolean addQuotes) {
        StringBuilder patternToSearch = new StringBuilder()
        patternToSearch.append('([^\'])')
        patternToSearch.append(varPrefix)
        patternToSearch.append('(\\w+)')

        Pattern p = Pattern.compile(patternToSearch.toString())
        def result = text
        Matcher m = p.matcher(result)

        while (m.find()) {
            def replacement = new StringBuilder()
            replacement.append(m.group(1))
            addQuote(replacement, addQuotes)

            replacement.append('\\$')
            replacement.append(m.group(2))

            addQuote(replacement, addQuotes)

            result = m.replaceFirst(replacement.toString())
            m = p.matcher(result)
        }

        return result
    }

    private static addQuote(StringBuilder builder, boolean addQuote) {
        if (addQuote) {
            builder.append('\'')
        }
    }
}

