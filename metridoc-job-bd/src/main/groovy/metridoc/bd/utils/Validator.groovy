package metridoc.bd.utils

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import metridoc.core.services.CamelService

import javax.sql.DataSource
import javax.xml.bind.ValidationEvent

/**
 * Validated counts for the large tables
 */
@Slf4j
class Validator {

    String startDate
    DataSource dataSource_from_relais_bd
    DataSource dataSource
    CamelService camelService

    def validateAndFixBdBibliography() {
        def specification = [
                filter: ">'$startDate'",
                loadingGroup: "substr(process_date,1,10)",
                loadingTable: "bd_bibliography",
                repository: dataSource,
                sourceConnection: dataSource_from_relais_bd,
                sourceFile: '',
                sourceFilter: "h.holdings_seq_2=1 and ",
                sourceGroup: " replace(convert(varchar,d.date_processed,111),'/','-')",
                sourceTables: "(((((id_request r left outer join id_delivery d on r.request_number=d.request_number) " +
                        "left outer join id_supplier s on d.supplier_code_1=s.supplier_code) " +
                        "left outer join id_patron p on r.patron_id=p.patron_id and r.library_id=p.library_id) " +
                        "left outer join id_patron_type pt on p.patron_type=pt.patron_type) " +
                        "left outer join id_pickup_location pl on d.deliv_address=pl.pickup_location and r.library_id=PL.library_id) " +
                        "left outer join id_holdings h on r.request_number=h.request_number and d.supplier_code_1=h.supplier_code",
                sourceType: "Database"
        ]

        def validator = new ValidateDBload(specification)
        validator.validate()
        def List invalidGroup = validator.getInvalidData()

        if ( invalidGroup.size()>0 ) {
            invalidGroup.each {date ->
                log.info "deleting any existing data for [$date]"
                Sql.newInstance(dataSource).execute("delete from " + specification.loadingTable + " where " + specification.loadingGroup + " = '${date}'")

                def sqlStmt = "select s.library_id as lender,r.library_id as borrower,r.request_number, " +
                        " abs(cast(HASHBYTES('md5',p.patron_id) as int)) as patron_id,p.patron_type,r.author, " +
                        "r.title,r.publisher,r.publication_place,r.publication_year,r.edition,r.isbn,r.isbn_2, " +
                        "r.bibliography_num as LCCN,system_number as oclc_text,r.date_entered as request_date,d.date_processed as process_date, " +
                        "pl.pickup_location_desc as pickup_location,d.supplier_code_1 as supplier_code,h.call_number from " +
                        specification.sourceTables + " where " + specification.sourceFilter + specification.sourceGroup + " = '${date}'"
                camelService.consume("sqlplus:"+sqlStmt+"?dataSource=dataSource_from_relais_bd") {resultSet ->
                    log.info "syncing data for [$date]"
                    camelService.send("sqlplus:"+specification.loadingTable+"?dataSource=dataSource", resultSet)
                }
            }
        }
    }
}

