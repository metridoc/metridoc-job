package metridoc.bd.entities

import grails.persistence.Entity

/**
 * Created by tbarker on 12/5/13.
 */
@Entity
class BdCallNumber {
    Long callNumberId
    String requestNumber
    Boolean holdingsSeq
    String supplierCode
    String callNumber
    Date processDate
    Date loadTime

    static constraints = {
        requestNumber(nullable: true, maxSize: 12)
        holdingsSeq(nullable: true)
        supplierCode(nullable: true, maxSize: 20)
        callNumber(nullable: true)
        processDate(nullable: true)
    }

    static mapping = {
        id name: "callNumberId"
        requestNumber index: "request_number"
    }
}
