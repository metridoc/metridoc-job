package metridoc.ezproxy.entities

import metridoc.service.gorm.GormService
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 10/8/13
 * @author Tommy Barker
 */
class EzDoiSpec extends Specification {

    void "when extractDoi throws error, populate will fail with assertion error saying doi is null"() {
        given: "a bad url"
        def badUrl = "http://foo?doi=10.%2"

        when: "extract doi is called"
        def doi = new EzDoi()
        doi.extractDoi(badUrl)

        then:
        thrown(Throwable)

        when: "populate is called"
        def body = [
                url: badUrl,
                ezproxyId: "asdasdf",
                fileName: "kjahsdfkjahsdf",
                urlHost: "foo"
        ]
        def ezDoi = new EzDoi()

        then:
        !ezDoi.acceptRecord(body)
    }

    void "test alreadyExists"() {
        given:
        def gormService = new GormService(embeddedDataSource: true)
        gormService.init()
        gormService.enableFor(EzDoi)
        EzDoi.withTransaction {
            new EzDoi(
                    doi: "foo",
                    ezproxyId: "bar",
                    fileName: "foobar",
                    lineNumber: 1,
                    proxyDate: new Date(),
                    proxyDay: 1,
                    proxyMonth: 1,
                    proxyYear: 2012,
                    urlHost: "http://foo.com"
            ).save(failOnError: true)
        }

        when:
        boolean exists = new EzDoi(
                doi: "foo",
                ezproxyId: "bar"
        ).alreadyExists()

        then:
        noExceptionThrown()
        exists
    }
}
