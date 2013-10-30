package metridoc.illiad.entities

import grails.persistence.Entity

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
@Entity
class IllLending {

    Long transactionNumber
    String requestType
    String status
    Date transactionDate

    static mapping = {
        version(defaultValue: '0')
    }

    static constraints = {
    }
}
