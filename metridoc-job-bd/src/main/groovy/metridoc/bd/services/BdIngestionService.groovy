package metridoc.bd.services

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import metridoc.bd.entities.BdBibliography
import metridoc.bd.entities.BdCallNumber
import metridoc.bd.entities.BdExceptionCode
import metridoc.bd.entities.BdInstitution
import metridoc.bd.entities.BdMinShipDate
import metridoc.bd.entities.BdPatronType
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
    String startDate = "2011-12-31"
    def bdRelaisSql = new RelaisSql()

    @Lazy
    Validator validator = {
        new Validator(
                startDate: startDate,
                camelService: camelService,
                dataSource: dataSource,
                dataSource_from_relais_bd: dataSource_from_relais_bd,
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
                BdPatronType
        )
    }

    @Step(description = "load lookup tables")
    void loadLookupTables() {
        doSimpleBorrowDirectMigration(BdInstitution, "library_id")
        doSimpleBorrowDirectMigration(BdPatronType, "patron_type")
        doSimpleBorrowDirectMigration(BdExceptionCode, "exception_code")
    }

    @Step(description = "load bd_bibliography table")
    void loadBdBibliography() {
        validator.validateAndFixBdBibliography()
    }

    @Step(description = "load bd_bcall_number table")
    void loadBdCallNumber() {
        validator.validateAndFixBdCallNumber()
    }

    @Step(description = "load bd_print_date table")
    void loadPrintDate() {
        validator.validateAndFixBdPrintDate()
    }

    @Step(description = "load bd_ship_date table")
    void loadShipDate() {
        validator.validateAndFixBdShipDate()
    }

    @Step(description = "load bd_min_ship_date table")
    void loadMinShipDate() {
        def query = """
		    REPLACE INTO ezb_min_ship_date (request_number, min_ship_date)
		    SELECT request_number, min(ship_date) as min_ship_date_shd
		    FROM ezb_ship_date shd where shd.ship_date is not null group by shd.request_number
	    """


        log.info "Executing query: " + query
        sql.execute(query);
    }



    @Step(description = "runs entire Borrow Direct and Ez Borrow workflow",
            depends = [
                "createTables",
                "loadLookupTables",
                "loadBdBibliography",
                "loadBdCallNumber",
                "loadPrintDate",
                "loadShipDate"
            ])
    void runWorkflow() {}

    private void doSimpleBorrowDirectMigration(Class entity, String uniqueColumn) {
        String unCapEntityName = StringUtils.uncapitalize(entity.simpleName)
        String fromSql = bdRelaisSql."${unCapEntityName}Sql"
        camelService.consumeNoWait("sqlplus:${fromSql}?dataSource=dataSource_from_relais_bd") { ResultSet resultSet ->
            BdUtils.migrateData(resultSet, entity, uniqueColumn)
        }
    }
}
