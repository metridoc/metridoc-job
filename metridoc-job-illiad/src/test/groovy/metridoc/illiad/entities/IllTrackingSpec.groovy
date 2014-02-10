package metridoc.illiad.entities

import metridoc.illiad.IlliadService
import metridoc.service.gorm.GormService
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
        illTracking.turnaround_req_rec > 0
        illTracking.turnaround_req_shp > 0
        illTracking.turnaround_shp_rec > 0

        cleanup:
        IllTracking.withTransaction {
            IllTracking.list().each{it.delete()}
        }
    }

    void "test calling directly from the step"() {
        when:
            new Script(){

                @Override
                Object run() {
                    includeService(embeddedDataSource: true,  GormService).enableFor(IllTracking)
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
            IllTracking.list().each{it.delete()}
        }
    }
}
