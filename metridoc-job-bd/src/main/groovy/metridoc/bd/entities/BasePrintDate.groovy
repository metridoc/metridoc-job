package metridoc.bd.entities

/**
 * Created by tbarker on 1/31/14.
 */
abstract class BasePrintDate {
    String requestNumber
    Date printDate
    String note
    Date processDate
    Long printDateId
    Date loadTime
    Integer libraryId

    static constraints = {
        requestNumber(maxSize: 12, nullable: true)
        printDate(nullable: true)
        note(nullable: true)
        processDate(nullable:true)
        libraryId(nullable: true)
    }

    static mapping = {
        id name: 'printDateId'
        requestNumber(index: "fk_bd_print_date_request_number")
        version defaultValue: '0'
        loadTime defaultValue: '0'
    }
}
