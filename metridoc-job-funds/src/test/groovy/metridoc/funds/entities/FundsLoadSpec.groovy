package metridoc.funds.entities

import groovy.sql.Sql
import metridoc.service.gorm.GormService
import spock.lang.Specification

/**
 * Created by tbarker on 1/2/14.
 */
class FundsLoadSpec extends Specification {

    void "test the id name and no version column"() {
        given:
        def service = new GormService(embeddedDataSource: true)
        def sql = new Sql(service.dataSource)

        when:
        service.enableFor(FundsLoad)
        service.withTransaction {
            new FundsLoad(loadTime: new Date()).save(failOnError: true)
        }

        then:
        noExceptionThrown()
        def row = sql.firstRow("select * from funds_load")
        !row.containsKey("id")
        !row.containsKey("version")
        row.containsKey("load_id")
    }
}
