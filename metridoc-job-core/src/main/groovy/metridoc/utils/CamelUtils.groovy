/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */


package metridoc.utils

import org.apache.camel.CamelContext
import org.apache.camel.Consumer
import org.apache.camel.Endpoint
import org.apache.camel.Route
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.spi.BrowsableEndpoint
import org.apache.camel.spi.ShutdownAware
import org.apache.camel.util.StopWatch
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 *
 * Helpful utilities for Apache Camel
 *
 * @deprecated this hooks into pretty low level stuff and has become hard
 * to maintain with new versions of camel.  Since we are rarely doing
 * routing anymore, it is time to deprecate this
 *
 */
class CamelUtils {
    static final log = LoggerFactory.getLogger(CamelUtils.class)
    private static final USE_POLLING_TEXT = "usePolling=(true|false)"
    private static final USE_POLLING = /${USE_POLLING_TEXT}/
    private static long TWELVE_SECONDS = TimeUnit.SECONDS.toMillis(12)

    /**
     *
     * waits till the specified time limit for messages to complete
     *
     * @param camelContext
     * @param timeout
     * @param timeUnit
     * @return if it completed in the alotted time
     *
     * @throws TimeoutException thrown when messages not completed by the specified time
     */
    static boolean waitTillDone(CamelContext camelContext, long timeout, TimeUnit timeUnit) throws Exception {

        log.info("waiting for all exchanges in {} to complete", camelContext)
        boolean notDone = true
        StopWatch stopWatch = new StopWatch()

        long nextUpdate = TWELVE_SECONDS

        //put everything into something that implements Runnable or Callable
        Closure work = {

            while (notDone) {
                int size = 0
                for (Route route : camelContext.routes) {
                    Consumer consumer = route.consumer

                    if (consumer instanceof ShutdownAware) {
                        def shutdownAware = consumer as ShutdownAware
                        size = shutdownAware.pendingExchangesSize
                        log.debug("consumer {} has {} in memory exchanges pending", consumer, size)
                    }

                    if (checkIfNotDone(size)) break
                    size = camelContext.inflightRepository.size(route.id)

                    log.debug("inflight repository found {} exchanges for consumer {}", size, consumer)
                    if (checkIfNotDone(size)) break
                }

                if (size == 0) {
                    def endpoints = camelContext.endpoints
                    for (Endpoint endpoint : endpoints) {
                        def notAMockEndpoint = !(endpoint instanceof MockEndpoint)
                        def isBrowsableEndpoint = endpoint instanceof BrowsableEndpoint
                        def shouldCheck = isBrowsableEndpoint && notAMockEndpoint
                        if (shouldCheck) {
                            def browsable = endpoint as BrowsableEndpoint
                            size = browsable.exchanges.size()
                            log.debug("seda endpoint {} has {} exchanges pending", endpoint, size)

                            if (checkIfNotDone(size)) break
                        }
                    }
                }
                int inflightSize = camelContext.inflightRepository.size()
                notDone = size > 0 || inflightSize
                nextUpdate = logProgress(stopWatch, nextUpdate, camelContext)
            }
        }

        def executor =
            camelContext.getExecutorServiceManager().newSingleThreadExecutor(CamelUtils.class, "WaitForCamelToFinish")
        def future = executor.submit(work)

        boolean finished = true
        try {
            future.get(timeout, timeUnit)
        } catch (TimeoutException e) {
            finished = false
        }

        stopWatch.stop()
        long taken = stopWatch.taken()
        long seconds = TimeUnit.MILLISECONDS.toSeconds(taken)
        long minutes = TimeUnit.MILLISECONDS.toMinutes(taken)

        def template = { unit, type -> "${camelContext} took ${unit} ${type} to process messages" }

        if (minutes) {
            log.info(template(minutes, "minutes"))
        } else if (seconds) {
            log.info(template(seconds, "seconds"))
        } else {
            log.info(template(taken, "milliseconds"))
        }

        if (!finished) {
            log.warn("the CamelContext {} is not done", camelContext)
        }

        return finished
    }

    private static boolean checkIfNotDone(int size) {
        if (size > 0) {
            Thread.sleep(300)
            return true
        }

        return false
    }

    private static long logProgress(StopWatch stopWatch, long nextUpdate, CamelContext camelContext) {
        long taken = stopWatch.taken()
        if (taken > nextUpdate) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(stopWatch.taken())
            if (minutes > 0) {
                log.debug("waited {} minutes, and {} has not completed", minutes, camelContext)
                return nextUpdate + TimeUnit.MINUTES.toMillis(1)
            } else {
                long secondsTaken = TimeUnit.MILLISECONDS.toSeconds(taken)
                log.debug("waited {} seconds, and {} has not completed", secondsTaken, camelContext)
                return nextUpdate + TWELVE_SECONDS
            }
        }

        return nextUpdate
    }

    /**
     * waits indefinitely for all messages to complete
     *
     * @param camelContext
     */
    static void waitTillDone(CamelContext camelContext) throws Exception {
        long timeOut = Long.MAX_VALUE
        TimeUnit timeOutUnit = TimeUnit.SECONDS
        waitTillDone(camelContext, timeOut, timeOutUnit)
    }

    /**
     * converts an endpoint to a polling uri; if a <code>usePolling</code> parameter is <code>false</code> in the
     * endpoint, then the <code>usePolling</code> parameter is removed and the original endpoint is passed.
     * <code>seda</code> endpoints are not polling by default.  <code>direct</code> can never be a polling endpoint
     * and will throw an {@link IllegalArgumentException} if it uses <code>usePolling</code> specified to true.
     *
     * <br/>
     * <br/>
     * example: CamelUtils
     * <br/>
     * <code>CamelUtils.convertToPollingEndpoint(
     *
     *
     *
     * @param uri
     * @throws IllegalArgumentException if endpoint is <code>direct</code> and has the parameter
     *  <code>usePolling</code> set to <code>true</code>
     * @return
     */
    static String convertToPollingEndpoint(String uri) {
        def newUri = uri

        if (usePolling(uri)) {
            boolean notDirect = !uri.startsWith("direct")
            assert notDirect: "Polling cannot be used on a direct endpoint"
            newUri = "poll:${newUri}"
        } else {
            newUri = uri.replaceAll(USE_POLLING_TEXT, StringUtils.EMPTY)
            newUri = removeUnWantedCharactersIfAtEnd("?", newUri)
            newUri = removeExtraneousAmpersands(newUri)
        }

        return newUri
    }

    private static String removeExtraneousAmpersands(String uriToFormat) {
        def result = uriToFormat
        result = result.replace("?&", "?")
        result = result.replace("&&", "&")
        result = removeUnWantedCharactersIfAtEnd("&", result)

        return result
    }

    private static String removeUnWantedCharactersIfAtEnd(String characters, String unFormattedText) {
        if (unFormattedText.endsWith(characters)) {
            return unFormattedText.substring(0, unFormattedText.size() - characters.size())
        }

        return unFormattedText
    }

    private static boolean usePolling(String uri) {

        def m = uri =~ USE_POLLING
        def boolean usePollingInUri

        //uri based
        if (m.find()) {
            return Boolean.valueOf(m[0][1] as String)
        }

        //defaults
        return !(uri.startsWith("seda") || uri.startsWith("direct"))
    }
}


