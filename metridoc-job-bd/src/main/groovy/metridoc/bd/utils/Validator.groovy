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
    DataSource dataSource_from_relais_ezb
    DataSource dataSource
    CamelService camelService
    Sql sql

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

        if (invalidGroup.size() > 0) {
            invalidGroup.each { date ->
                log.info "deleting any existing data for [$date]"
                sql.execute("delete from " + specification.loadingTable + " where " + specification.loadingGroup + " = '${date}'")
                def duplicateExceptions = ""  //Populated if errors are thrown during sync
                def entries = new ArrayList()
                def sqlStmt = "select s.library_id as lender,r.library_id as borrower,r.request_number, " +
                        " abs(cast(HASHBYTES('md5',p.patron_id) as int)) as patron_id,p.patron_type,r.author, " +
                        "r.title,r.local_item_found,r.publisher,r.publication_place,r.publication_year,r.edition,r.isbn,r.isbn_2, " +
                        "r.bibliography_num as LCCN,system_number as oclc_text,r.date_entered as request_date,d.date_processed as process_date, " +
                        "pl.pickup_location_desc as pickup_location,d.supplier_code_1 as supplier_code,h.call_number from " +
                        specification.sourceTables + " where " + specification.sourceFilter +duplicateExceptions+specification.sourceGroup + " = '${date}'"
                camelService.consume("sqlplus:" + sqlStmt + "?dataSource=dataSource_from_relais_bd") { resultSet ->
                    log.info "syncing data for [$date]"
                    def success = false

                    while (!success) {
                        try {
                            camelService.send("sqlplus:" + specification.loadingTable + "?dataSource=dataSource", resultSet)
                            success = true
                        } catch (Exception e) {
                            def split_exception = e.message.tokenize()
                            def entry = split_exception[3]
                            def key = split_exception[6]
                            log.warn "*******************"
                            log.warn "Problem entry is ${entry}"
                            log.warn "Problem key is ${key}"
                            entries.add("'"+entry+"'")
                            duplicateExceptions = "h.request_number not in ("
                            duplicateExceptions += entries.join(", ")
                            duplicateExceptions += ") and "
                            log.warn "Adding following exclusion: ${duplicateExceptions}"
                            sqlStmt = "select s.library_id as lender,r.library_id as borrower,r.request_number, " +
                                    " abs(cast(HASHBYTES('md5',p.patron_id) as int)) as patron_id,p.patron_type,r.author, " +
                                    "r.title,r.local_item_found,r.publisher,r.publication_place,r.publication_year,r.edition,r.isbn,r.isbn_2, " +
                                    "r.bibliography_num as LCCN,system_number as oclc_text,r.date_entered as request_date,d.date_processed as process_date, " +
                                    "pl.pickup_location_desc as pickup_location,d.supplier_code_1 as supplier_code,h.call_number from " +
                                    specification.sourceTables + " where " + specification.sourceFilter +duplicateExceptions+specification.sourceGroup + " = '${date}'"
                            

                        }
                    }
                }
            }
        }
    }

    def validateAndFixEzbBibliography() {
        def specification = [
                filter: ">'2011-12-31'",
                loadingGroup: "substr(process_date,1,10)",
                loadingTable: "ezb_bibliography",
                repository: dataSource,
                sourceConnection: dataSource_from_relais_ezb,
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

        ValidateDBload v = new ValidateDBload(specification)
        v.validate()

        def List invalidGroup = v.getInvalidData()

        if (invalidGroup.size() > 0) {
            invalidGroup.each { date ->
                log.info "deleting any existing data for [$date]"
                sql.execute("delete from " + specification.loadingTable + " where " + specification.loadingGroup + " = '${date}'")
                def duplicateExceptions = ""  //Populated if errors are thrown during sync
                def entries = new ArrayList()
                def sqlStmt = "select s.library_id as lender,r.library_id as borrower,r.request_number, " +
                        " abs(cast(HASHBYTES('md5',p.patron_id) as int)) as patron_id,p.patron_type,r.author, " +
                        "r.title,r.local_item_found,r.publisher,r.publication_place,r.publication_year,r.edition,r.isbn,r.isbn_2, " +
                        "r.bibliography_num as LCCN,oclc_num as oclc,r.date_entered as request_date,d.date_processed as process_date, " +
                        "pl.pickup_location_desc as pickup_location,d.supplier_code_1 as supplier_code,h.call_number from " +
                        specification.sourceTables + " where " + specification.sourceFilter + specification.sourceGroup + " = '${date}'"

                camelService.consume("sqlplus:" + sqlStmt + "?dataSource=dataSource_from_relais_ezb") { resultSet ->
                    log.info "syncing data for [$date]"
                    
                    def success = false

                    while (!success) {
                        try {
                            camelService.send("sqlplus:" + specification.loadingTable + "?dataSource=dataSource", resultSet)
                            success = true
                        } catch (Exception e) {
                            def split_exception = e.message.tokenize()
                            def entry = split_exception[3]
                            def key = split_exception[6]
                            log.warn "*******************"
                            log.warn "Problem entry is ${entry}"
                            log.warn "Problem key is ${key}"
                            entries.add("'"+entry+"'")
                            duplicateExceptions = "h.request_number not in ("
                            duplicateExceptions += entries.join(", ")
                            duplicateExceptions += ") and "
                            log.warn "Adding following exclusion: ${duplicateExceptions}"
                            sqlStmt = "select s.library_id as lender,r.library_id as borrower,r.request_number, " +
                                    " abs(cast(HASHBYTES('md5',p.patron_id) as int)) as patron_id,p.patron_type,r.author, " +
                                    "r.title,r.local_item_found,r.publisher,r.publication_place,r.publication_year,r.edition,r.isbn,r.isbn_2, " +
                                    "r.bibliography_num as LCCN,oclc_num as oclc,r.date_entered,r.date_entered as request_date,d.date_processed as process_date, " +
                                    "pl.pickup_location_desc as pickup_location,d.supplier_code_1 as supplier_code,h.call_number from " +
                                    specification.sourceTables + " where " + specification.sourceFilter +duplicateExceptions+specification.sourceGroup + " = '${date}'"
                        }
                    }
                }
            }
        }
    }

    def validateAndFixEzbCallNumber() {
        def specification = [
                filter: ">'2011-12-31'",
                loadingGroup: "substr(process_date,1,10)",
                loadingTable: "ezb_call_number",
                repository: dataSource,
                sourceConnection: dataSource_from_relais_ezb,
                sourceFile: '',
                sourceFilter: "h.call_number is not null and ",
                sourceGroup: " replace(convert(varchar,d.date_processed,111),'/','-')",
                sourceTables: "id_holdings h left outer join id_delivery d on h.request_number=d.request_number",
                sourceType: "Database"
        ]

        ValidateDBload v = new ValidateDBload(specification)
        v.validate()

        def List invalidGroup = v.getInvalidData()

        if (invalidGroup.size() > 0) {
            invalidGroup.each { date ->
                log.info "deleting any existing data for [$date]"
                sql.execute("delete from " + specification.loadingTable + " where " + specification.loadingGroup + " = '${date}'")

                def sqlStmt = "select h.request_number, h.holdings_seq, h.supplier_code, h.call_number, d.date_processed as process_date from " +
                        specification.sourceTables + " where " + specification.sourceFilter + specification.sourceGroup + " = '${date}'"

                camelService.consume("sqlplus:" + sqlStmt + "?dataSource=dataSource_from_relais_ezb") { resultSet ->
                    log.info "syncing data for [$date]"
                    camelService.send("sqlplus:" + specification.loadingTable + "?dataSource=dataSource", resultSet)
                }
            }
        }
    }

    def validateAndFixBdCallNumber() {
        def specification = [
                filter: ">'2011-12-31'",
                loadingGroup: "substr(process_date,1,10)",
                loadingTable: "bd_call_number",
                repository: dataSource,
                sourceConnection: dataSource_from_relais_bd,
                sourceFile: '',
                sourceFilter: "h.call_number is not null and ",
                sourceGroup: " replace(convert(varchar,d.date_processed,111),'/','-')",
                sourceTables: "id_holdings h left outer join id_delivery d on h.request_number=d.request_number",
                sourceType: "Database"
        ]

        ValidateDBload v = new ValidateDBload(specification)
        v.validate()

        def List invalidGroup = v.getInvalidData()

        if (invalidGroup.size() > 0) {
            invalidGroup.each { date ->

                sql.execute("delete from " + specification.loadingTable + " where " + specification.loadingGroup + " = '${date}'")

                def sqlStmt = "select h.request_number, h.holdings_seq, h.supplier_code, h.call_number, d.date_processed as process_date from " +
                        specification.sourceTables + " where " + specification.sourceFilter + specification.sourceGroup + " = '${date}'"

                log.info "deleted any existing data for [$date]"
                camelService.consume("sqlplus:" + sqlStmt + "?dataSource=dataSource_from_relais_bd") { resultSet ->
                    log.info "syncing data for [$date]"
                    camelService.send("sqlplus:" + specification.loadingTable + "?dataSource=dataSource", resultSet)
                }
            }
        }
    }

    def validateAndFixEzbPrintDate() {
        def specification = [
                filter: ">'2011-12-31'",
                loadingGroup: "substr(process_date,1,10)",
                loadingTable: "ezb_print_date",
                repository: dataSource,
                sourceConnection: dataSource_from_relais_ezb,
                sourceFile: '',
                sourceFilter: "a.stat_location='8' and ",
                sourceGroup: " replace(convert(varchar,d.date_processed,111),'/','-')",
                sourceTables: "id_audit a left outer join id_delivery d on a.request_number=d.request_number" +
                        " left outer join id_event e on replace(replace(replace(e.event_desc,'Print Request',''),'s - ',' - '),' - ','') = replace(a.note,'Printed At: ','')",
                sourceType: "Database"
        ]

        ValidateDBload v = new ValidateDBload(specification)
        v.validate()

        def List invalidGroup = v.getInvalidData()

        if (invalidGroup.size() > 0) {
            invalidGroup.each { date ->
                sql.execute("delete from " + specification.loadingTable + " where " + specification.loadingGroup + " = '${date}'")

                def sqlStmt = "select a.request_number, a.time_stamp as print_date, a.note, d.date_processed as process_date, substring(e.event_rule,22,8) as library_id from " +
                        specification.sourceTables + " where " + specification.sourceFilter + specification.sourceGroup + " = '${date}'"

                camelService.consume("sqlplus:" + sqlStmt + "?dataSource=dataSource_from_relais_ezb") { resultSet ->
                    camelService.send("sqlplus:" + specification.loadingTable + "?dataSource=dataSource", resultSet)
                }
            }
        }
    }

    def validateAndFixBdPrintDate() {
        def specification = [
                filter: ">'2011-12-31'",
                loadingGroup: "substr(process_date,1,10)",
                loadingTable: "bd_print_date",
                repository: dataSource,
                sourceConnection: dataSource_from_relais_bd,
                sourceFile: '',
                sourceFilter: "a.stat_location='8' and ",
                sourceGroup: " replace(convert(varchar,d.date_processed,111),'/','-')",
                sourceTables: "id_audit a left outer join id_delivery d on a.request_number=d.request_number" +
                        " left outer join id_catalog_code c on catalog_code_desc=replace(a.note,'Printed At: ','')",
                sourceType: "Database"
        ]

        ValidateDBload v = new ValidateDBload(specification)
        v.validate()

        def List invalidGroup = v.getInvalidData()

        if (invalidGroup.size() > 0) {
            invalidGroup.each { date ->
                log.info "deleting any existing data for [$date]"
                sql.execute("delete from " + specification.loadingTable + " where " + specification.loadingGroup + " = '${date}'")

                def sqlStmt = "select a.request_number, a.time_stamp as print_date, a.note, d.date_processed as process_date, c.catalog_library_id as library_id from " +
                        specification.sourceTables + " where " + specification.sourceFilter + specification.sourceGroup + " = '${date}'"

                camelService.consume("sqlplus:" + sqlStmt + "?dataSource=dataSource_from_relais_bd") { resultSet ->
                    log.info "syncing data for [$date]"
                    camelService.send("sqlplus:" + specification.loadingTable + "?dataSource=dataSource", resultSet)
                }
            }
        }

        sql.execute("update bd_print_date p join bd_institution i on instr(p.note, i.institution) set p.library_id = i.library_id where p.library_id is null")
    }

    def validateAndFixBdShipDate() {
        def specification = [
                filter: ">'2011-12-31'",
                loadingGroup: "substr(process_date,1,10)",
                loadingTable: "bd_ship_date",
                repository: dataSource,
                sourceConnection: dataSource_from_relais_bd,
                sourceFile: '',
                sourceFilter: "a.exception_code<>'NULL' and ",
                sourceGroup: " replace(convert(varchar,d.date_processed,111),'/','-')",
                sourceTables: "id_audit a left outer join id_delivery d on a.request_number=d.request_number",
                sourceType: "Database"
        ]

        ValidateDBload v = new ValidateDBload(specification)
        v.validate()

        def List invalidGroup = v.getInvalidData()

        if (invalidGroup.size() > 0) {
            invalidGroup.each { date ->
                log.info "deleting any existing data for [$date]"
                sql.execute("delete from " + specification.loadingTable + " where " + specification.loadingGroup + " = '${date}'")

                def sqlStmt = "select a.request_number, a.time_stamp as ship_date, a.exception_code, d.date_processed as process_date from " +
                        specification.sourceTables + " where " + specification.sourceFilter + specification.sourceGroup + " = '${date}'"

                camelService.consume("sqlplus:" + sqlStmt + "?dataSource=dataSource_from_relais_bd") { resultSet ->
                    log.info "syncing data for [$date]"
                    camelService.send("sqlplus:" + specification.loadingTable + "?dataSource=dataSource", resultSet)
                }
            }
        }
    }

    def validateAndFixEzbShipDate() {

        def specification = [
                filter: ">'2011-12-31'",
                loadingGroup: "substr(process_date,1,10)",
                loadingTable: "ezb_ship_date",
                repository: dataSource,
                sourceConnection: dataSource_from_relais_ezb,
                sourceFile: '',
                sourceFilter: "a.exception_code<>'NULL' and ",
                sourceGroup: " replace(convert(varchar,d.date_processed,111),'/','-')",
                sourceTables: "id_audit a left outer join id_delivery d on a.request_number=d.request_number",
                sourceType: "Database"
        ]

        ValidateDBload v = new ValidateDBload(specification)
        v.validate()

        def List invalidGroup = v.getInvalidData()

        if (invalidGroup.size() > 0) {
            invalidGroup.each { date ->
                log.info "deleting any existing data for [$date]"
                sql.execute("delete from " + specification.loadingTable + " where " + specification.loadingGroup + " = '${date}'")

                def sqlStmt = "select a.request_number, a.time_stamp as ship_date, a.exception_code, d.date_processed as process_date from " +
                        specification.sourceTables + " where " + specification.sourceFilter + specification.sourceGroup + " = '${date}'"

                camelService.consume("sqlplus:" + sqlStmt + "?dataSource=dataSource_from_relais_ezb") { resultSet ->
                    camelService.send("sqlplus:" + specification.loadingTable + "?dataSource=dataSource", resultSet)
                }
            }
        }

    }
}

