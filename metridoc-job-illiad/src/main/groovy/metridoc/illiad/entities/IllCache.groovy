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
