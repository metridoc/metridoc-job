package metridoc.illiad.entities

import grails.persistence.Entity
import metridoc.illiad.DateUtil

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
@Entity
class IllLendingTracking {
    Long transactionNumber
    String requestType
    Date arrivalDate
    Date completionDate
    String completionStatus
    Double turnaround

    static mapping = {
        version(defaultValue: '0')
    }

    static constraints = {
        transactionNumber(unique: true)
        arrivalDate(nullable: true)
        completionDate(nullable: true)
        completionStatus(nullable: true)
        turnaround(nullable: true)
    }

    static void updateTurnAroundsForAllRecords() {
        IllLendingTracking.withNewTransaction {
            IllLendingTracking.list().each { IllLendingTracking illLendingTracking ->
                illLendingTracking.turnaround = DateUtil.differenceByDays(illLendingTracking.completionDate, illLendingTracking.arrivalDate)
            }
        }
    }
}
