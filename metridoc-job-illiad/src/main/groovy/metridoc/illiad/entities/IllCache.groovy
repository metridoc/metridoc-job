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
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
@Entity
@Slf4j
class IllCache {
    String jsonData
    Date lastUpdated
    Date dateCreated

    static constraints = {
        jsonData(maxSize: Integer.MAX_VALUE)
    }

    static void update(String jsonData) {
        withNewTransaction {
            if (count()) {
                def illCache = list().get(0)
                illCache.jsonData = jsonData
                illCache.save(failOnError: true)
            } else {
                new IllCache(jsonData: jsonData).save(failOnError: true)
            }
        }
    }

    static void update(Map data) {
        update(marshal(data))
    }

    static String marshal(Map data) {
        def builder = new JsonBuilder()
        builder.call(data)
        def result = builder.toPrettyString()
        log.info "storing cached reporting data in json format: ${result}"

        return result
    }

    static getData() {
        if (count() == 0) return null

        def cache = list().get(0)
        def slurper = new JsonSlurper()
        def data = slurper.parseText(cache.jsonData)

        if (cache.lastUpdated) {
            data.lastUpdated = cache.lastUpdated
        } else {
            data.lastUpdated = cache.dateCreated
        }

        return data
    }
}
