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

import groovy.util.logging.Slf4j
import metridoc.core.InjectArgBase
import metridoc.core.Step
import metridoc.ezproxy.entities.EzDoi
import metridoc.ezproxy.entities.EzDoiJournal
import metridoc.ezproxy.utils.TruncateUtils

/**
 * Created with IntelliJ IDEA on 9/24/13
 * @author Tommy Barker
 */
@InjectArgBase("ezproxy")
@Slf4j
class ResolveDoisService {

    int doiResolutionCount = 2000
    boolean stacktrace = false
    boolean use4byte = false
    String fourByteReplacement = "_?_"
    CrossRefService crossRefService

    @Step(description = "resolves dois against crossref")
    void resolveDois() {
        EzDoiJournal.withTransaction {

            def stats = [
                    processed: 0,
                    unresolvable: 0,
                    total: 0
            ]

            List ezDoiJournals = EzDoiJournal.findAllByProcessedDoi(false, [max: doiResolutionCount])

            if (ezDoiJournals) {
                log.info "processing a batch of [${ezDoiJournals.size()}] dois"
            } else {
                log.info "there are no dois to process"
                return
            }

            ezDoiJournals.each { EzDoiJournal ezDoiJournal ->
                def response = crossRefService.resolveDoi(ezDoiJournal.doi)
                try {
                    stats = processResponse(response, ezDoiJournal, stats)
                }
                catch (Throwable throwable) {
                    log.error """
                        Could not save doi info for file: $ezDoiJournal.fileName at line: $ezDoiJournal.lineNumber

                        Response from CrossRef:
                        $response

                        error will be thrown now to stop ingestion
                    """
                    throw throwable
                }

                ezDoiJournal.save(failOnError: true)
            }
        }
    }

    protected Map processResponse(CrossRefResponse response, EzDoiJournal ezDoiJournal, Map stats) {
        assert !response.loginFailure: "Could not login into cross ref"
        if (response.malformedDoi || response.unresolved) {
            stats.unresolvable += 1
            log.debug "Could not resolve doi $ezDoiJournal.doi, it was either malformed or unresolvable"
        } else if (response.statusException) {
            String message = "An exception occurred trying to resolve doi [$ezDoiJournal.doi]"
            logWarning(message, response.statusException)
            stats.unresolvable += 1
        } else {
            ingestResponse(ezDoiJournal, response)
            stats.processed += 1
        }
        ezDoiJournal.processedDoi = true

        if (stats.total % 100 == 0) {
            log.info "Record stats: [$stats]"
        }

        stats.total += 1
        return stats
    }

    protected void logWarning(String message, Exception statusException) {
        if (stacktrace) {
            log.warn message, statusException
        } else {
            log.warn "{}: {}", message, statusException.message
        }
    }

    protected void ingestResponse(EzDoiJournal ezDoiJournal, CrossRefResponse crossRefResponse) {
        crossRefResponse.properties.each { key, value ->
            if (key != "loginFailure"
                    && key != "class"
                    && key != "status"
                    && key != "malformedDoi"
                    && key != "statusException"
                    && key != "unresolved") {

                def chosenValue = crossRefResponse."$key"
                if (chosenValue instanceof String) {
                    chosenValue = TruncateUtils.truncate(chosenValue, TruncateUtils.DEFAULT_VARCHAR_LENGTH)
                    if (key == "articleTitle" || key == "journalTitle") {
                        chosenValue = use4byte ? chosenValue : convertToBMP(chosenValue as String)
                    }
                }

                ezDoiJournal."$key" = chosenValue
            }
        }
        ezDoiJournal.resolvableDoi = true
    }

    /**
     * got this idea from
     * http://stackoverflow.com/questions/14981109/checking-utf-8-data-type-3-byte-or-4-byte-unicode/14983652#14983652.
     * Out of the box utf8 in mysql is only BMP, all 4byte characters, like japanese / chinese characters and math
     * symbols are not supported.
     *
     * @param text
     */
    String convertToBMP(String text) {
        def response = text.replaceAll("[\\ud800-\\udfff]", fourByteReplacement)
        if (response.contains("_?_")) {
            log.warn "text [$text] contains unsupportted characters"
        }

        return response
    }
}
