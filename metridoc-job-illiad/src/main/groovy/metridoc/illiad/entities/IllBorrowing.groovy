package metridoc.illiad.entities

import grails.persistence.Entity

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
@Entity
class IllBorrowing {

    public static final String AWAITING_REQUEST_PROCESSING = "Awaiting Request Processing"
    public static final String AWAITING_COPYRIGHT_CLEARANCE = "Awaiting Copyright Clearance"
    public static final String REQUEST_SENT = "Request Sent"
    Long transactionNumber
    String requestType
    String transactionStatus
    Date transactionDate

    static mapping = {
        version(defaultValue: '0')
        transactionNumber(index: "idx_ill_borrowing_transaction_num")
        transactionStatus(index: "idx_ill_borrowing_transaction_num,idx_ill_borrowing_transaction_status")
    }

    static constraints = {
    }

}
