package metridoc.bd.services

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import metridoc.bd.entities.BdBibliography
import metridoc.bd.entities.BdCallNumber
import metridoc.bd.entities.BdExceptionCode
import metridoc.bd.entities.BdInstitution
import metridoc.bd.entities.BdMinShipDate
import metridoc.bd.entities.BdPatronType
import metridoc.bd.entities.BdPrintDate
import metridoc.bd.entities.BdShipDate
import metridoc.bd.entities.EzbCallNumber
import metridoc.bd.entities.EzbExceptionCode
import metridoc.bd.entities.EzbInstitution
import metridoc.bd.entities.EzbMinShipDate
import metridoc.bd.entities.EzbPatronType
import metridoc.bd.entities.EzbPrintDate
import metridoc.bd.entities.EzbShipDate
import metridoc.bd.utils.BdUtils
import metridoc.bd.utils.RelaisSql
import metridoc.bd.utils.Validator
import metridoc.core.Step
import metridoc.core.services.CamelService
import metridoc.service.gorm.GormService
import org.apache.commons.lang.StringUtils

import javax.sql.DataSource
import java.sql.ResultSet

/**
 * Created by tbarker on 12/4/13.
 */
@Slf4j
class BdIngestionService {

    GormService gormService
    CamelService camelService
    DataSource dataSource
    Sql sql
    DataSource dataSource_from_relais_bd
    DataSource dataSource_from_relais_ezb
    String startDate = "2011-12-31"
    def relaisSql = new RelaisSql()

    @Lazy
    Validator validator = {
        new Validator(
                startDate: startDate,
                camelService: camelService,
                dataSource: dataSource,
                dataSource_from_relais_bd: dataSource_from_relais_bd,
                dataSource_from_relais_ezb: dataSource_from_relais_ezb,
                sql: sql
        )
    }()

    @Step(description = "create tables for Borrow Direct and EzBorrow")
    void createTables() {
        gormService.enableFor(
                BdBibliography,
                BdCallNumber,
                BdExceptionCode,
                BdInstitution,
                BdMinShipDate,
                BdPatronType,
                BdPrintDate,
                BdShipDate,
                EzbInstitution,
                EzbPatronType,
                EzbExceptionCode,
                EzbCallNumber,
                EzbPrintDate,
                EzbShipDate,
                EzbMinShipDate
        )
    }

    @Step(description = "load lookup tables", depends = "createTables")
    void loadLookupTables() {
        doSimpleBorrowDirectMigration(BdInstitution, "library_id")
        doSimpleBorrowDirectMigration(BdPatronType, "patron_type")
        doSimpleBorrowDirectMigration(BdExceptionCode, "exception_code")
        doSimpleEZBorrowMigration(EzbInstitution, "library_id")
        doSimpleEZBorrowMigration(EzbPatronType, "patron_type")
        doSimpleEZBorrowMigration(EzbExceptionCode, "exception_code")
    }

    @Step(description = "load bd_bibliography table")
    void loadBdBibliography() {
        validator.validateAndFixBdBibliography()
    }

    @Step(description = "load bd_bibliography table")
    void loadEzbBibliography() {
        validator.validateAndFixEzbBibliography()
    }

    @Step(description = "load bd_bcall_number table")
    void loadBdCallNumber() {
        validator.validateAndFixBdCallNumber()
    }

    @Step(description = "load ezb_call_number table")
    void loadEzbCallNumber() {
        validator.validateAndFixEzbCallNumber()
    }

    @Step(description = "load bd_print_date table")
    void loadBdPrintDate() {
        validator.validateAndFixBdPrintDate()
    }

    @Step(description = "load bd_print_date table")
    void loadEzbPrintDate() {
        validator.validateAndFixEzbPrintDate()
    }

    @Step(description = "load bd_ship_date table")
    void loadBdShipDate() {
        validator.validateAndFixBdShipDate()
    }

    @Step(description = "load ezb_ship_date table")
    void loadEzbShipDate() {
        validator.validateAndFixEzbShipDate()
    }

    @Step(description = "load min ship dates")
    void loadMinShipDate() {
        def query = """
		    REPLACE INTO bd_min_ship_date (request_number, min_ship_date)
		    SELECT request_number, min(ship_date) as min_ship_date_shd
		    FROM bd_ship_date shd where shd.ship_date is not null group by shd.request_number
	    """


        log.info "Executing query: " + query
        sql.execute(query);

        query = """
		    REPLACE INTO ezb_min_ship_date (request_number, min_ship_date)
		    SELECT request_number, min(ship_date) as min_ship_date_shd
		    FROM ezb_ship_date shd where shd.ship_date is not null group by shd.request_number
	    """


        log.info "Executing query: " + query
        sql.execute(query);
    }


    @Step(description = "resolve oclc numbers on bd")
    void resolveOclcNumbers() {
        def rows = sql.rows("select * from bd_bibliography where oclc_text is not null and oclc is null")

        def recordsToUpdate = [:]

        rows.each {row ->
            recordsToUpdate[row.bibliography_id] = getNumber(row.oclc_text)
        }

        sql.withTransaction {
            recordsToUpdate.each {key, value ->
                sql.executeUpdate("update bd_bibliography set oclc = ${value} where bibliography_id = ${key}")
            }
        }
    }

    private Integer getNumber(String oclcText) {
        Integer result
        try {
            result = Integer.valueOf(oclcText)
        }
        catch (Throwable ignored) {
            //ignore, must not be a number
        }

        if(result == null) {
            def m = oclcText =~ /^[^0-9]*(\d+)$/
            m.find()
            result = Integer.valueOf(m.group(1))
        }

        return result
    }

    @Step(description = "runs entire Borrow Direct and Ez Borrow workflow",
            depends = [
                "createTables",
                "loadLookupTables",
                "loadBdBibliography",
                "loadBdCallNumber",
                "loadBdPrintDate",
                "loadBdShipDate",
                "loadEzbBibliography",
                "loadEzbCallNumber",
                "loadEzbPrintDate",
                "loadEzbShipDate",
                "loadMinShipDate",
                "resolveOclcNumbers"
            ])
    void runWorkflow() {}

    private void doSimpleEZBorrowMigration(Class entity, String uniqueColumn) {
        doSimpleMigration(entity, uniqueColumn, "dataSource_from_relais_ezb")
    }

    private void doSimpleBorrowDirectMigration(Class entity, String uniqueColumn) {
        doSimpleMigration(entity, uniqueColumn, "dataSource_from_relais_bd")
    }

    private void doSimpleMigration(Class entity, String uniqueColumn, String relaisDataSourceName) {
        String unCapEntityName = StringUtils.uncapitalize(entity.simpleName)
        String fromSql = relaisSql."${unCapEntityName}Sql"
        camelService.consumeNoWait("sqlplus:${fromSql}?dataSource=$relaisDataSourceName") { ResultSet resultSet ->
            BdUtils.migrateData(resultSet, entity, uniqueColumn)
        }
    }
}
