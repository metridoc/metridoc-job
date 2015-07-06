/*
  *Copyright 2013 Trustees of the University of Pennsylvania. Licensed under the
  *	Educational Community License, Version 2.0 (the "License"); you may
  *	not use this file except in compliance with the License. You may
  *	obtain a copy of the License at
  *
  *http://www.osedu.org/licenses/ECL-2.0
  *
  *	Unless required by applicable law or agreed to in writing,
  *	software distributed under the License is distributed on an "AS IS"
  *	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  *	or implied. See the License for the specific language governing
  *	permissions and limitations under the License.
  */

package metridoc.illiad

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import metridoc.core.Step
import metridoc.core.services.CamelService
import metridoc.core.tools.CamelTool
import metridoc.core.tools.RunnableTool
import metridoc.illiad.entities.IllGroup
import metridoc.illiad.entities.IllLenderGroup
import metridoc.illiad.entities.IllLendingTracking
import metridoc.illiad.entities.IllTracking
import metridoc.service.gorm.GormService
import metridoc.utils.DataSourceConfigUtil

import javax.sql.DataSource
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.text.SimpleDateFormat

/**
 * Created with IntelliJ IDEA on 9/6/13
 * @author Tommy Barker
 */
@Slf4j
class IlliadService {

    Sql sql
    DataSource dataSource
    Sql sql_from_illiad
    CamelService camelService

    def fromIlliadSqlStatements = new IlliadMsSqlQueries()
    def toIlliadSqlStatements = new IlliadMysqlQueries()
    def illiadHelper = new IlliadHelper(illiadTool: this)

    def _lenderTableName
    def _userTableName
    String startDate
    static final LENDER_ADDRESSES_ALL = "LenderAddressesAll"
    static final LENDER_ADDRESSES = "LenderAddresses"
    static final USERS = "Users"
    static final USERS_ALL = "UsersAll"
    public static final String OTHER = "Other"

    List illiadTables = [
            "ill_group",
            "ill_lending",
            "ill_borrowing",
            "ill_user_info",
            "ill_transaction",
            "ill_lender_info",
            "ill_lender_group",
            "ill_lending_tracking",
            "ill_location",
            "ill_reference_number",
            "ill_tracking"
    ]

    @Step(description = "running full workflow", depends = [
        "truncateLoadingTables",
        "migrateData",
        "migrateBorrowingDataToIllTracking",
        "doUpdateBorrowing",
        "doUpdateLending",
        "doIllGroupOtherInsert",
        "cleanUpIllTransactionLendingLibraries",
        "calculateTurnAroundsForIllTracking",
        "calculateTurnAroundsForIllLendingTracking",
        "updateCache"
    ])
    void runFullWorkflow() {}

    @Step(description = "truncating loading tables")
    void truncateLoadingTables() {
        illiadTables.each {
            log.info "truncating table ${it} in the repository"
            getSql().execute("truncate ${it}" as String)
        }
    }

    @Step(description = "migrates data", depends = ["truncateLoadingTables"])
    void migrateData() {
        log.info "migrating data to ${dataSource.connection.metaData.getURL()}"

        [
                ill_group: fromIlliadSqlStatements.groupSqlStmt,
                ill_lender_group: fromIlliadSqlStatements.groupLinkSqlStmt,
                ill_lender_info: fromIlliadSqlStatements.lenderAddrSqlStmt(lenderTableName as String),
                ill_reference_number: fromIlliadSqlStatements.referenceNumberSqlStmt,
                ill_transaction: fromIlliadSqlStatements.transactionSqlStmt(getStartDate()),
                ill_lending: fromIlliadSqlStatements.lendingSqlStmt(getStartDate()),
                ill_borrowing: fromIlliadSqlStatements.borrowingSqlStmt(getStartDate()),
                ill_user_info: fromIlliadSqlStatements.userSqlStmt(userTableName as String)

        ].each { key, value ->
            log.info("migrating to ${key} using \n    ${value}" as String)
            camelService.with {
                consumeNoWait("sqlplus:${value}?dataSource=dataSource_from_illiad") { ResultSet resultSet ->
                    if(resultSet){
                         while (resultSet.next()) {
                             ResultSetMetaData rsmd = resultSet.getMetaData();
                             int columnsNumber = rsmd.getColumnCount();
                             for (int i=0; i<columnsNumber;i++){
                                 String item = resultSet.getString(i)
                                 log.info("${item}")
                             }
                         }
                    }
                    send("sqlplus:${key}?dataSource=dataSource", resultSet)
                }
            }
        }
    }

    @Step(description = "migrates data from illborrowing to ill_tracking")
    void migrateBorrowingDataToIllTracking() {
        IllTracking.updateFromIllBorrowing()
    }

    @Step(description = "updates the borrowing tables")
    void doUpdateBorrowing() {
        [
                fromIlliadSqlStatements.orderDateSqlStmt,
                fromIlliadSqlStatements.shipDateSqlStmt,
                fromIlliadSqlStatements.receiveDateSqlStmt,
                fromIlliadSqlStatements.articleReceiveDateSqlStmt
        ].each {
            log.info "update borrowing with sql statement $it"
            getSql().execute(it as String)
        }
    }

    @Step(description = "updates the lending table")
    void doUpdateLending() {
        [
                fromIlliadSqlStatements.arrivalDateSqlStmt,
                fromIlliadSqlStatements.completionSqlStmt,
                fromIlliadSqlStatements.shipSqlStmt,
                fromIlliadSqlStatements.cancelledSqlStmt
        ].each {
            log.info "updating lending with sql statement $it"
            getSql().execute(it as String)
        }
    }

    @Step(description = "inserts extra records into ill_group to deal with 'OTHER'")
    void doIllGroupOtherInsert() {
        IllGroup.withTransaction {
            new IllGroup(groupNo: IlliadHelper.GROUP_ID_OTHER, groupName: OTHER).save(failOnError: true)
            new IllLenderGroup(groupNo: IlliadHelper.GROUP_ID_OTHER, lenderCode: OTHER).save(failOnError: true)
        }
    }

    @Step(description = "calculates turnarounds for ill_tracking")
    void calculateTurnAroundsForIllTracking() {
        IllTracking.updateTurnAroundsForAllRecords()
    }

    @Step(description = "calculates turnarounds for ill_lending_tracking")
    void calculateTurnAroundsForIllLendingTracking() {
        println "calculating turnarounds for ill_lending_tracking"
        IllLendingTracking.updateTurnAroundsForAllRecords()
    }

    @Step(description = "cleans up data in ill_transaction, ill_lending_tracking and ill_tracking to facilitate agnostic sql queries in the dashboard")
    void cleanUpIllTransactionLendingLibraries() {
        sql.withTransaction {
            int updates
            updates = getSql().executeUpdate("update ill_transaction set lending_library = 'Other' where lending_library is null")
            log.info "changing all lending_library entries in ill_transaction from null to other caused $updates updates"
            updates = getSql().executeUpdate("update ill_transaction set lending_library = 'Other' where lending_library not in (select distinct lender_code from ill_lender_group)")
            log.info "changing all lending_library entries in ill_transaction that are not in ill_lender_group to other caused $updates updates"
        }
    }

    @Step(description = "updates reporting cache")
    void updateCache() {
        illiadHelper.storeCache()
    }

    @Step(description = "drops illiad tables")
    void dropTables() {
        illiadTables.each {
            sql.execute("drop table $it" as String)
        }
    }

    def getLenderTableName() {
        if (_lenderTableName) return _lenderTableName

        _lenderTableName = pickTable(LENDER_ADDRESSES_ALL, LENDER_ADDRESSES)
    }

    def getUserTableName() {
        if (_userTableName) return _userTableName

        _userTableName = pickTable(USERS, USERS_ALL)
    }

    String getStartDate() {
        if (startDate) return startDate

        def formatter = new SimpleDateFormat('yyyyMMdd')
        def fiscalYear = DateUtil.currentFiscalYear
        def startDateAsDate = DateUtil.getFiscalYearStartDate(fiscalYear)

        startDate = formatter.format(startDateAsDate)
    }

    private pickTable(option1, option2) {
        if (tableExists(option1)) {
            return option1
        }
        else {
            return option2
        }
    }

    private tableExists(tableName) {
        try {
            sql_from_illiad.execute("select count(*) from $tableName" as String)
            return true
        }
        catch (SQLException ignored) {
            //table does not exist
            return false
        }
    }
}
