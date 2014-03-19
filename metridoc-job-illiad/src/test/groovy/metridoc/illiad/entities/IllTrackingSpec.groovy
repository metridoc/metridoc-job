package metridoc.illiad.entities

import groovy.sql.Sql
import metridoc.illiad.IlliadService
import metridoc.service.gorm.GormService
import org.hibernate.SessionFactory
import org.slf4j.LoggerFactory
import spock.lang.Specification

/**
 * Created by tbarker on 2/9/14.
 */
class IllTrackingSpec extends Specification {

    void "test calculating turnarounds"() {
        setup:
        new GormService(embeddedDataSource: true).enableFor(IllTracking)

        def now = new Date()
        IllTracking.withTransaction {
            new IllTracking(
                    receiveDate: now,
                    requestDate: now - 2,
                    shipDate: now - 1,
                    transactionNumber: 1L,
                    processType: 'Borrowing',
                    requestType: 'Loan'

            ).save(failOnError: true)
        }

        when:
        IllTracking.updateTurnAroundsForAllRecords()
        def illTracking = IllTracking.first()

        then:
        illTracking.turnaroundsProcessed
        illTracking.receiveDate
        illTracking.turnaround_req_rec > 0
        illTracking.turnaround_req_shp > 0
        illTracking.turnaround_shp_rec > 0

        cleanup:
        IllTracking.withTransaction {
            IllTracking.list().each { it.delete() }
        }
    }

    void "test calling directly from the step"() {
        when:
        new Script() {

            @Override
            Object run() {
                includeService(embeddedDataSource: true, GormService).enableFor(IllTracking)
                def now = new Date()

                IllTracking.withTransaction {
                    new IllTracking(
                            receiveDate: now,
                            requestDate: now - 2,
                            shipDate: now - 1,
                            transactionNumber: 1L,
                            processType: 'Borrowing',
                            requestType: 'Loan'

                    ).save(failOnError: true)
                }

                includeService(IlliadService)

                runStep("calculateTurnAroundsForIllTracking")
            }
        }.run()
        def illTracking = IllTracking.first()

        then:
        illTracking.turnaround_req_rec > 0
        illTracking.turnaround_req_shp > 0
        illTracking.turnaround_shp_rec > 0

        cleanup:
        IllTracking.withTransaction {
            IllTracking.list().each { it.delete() }
        }
    }

    void "test full ill_tracking ingestion"() {
        given:
        def service = new GormService(embeddedDataSource: true)
        service.enableFor(IllTracking, IllBorrowing)
        def sql = new Sql(service.dataSource)
        def session = service.sessionFactory.getCurrentSession()

        IllTracking.withTransaction {
            new IllBorrowing(
                    transactionNumber: 1L,
                    requestType: "Loan",
                    transactionStatus: IllBorrowing.AWAITING_COPYRIGHT_CLEARANCE,
                    transactionDate: new Date()
            ).save(failOnError: true)
            new IllBorrowing(
                    transactionNumber: 1L,
                    requestType: "Loan",
                    transactionStatus: IllBorrowing.REQUEST_SENT,
                    transactionDate: new Date() - 3
            ).save(failOnError: true)
            new IllBorrowing(
                    transactionNumber: 1L,
                    requestType: "Loan",
                    transactionStatus: IllBorrowing.SHIPPED,
                    transactionDate: new Date() - 2
            ).save(failOnError: true)
            new IllBorrowing(
                    transactionNumber: 1L,
                    requestType: "Loan",
                    transactionStatus: IllBorrowing.AWAITING_REQUEST_POST_PROCESSING,
                    transactionDate: new Date() - 1
            ).save(failOnError: true)

        }

        when:
        new Script() {
            @Override
            Object run() {
                gormService = service
                includeService(sql: sql, IlliadService)
                runStep("migrateBorrowingDataToIllTracking")
                runStep("doUpdateBorrowing")
                runStep("calculateTurnAroundsForIllTracking")
            }
        }.run()
        IllTracking illTracking
        IllTracking.withNewSession {
            illTracking = IllTracking.findByTransactionNumber(1L)
        }

        then:
        illTracking
        illTracking.orderDate
        illTracking.receiveDate
        illTracking.shipDate
        illTracking.turnaroundsProcessed
        illTracking.turnaround_req_rec
        illTracking.turnaround_req_shp
        illTracking.turnaround_shp_rec
    }
}
