package metridoc.sql

import groovy.sql.Sql
import groovy.util.logging.Slf4j

import java.sql.PreparedStatement

/**
 * Created by tbarker on 12/6/13.
 */
@Slf4j
class PreparedHandler {
    protected final Map<Set, PreparedStatement> preparedStatements = [:]

    //should not be instantiated
    private PreparedHandler() {}

    int[] executeBatches() {
        def result = []
        preparedStatements.values().each { PreparedStatement preparedStatement ->
            result.addAll preparedStatement.executeBatch()
        }
        return result
    }

    void addToStatement(InsertableRecord record) {
        def statement = getPreparedStatement(record)
        Sql sqlPlus = record.insertMetaData.sqlPlus as Sql
        sqlPlus.setParameters(record.transformedRecord.values() as List, statement)
        statement.addBatch()
    }

    PreparedStatement getPreparedStatement(InsertableRecord insertableRecord) {
        def columnsWithDefaults = insertableRecord.insertMetaData.columnsWithDefaults
        def ignoredColumnsWithDefaults = getIgnoredColumnsWithDefaults(insertableRecord.originalRecord, columnsWithDefaults)
        def statement = preparedStatements[ignoredColumnsWithDefaults]
        if (statement) {
            return statement
        }
        String insertStatement = getInsertStatement(insertableRecord)
        statement = insertableRecord.insertMetaData.connection.prepareStatement(insertStatement)
        preparedStatements[columnsWithDefaults] = statement
        return statement
    }

    static
    protected Set<String> getIgnoredColumnsWithDefaults(Map<String, Object> record, Set<String> columnsWithDefaults) {
        def result = [] as Set<String>

        columnsWithDefaults.each { columnWithDefault ->
            def ignoredDefaultValue = !record.containsKey(columnWithDefault)
            if (ignoredDefaultValue) {
                result << columnWithDefault
            }
        }

        return result
    }

    static String getInsertStatement(InsertableRecord record) {
        if (record.insertMetaData.destinationIsTable()) {
            getInsertStatementForTable(record)
        }
        else {
            getInsertStatementFromParamInsert(record)
        }
    }

    static private getInsertStatementFromParamInsert(InsertableRecord insertableRecord) {
        String result = insertableRecord.insertMetaData.destination

        //remove colon
        result = result.replaceAll(':\\w+', "?")
        //remove hash
        result = result.replaceAll("#\\w+", "?")
        //remove dollar
        result = result.replaceAll('\\$\\w+', "?")

        return result
    }

    static private String getInsertStatementForTable(InsertableRecord record) {
        String table = record.insertMetaData.destination
        Map transformedRecord = record.transformedRecord
        log.debug("retrieving insert statement for table {} using record {}", table, record)
        StringBuffer insert = new StringBuffer("insert into ")
        StringBuffer valuesToInsert = new StringBuffer("values (")
        insert.append(table)
        insert.append(" (")
        transformedRecord.keySet().each { key ->
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
}
