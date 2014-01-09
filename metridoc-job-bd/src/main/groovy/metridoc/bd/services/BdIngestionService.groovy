package metridoc.bd.services

import metridoc.bd.entities.BdBibliography
import metridoc.bd.entities.BdCallNumber
import metridoc.bd.entities.BdExceptionCode
import metridoc.bd.entities.BdInstitution
import metridoc.bd.entities.BdMinShipDate
import metridoc.bd.entities.BdPatronType
import metridoc.bd.utils.BdUtils
import metridoc.core.Step
import metridoc.core.services.CamelService
import metridoc.service.gorm.GormService
import org.apache.commons.lang.StringUtils

import java.sql.ResultSet

/**
 * Created by tbarker on 12/4/13.
 */
class BdIngestionService {

    GormService gormService
    CamelService camelService
    def bdRelaisSql = new RelaisSql()

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

    @Step(description = "runs entire Borrow Direct and Ez Borrow workflow", depends = ["createTables", "loadLookupTables"])
    void runWorkflow() {}

    private void doSimpleBorrowDirectMigration(Class entity, String uniqueColumn) {
        String unCapEntityName = StringUtils.uncapitalize(entity.simpleName)
        String fromSql = bdRelaisSql."${unCapEntityName}Sql"
        camelService.consumeNoWait("sqlplus:${fromSql}?dataSource=dataSource_from_relais_bd") {ResultSet resultSet ->
            BdUtils.migrateData(resultSet, entity, uniqueColumn)
        }
    }
}
