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

import metridoc.utils.CamelUtils
import org.apache.camel.Component
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.impl.SimpleRegistry
import org.apache.camel.spi.Registry
import org.apache.commons.lang.ObjectUtils
import org.slf4j.LoggerFactory

/**
 * Never found this class particularily useful.  Going to move to camel-glite
 *
 * @deprecated
 */
class CamelScript {

    /**
     * components that should be added when instantiating the camel context
     */
    static Map<String, Class<? extends Component>> components = Collections.synchronizedMap([:])
    static final log = LoggerFactory.getLogger(CamelScript)

    /**
     * runs a route constructed from the passed closure.  A camel {@link Registry} will be built upon the properties
     * provided by the closure's owner and delegate.  If a direct endpoint with the name <code>direct:start</code>
     * exists, it will be called after the route is loaded.
     *
     * @param closure
     * @return
     */
    static RouteBuilder runRoute(Closure closure) {
        runRoute(null, closure)
    }

    /**
     *
     *
     * @param start
     * @param closure
     * @return
     */
    static RouteBuilder runRoute(String start, Closure closure) {
        def delegateOverride = closure.delegate
        def routeBuilder = new GroovyRouteBuilder(route: closure)
        def registry = new CamelScriptRegistry(closure: closure, delegateOverride: delegateOverride)
        runRouteBuilders(start, registry, routeBuilder)

        return routeBuilder
    }

    static void runRouteBuilders(Map<String, Object> registry, RouteBuilder... builders) {
        runRouteBuilders(null, registry, builders)
    }

    static void runRouteBuilders(String start, Map<String, Object> registry, RouteBuilder... builders) {
        def simpleRegistry = new SimpleRegistry()
        simpleRegistry.putAll(registry)
        runRouteBuilders(start, simpleRegistry, builders)
    }

    static void runRouteBuilders(Registry registry, RouteBuilder... builders) {
        runRouteBuilders(null, registry, builders)
    }

    static void runRouteBuilders(String start, Registry registry, RouteBuilder... builders) {
        def camelContext = new DefaultCamelContext(registry)
        camelContext.disableJMX()
        try {
            components.each {
                camelContext.addComponent(it.key, it.value.newInstance(camelContext))
            }
            builders.each {
                camelContext.addRoutes(it)
            }
            camelContext.start()
            if (start) {
                camelContext.createProducerTemplate().requestBody(start, ObjectUtils.NULL)
            } else {
                def callStart = false
                camelContext.routes.each {
                    def consumer = it.consumer
                    def uri = consumer.endpoint.endpointUri
                    if ("direct://start" == uri) {
                        callStart = true
                    }
                }
                if (callStart) {
                    camelContext.createProducerTemplate().requestBody("direct://start", ObjectUtils.NULL)
                }
            }
            CamelUtils.waitTillDone(camelContext)
            builders.each {
                if (it instanceof ManagedExceptionRouteBuilder) {
                    if (it.firstException) {
                        throw it.firstException
                    }
                }
            }
        } finally {
            try {
                camelContext.shutdown()
            } catch (Exception e) {
                log.warn("Unexpected exception occurred while shutting down camel", e)
            }
        }

    }
}
