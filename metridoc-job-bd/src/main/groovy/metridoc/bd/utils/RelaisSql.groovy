package metridoc.bd.utils

/**
 * Created by tbarker on 1/9/14.
 */
class RelaisSql {
    String bdInstitutionSql = "select catalog_code as catalog_code, catalog_code_desc as institution, " +
            "catalog_library_id as library_id from id_catalog_code where catalog_library_id is not null"

    String bdPatronTypeSql = "select patron_type, patron_type_desc from id_patron_type"

    String bdExceptionCodeSql = "select exception_code, exception_code_desc from id_exception_code"

    Closure bdInstitutionCounts = {String startDate ->
        "select 'bd_bibliography' data_store, substr(process_date,1,10) data_group, count(*) group_count" +
        "from bd_bibliography where substr(process_date,1,10) > '$startDate' group by data_group;"
    }
}
