package metridoc.bd.entities

/**
 * Created by tbarker on 1/31/14.
 */
abstract class BaseMinShipDate {
    String requestNumber
    Date minShipDate

    static constraints = {
        requestNumber maxSize: 12
    }

    static mapping = {
        id name: "requestNumber", generator: "assigned"
        version(false)
    }
}
