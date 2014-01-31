package metridoc.bd.entities

/**
 * Created by tbarker on 1/31/14.
 */
abstract class BaseShipDate {
    String requestNumber
    String shipDate
    Long shipDateId
    Date processDate
    String exceptionCode
    Date loadTime

    static constraints = {
        requestNumber maxSize: 12, nullable: true
        shipDate maxSize: 24
        processDate(nullable:true)
        exceptionCode(nullable: true, maxSize: 3)
    }

    static mapping = {
        id name: "shipDateId"
        requestNumber(index: "idx_bd_ship_date_request_number")
        shipDate(index: "idx_bd_ship_date_ship_date")
        version defaultValue: '0'
        loadTime defaultValue: '0'
    }

}
