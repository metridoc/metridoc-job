package metridoc.bd.utils

import groovy.sql.Sql
import groovy.util.logging.Slf4j

/**
 * Created by tbarker on 1/30/14.
 */
@Slf4j
class ValidateDBload extends ValidateRecordCounts {

    ValidateDBload ( specs ) { spec = specs; }

    /**
     *  construct a sql query to create a temporary work table from specific values passed into a class level hash
     *
     *  @returns String - create table query to hold application-specific ingested row counts for validation
     */
    String populateWorkTable() {
        StringBuffer workSql = new StringBuffer("create table if not exists work_table select '")
        workSql.append( spec.loadingTable )
        workSql.append( "' data_store," )
        workSql.append( spec.loadingGroup )
        workSql.append( " data_group, count(*) group_count from " )
        workSql.append( spec.loadingTable )
        workSql.append( " where " )
        workSql.append( spec.loadingGroup )
        workSql.append( spec.filter )
        workSql.append( " group by data_group" )
        return workSql.toString()
    }

    /**
     *  construct a sql query to create a temporary work table to hold values
     *
     *  @returns String - create table query to hold source row counts to validate against
     */
    String sourceCount() {
        StringBuffer countSql = new StringBuffer("select '")
        countSql.append( spec.loadingTable )
        countSql.append( "' data_store," )
        countSql.append( spec.sourceGroup )
        countSql.append( " data_group, count('x') source_count from " )
        countSql.append( spec.sourceTables )
        countSql.append( " where " )
        countSql.append( spec.sourceFilter )
        countSql.append( spec.sourceGroup )
        countSql.append( spec.filter )
        countSql.append( " group by" )
        countSql.append( spec.sourceGroup )
        return countSql.toString()
    }

    /**
     *  executes queries to gather row counts from source tables and loading tables for comparison
     */
    void loadCounts() {
        Sql targetSql = Sql.newInstance( spec.repository )
        Sql sourceSql = Sql.newInstance( spec.sourceConnection )
        log.info "getting source counts..."
        boolean alreadyLoggedInsertInfo = false
        def sourceCountSql = sourceCount()
        log.info "calling $sourceCountSql to get data from relais"
        sourceSql.eachRow ( sourceCountSql ) {
            if(!alreadyLoggedInsertInfo) {
                //only log once
                log.info "beginning insert into validate_counts"
                alreadyLoggedInsertInfo = true
            }
            String populateValidationTable = "insert into validate_counts values ( '${it.data_store}', '${it.data_group}', ${it.source_count}, 0 )"
            targetSql.execute( populateValidationTable )
        }

        log.info "getting loaded counts..."
        targetSql.execute( populateWorkTable() )
        targetSql.execute( UPDATE_VALIDATION_TABLE )
    }

    /**
     *  main method to invoke the validation process
     */
    void validate() {
        setup( spec.loadingTable )
        loadCounts()
        report()
    }
}
