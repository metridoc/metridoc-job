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



package metridoc.camel

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder

/**
 * @deprecated use camel-groovy component instead
 */
abstract class ManagedExceptionRouteBuilder extends RouteBuilder {

    Throwable routeException
    Closure exceptionHandler

    abstract void doConfigure()

    @Override
    void configure() {

        use(CamelExtensions) {

            onException(Throwable.class).process { Exchange exchange ->
                def exception = exchange.getException()
                if (exception) {
                    handleException(exception, exchange)
                } else {
                    exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT)
                    if (exception) {
                        handleException(exception, exchange)
                    } else {
                        exception = new RuntimeException("An unknown exception occurred, if the log is not " +
                                "sufficient to determine what happened consider setting logging level to DEBUG")
                        handleException(exception, exchange)
                    }
                }
            }

            doConfigure()
        }
    }

    Throwable getFirstException() {
        return routeException
    }

    void handleException(Throwable throwable, Exchange exchange) {
        if (exceptionHandler) {
            exceptionHandler.call(throwable, exchange)
        } else {

            if (routeException) {
                log.error("an additional exception occurred", throwable)
                return //only catch and throw the first one
            }

            routeException = throwable
        }
    }
}
