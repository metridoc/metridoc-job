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

import com.sun.xml.internal.ws.util.StringUtils
import groovy.util.logging.Slf4j
import metridoc.utils.ApacheLogParser
import org.slf4j.LoggerFactory

import static metridoc.ezproxy.utils.TruncateUtils.truncateProperties

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
@Slf4j
abstract class EzproxyBase {
    public static final transient NATURAL_KEY_CACHE = "naturalKeyCache"
    Date proxyDate
    Integer proxyMonth
    Integer proxyYear
    Integer proxyDay
    String ezproxyId
    String fileName
    String urlHost
    Integer lineNumber
    boolean failedPopulate = false
    Set<String> naturalKeyCache = []

    static transients = ['naturalKeyCache', 'fieldsToLoad', 'failedPopulate']

    static constraints = {
        ezproxyId(maxSize: 50)
        urlHost(maxSize: 75)
    }

    static mapping = {
        fileName(index: "idx_file_name")
        ezproxyId(index: "idx_ezproxy_id")
        urlHost(index: "idx_url_host")
        version(false)
    }

    static runBaseConstraints(delegate, it) {
        runStaticClosure(constraints, delegate, it)
    }

    static runBaseMapping(delegate, it) {
        runStaticClosure(mapping, delegate, it)
    }

    static runStaticClosure(Closure closure, delegate, it) {
        def clone = closure.clone() as Closure
        clone.delegate = delegate
        clone.call(it)
    }

    boolean acceptRecord(Map body) {
        log.debug  "checking to accept record {}", body

        boolean result = body.ezproxyId &&
                body.urlHost

        if (!result) {
            log.debug "record {} was rejected", body
            log.debug "record was rejected: ezproxyId=${body.ezproxyId ?: 'NULL'}, urlHost=${body.urlHost ?: 'NULL'}"
            return false
        }

        truncateProperties(body, "ezproxyId", "fileName", "urlHost")
        addDateValues(body)

        log.debug "record {} was accepted", body
        return result
    }

    void populate(Map body) {
        log.debug "populating {}", body

        def dataOfInterest = body.findAll {
            String propertyName = StringUtils.capitalize(it.key)
            this.metaClass.respondsTo(this, "set${propertyName}", [it.value.getClass()] as Object[])
        }

        dataOfInterest.each {
            log.debug "updating property [$it.key] with [$it.value]"
            try {
                this."$it.key" = it.value
            }
            catch (Throwable throwable) {
                log.warn "Could not store ${it.key}", throwable
                failedPopulate = true
            }
        }
        log.debug "failedPopulate: ${failedPopulate?:'null'}"
        log.debug "finished populating {}", body
    }

    protected void addDateValues(Map record) {
        def proxyDate = record.proxyDate
        if (notNull(proxyDate)) {
            if (proxyDate instanceof String) {
                try {
                    //transform it to a date type
                    record.proxyDate = ApacheLogParser.parseLogDate(proxyDate)
                }
                catch (Throwable ignored) {
                    def log = LoggerFactory.getLogger(this.getClass())
                    log.warn("Could not parse date $proxyDate")
                    return
                }
            }

            def calendar = new GregorianCalendar()
            calendar.setTime(record.proxyDate as Date)
            record.proxyYear = calendar.get(Calendar.YEAR)
            record.proxyMonth = calendar.get(Calendar.MONTH) + 1
            record.proxyDay = calendar.get(Calendar.DAY_OF_MONTH)
        }
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected boolean notNull(item) {
        if (item == null) {
            return false
        }

        if (item instanceof String) {
            return item.trim()
        }
        return true
    }

    @Override
    boolean shouldSave() {
        if(failedPopulate) {
            log.debug "Populate failed: ${failedPopulate}"
            return false
        }
        String naturalKey = createNaturalKey()
        log.debug "Natural key is ${naturalKey}"
        if (naturalKeyCache.add(naturalKey)) {
            log.debug "Natural key added"
            def doesNotExist = !alreadyExists()
            if (doesNotExist) {
                log.debug "validating [{}]", this
                if (!this.validate()) {
                    log.debug "[$this] is invalid"
                    if (this.errors.fieldErrorCount) {
                        def message = """
error on field [${this.errors.fieldError.field}] with error code [${this.errors.fieldError.code}]
    file: $fileName,
    line: $lineNumber
"""
                        log.warn message
                        log.debug "[{}] will not be saved", naturalKey
                        return false
                    }
                    else {
                        throw new RuntimeException("unknown error occurred \n ${this.errors}")
                    }
                }
                log.debug "[{}] will be saved", naturalKey
            } else {
                log.debug "[{}] will not be saved", naturalKey
            }

            return doesNotExist
        }

        log.debug "[{}] is already in the cache, will not save", naturalKey
        return false
    }

    abstract String createNaturalKey()

    abstract boolean alreadyExists()
}
