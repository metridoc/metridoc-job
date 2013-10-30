package metridoc.illiad.entities

import grails.persistence.Entity
import metridoc.illiad.DateUtil
import org.slf4j.LoggerFactory

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
@Entity
class IllTracking {
    public static final String BORROWING = "Borrowing"
    Long transactionNumber
    String requestType
    String processType
    Date requestDate
    Date shipDate
    Date receiveDate
    Date orderDate
    Double turnaround_shp_rec
    Double turnaround_req_shp
    Double turnaround_req_rec

    static constraints = {
        transactionNumber(unique: true)
        requestDate(nullable: true)
        shipDate(nullable: true)
        receiveDate(nullable: true)
        orderDate(nullable: true)
        turnaround_req_rec(nullable: true)
        turnaround_req_shp(nullable: true)
        turnaround_shp_rec(nullable: true)
    }

    static mapping = {
        version(defaultValue: '0')
        orderDate(index: "idx_ill_tracking_order_date")
        shipDate(index: "idx_ill_tracking_ship_date")
    }

    static updateFromIllBorrowing() {
        updateFromIllBorrowing_AwaitingCopyrightClearance()
        updateFromIllBorrowing_AwaitingRequestProcessing()
    }

    static updateFromIllBorrowing_AwaitingRequestProcessing() {
        Set<Long> alreadyProcessedTransactions
        //need to do a new one since this method might already be surrounded by a transaction
        IllTracking.withNewTransaction {
            alreadyProcessedTransactions = IllTracking.list().collect { it.transactionNumber } as Set
        }
        def itemsToStore = []
        LoggerFactory.getLogger(IllTracking).info "migrating all borrowing data that is awaiting request processing"
        IllBorrowing.findAllByTransactionStatus(IllBorrowing.AWAITING_REQUEST_PROCESSING).each { IllBorrowing borrowing ->
            if (!alreadyProcessedTransactions.contains(borrowing.transactionNumber)) {
                addItem(borrowing, itemsToStore)
            }
        }
        processBatch(itemsToStore)
        LoggerFactory.getLogger(IllTracking).info "finished migrating all borrowing data that is awaiting request processing"
    }

    static updateTurnAroundsForAllRecords() {
        IllTracking.withNewTransaction {
            IllTracking.list().each { IllTracking illTracking ->
                updateTurnArounds(illTracking)
                illTracking.save(failOnError: true)
            }
        }
    }

    private static updateTurnArounds(IllTracking illTracking) {
        def receiveDate = illTracking.receiveDate
        def requestDate = illTracking.requestDate
        def shipDate = illTracking.shipDate

        illTracking.turnaround_req_rec = DateUtil.differenceByDays(receiveDate, requestDate)
        illTracking.turnaround_req_shp = DateUtil.differenceByDays(shipDate, requestDate)
        illTracking.turnaround_shp_rec = DateUtil.differenceByDays(receiveDate, shipDate)
    }

    static updateFromIllBorrowing_AwaitingCopyrightClearance() {
        LoggerFactory.getLogger(IllTracking).info "migrating all borrowing data that is awaiting copyright clearance"
        def itemsToStore = []
        IllBorrowing.findAllByTransactionStatus(IllBorrowing.AWAITING_COPYRIGHT_CLEARANCE).each { IllBorrowing borrowing ->
            addItem(borrowing, itemsToStore)
        }
        processBatch(itemsToStore)
        LoggerFactory.getLogger(IllTracking).info "finished migrating all borrowing data that is awaiting copyright clearance"
    }

    private static addItem(IllBorrowing borrowing, List<IllTracking> itemsToStore) {
        addTrackingItem(createTrackingFromBorrowing(borrowing), itemsToStore)
    }

    private static IllTracking createTrackingFromBorrowing(IllBorrowing borrowing) {
        new IllTracking(
                transactionNumber: borrowing.transactionNumber,
                requestType: borrowing.requestType,
                processType: BORROWING,
                requestDate: borrowing.transactionDate
        )
    }

    private static addTrackingItem(IllTracking illTracking, List<IllTracking> itemsToStore) {
        itemsToStore.add(illTracking)
        if (itemsToStore.size() > 50) {
            processBatch(itemsToStore)
        }
    }

    private static processBatch(List<IllTracking> illTrackingList) {
        IllTracking.withNewTransaction {
            illTrackingList*.save(failOnError: true)
        }
        illTrackingList.clear()
    }
}
