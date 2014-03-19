/*
  *Copyright 2013 Trustees of the University of Pennsylvania. Licensed under the
  *	Educational Community License, Version 2.0 (the "License"); you may
  *	not use this file except in compliance with the License. You may
  *	obtain a copy of the License at
  *
  *http://www.osedu.org/licenses/ECL-2.0
  *
  *	Unless required by applicable law or agreed to in writing,
  *	software distributed under the License is distributed on an "AS IS"
  *	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  *	or implied. See the License for the specific language governing
  *	permissions and limitations under the License.
  */

package metridoc.illiad.entities

import grails.persistence.Entity
import metridoc.illiad.DateUtil
import org.hibernate.Session
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
    boolean turnaroundsProcessed

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
        turnaroundsProcessed(index: "idx_ill_tracking_turn", defaultValue: '0')
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
        boolean notDone = true
        int count = 0
        while (notDone) {
            def illTrackingList
            IllTracking.withNewSession {
                illTrackingList = IllTracking.findAllByTurnaroundsProcessed(false, [max: 10000])
                if (illTrackingList) {
                    IllTracking.withTransaction {
                        illTrackingList.each { IllTracking illTracking ->
                            count++
                            updateTurnArounds(illTracking)
                            illTracking.save(failOnError: true)
                        }
                    }

                } else {
                    notDone = false
                }
            }

            LoggerFactory.getLogger(IllTracking).info "processed [$count] records"
        }
    }

    private static updateTurnArounds(IllTracking illTracking) {
        def receiveDate = illTracking.receiveDate
        def requestDate = illTracking.requestDate
        def shipDate = illTracking.shipDate

        illTracking.turnaround_req_rec = DateUtil.differenceByDays(receiveDate, requestDate)
        illTracking.turnaround_req_shp = DateUtil.differenceByDays(shipDate, requestDate)
        illTracking.turnaround_shp_rec = DateUtil.differenceByDays(receiveDate, shipDate)
        illTracking.turnaroundsProcessed = true
        def log = LoggerFactory.getLogger(IllTracking)
        if (log.isDebugEnabled()) {
            log.debug(
                    "for illTracking-${illTracking.id}, req_rec = ${illTracking.turnaround_req_rec}, req_shp = ${illTracking.turnaround_req_shp}, shp_rec = ${illTracking.turnaround_shp_rec}"
            )
        }
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
        IllTracking.withTransaction {
            illTrackingList*.save(failOnError: true)
        }
        illTrackingList.clear()
    }
}
