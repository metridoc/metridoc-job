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
        gormService.enableFor(EzDoi, EzDoiJournal)
        def defaultParams = [
                ezproxyId: "bar",
                fileName: "foobar",
                lineNumber: 1,
                proxyDate: new Date(),
                proxyDay: 1,
                proxyMonth: 1,
                proxyYear: 2012,
                urlHost: "http://foo.com"
        ]
        defaultParams.ezDoiJournal = new EzDoiJournal(doi: "foo")
        EzDoi.withTransaction {
            defaultParams.ezDoiJournal.save(failOnError: true)
            new EzDoi(defaultParams).save(failOnError: true)
        }
        defaultParams.ezDoiJournal = new EzDoiJournal(doi: "bar")
        EzDoi.withTransaction {
            defaultParams.ezDoiJournal.save(failOnError: true)
            new EzDoi(defaultParams).save(failOnError: true)
        }

        expect:
        new EzDoi(
                ezDoiJournal: new EzDoiJournal(doi: "foo"),
                ezproxyId: "bar"
        ).alreadyExists()

        new EzDoi(
                ezDoiJournal: new EzDoiJournal(doi: "bar"),
                ezproxyId: "bar"
        ).alreadyExists()

        !new EzDoi(
                ezDoiJournal: new EzDoiJournal(doi: "foobar"),
                ezproxyId: "bar"
        ).alreadyExists()
    }

    void "acceptRecord sets EzDoiJournal on ezDoiJournal"() {
        given:
        def gormService = new GormService(embeddedDataSource: true)
        gormService.init()
        gormService.enableFor(EzDoi, EzDoiJournal)

        when:
        def ezDoi = new EzDoi()
        boolean accept
        def record = [ezproxyId: "foobar", urlHost: "www.foo.com", url: "http://www.foo.com/doi=10.123/"]
        EzDoi.withTransaction {
            accept = ezDoi.acceptRecord(record)
        }

        then:
        accept
        "10.123/" == record.ezDoiJournal.doi
    }
}
