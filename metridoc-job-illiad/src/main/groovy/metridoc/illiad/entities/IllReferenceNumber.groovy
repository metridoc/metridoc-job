package metridoc.illiad.entities

import grails.persistence.Entity

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
@Entity
class IllReferenceNumber {
    Long transactionNumber
    String oclc
    String refType
    String refNumber

    static mapping = {
        version defaultValue: '0'
    }

    static constraints = {
        oclc(nullable: true)
        refType(nullable: true)
        refNumber(nullable: true)
    }
}
