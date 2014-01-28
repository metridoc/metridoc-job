package metridoc.bd.utils

import groovy.sql.Sql

import javax.sql.DataSource

/**
 * Validated counts for the large tables
 */
class Validator {

    String startDate = "2011-12-31"
    DataSource dataSource_from_relais_bd
    DataSource dataSource
    Sql sql
    Sql sql_from_relais_bd

    def relaisSql = new RelaisSql()

    def validateBibliography() {
        def specification = [
                filter: ">'2011-12-31'",
                loadingGroup: "substr(process_date,1,10)",
                loadingTable: "bd_bibliography",
                repository: dataSource_from_relais_bd,
                sourceConnection: dataSource,
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
    }

    List<String> getInvalidDates(String sourceSqlForCounts, String targetSqlForCounts) {
        String query = relaisSql.bdInstitutionCounts(startDate)
        def targetCounts = getCounts(query, sql)
    }

    static TreeMap<String, Integer> getCounts(String sqlForCounts, Sql sql) {
        Map<String, Integer> result = [:] as TreeMap
        sql.rows(sqlForCounts) {row ->
            result[row.data_group] = row.group_count
        }

        return result
    }
}

