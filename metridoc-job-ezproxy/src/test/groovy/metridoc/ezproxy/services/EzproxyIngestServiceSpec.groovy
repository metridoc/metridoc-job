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

package metridoc.ezproxy.services

import metridoc.ezproxy.entities.EzHosts
import metridoc.ezproxy.entities.EzproxyBase
import metridoc.service.gorm.GormService
import org.apache.commons.lang.text.StrBuilder
import spock.lang.Specification

/**
 * @author Tommy Barker
 */
class EzproxyIngestServiceSpec extends Specification {

    void "if the gormService has already been setup, checkConnection should not fail"() {
        given:
        GormService gormService = new GormService(embeddedDataSource: true)
        gormService.init()
        gormService.enableFor(EzHosts)
        def ingestService = new EzproxyIngestService(entityClass: EzHosts)
        Binding binding = ingestService.binding
        binding.gormService = gormService

        when:
        ingestService.checkConnection()

        then:
        noExceptionThrown()
    }

    void "test mock ingestion"() {
        given:
        def iterator = new EzproxyIteratorService(ezproxyId: 0, url: 1, proxyDate: 2, delimiter: ",")
        StrBuilder builder = new StrBuilder()
        builder.appendln("1,http://foo.com,[31/Dec/2010:00:00:14 -0500]")
        builder.appendln("1,http://foo.com,[31/Dec/2010:00:00:14 -0500]")
        builder.appendln("3,http://foo.com,[31/Dec/2010:00:00:14 -0500]")
        builder.appendln("3,http://bar.com,[31/Dec/2010:00:00:14 -0500]")
        builder.appendln("1")
        iterator.inputStream = new ByteArrayInputStream(builder.toString().bytes)

        when:
        def (Map stats, List entitiesSaved) = EzproxyIngestService.doIngest(iterator, MockBase)

        then:
        2 == stats.written
        3 == stats.ignored
        entitiesSaved.find {MockBase base -> base.urlHost.contains("foo") && base.ezproxyId == "1"}
        entitiesSaved.find {MockBase base -> base.urlHost.contains("foo") && base.ezproxyId == "3"}
    }
}

class MockBase extends EzproxyBase {

    static withTransaction(Closure closure) {
        closure.call()
    }

    boolean save(Map options) {
        return true
    }

    @Override
    String createNaturalKey() {
        return ezproxyId + urlHost
    }

    @Override
    boolean alreadyExists() {
        if(urlHost.contains("bar")) return true


        return false
    }

    boolean validate() {
        return true
    }
}
