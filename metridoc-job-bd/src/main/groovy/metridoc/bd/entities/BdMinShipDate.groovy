package metridoc.bd.entities

import grails.persistence.Entity

/**
 * Created by tbarker on 12/11/13.
 */
@Entity
class BdMinShipDate {
    String requestNumber
    Date minShipDate

    static constraints = {
        requestNumber maxSize: 12, unique: true
    }
}
