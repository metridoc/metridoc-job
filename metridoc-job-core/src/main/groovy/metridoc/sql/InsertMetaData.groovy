package metridoc.sql

import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Created by tbarker on 12/6/13.
 */
class InsertMetaData {
    String destination
    Connection connection
    SqlPlus sqlPlus

    @Lazy
    DatabaseMetaData databaseMetaData = {
        connection.getMetaData()
    }()

    @Lazy(soft = true)
    Collection<String> sortedParams = {
        if(destinationIsTable()) {
            def colNames = new TreeSet()
            def columnResultSet = createColumnResultSet()
            boolean resultExists = false
            while (columnResultSet.next()) {
                resultExists = true
                def rowResult = columnResultSet.toRowResult()
                def columnName = rowResult.COLUMN_NAME
                colNames.add(columnName);
            }

            if(!resultExists) {
                throw new SQLException("Could not create params from [$destination], is the name right?  Is capitalization right?")
            }
            return colNames
        }
        else {
            def m = (
                    destination =~ /:(\w+)|#(\w+)|\$(\w+)/
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
    }()

    @Lazy(soft = true)
    Set<String> columnsWithDefaults = {
        def columnResultSet = createColumnResultSet()
        def columnsWithDefaults = [] as Set<String>
        if (destinationIsTable()) {
            boolean resultExists = false
            while (columnResultSet.next()) {
                resultExists = true
                def rowResult = columnResultSet.toRowResult()
                def columnName = rowResult.COLUMN_NAME
                def defaultValue = rowResult.COLUMN_DEF
                if (defaultValue != null) {
                    columnsWithDefaults << columnName
                }
            }

            if(!resultExists) {
                throw new SQLException("Could not create params from [$destination], is the name right?  Is capitalization right?")
            }
        }

        return columnsWithDefaults
    }()

    boolean destinationIsTable() {
        !isDestinationAnInsertOrUpdate()
    }

    boolean isDestinationAnInsertOrUpdate() {
        destination.split().size() > 1
    }

    private ResultSet createColumnResultSet() {
        databaseMetaData.getColumns(null, null, destination, null)
    }
}
