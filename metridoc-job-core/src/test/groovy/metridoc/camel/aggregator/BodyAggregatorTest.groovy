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


package metridoc.camel.aggregator

import metridoc.utils.CamelUtils
import org.apache.camel.CamelExecutionException
import org.apache.camel.EndpointInject
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultExchange
import org.apache.camel.model.RouteDefinition
import org.apache.camel.model.RoutesDefinition
import org.apache.camel.test.junit4.CamelTestSupport
import org.apache.commons.lang.ObjectUtils
import org.junit.Test

import java.util.concurrent.TimeUnit

/**
 *
 * @author Thomas Barker
 */
public class BodyAggregatorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:end")
    private MockEndpoint mockEnd;

    @Test
    public void testAggregate() {
        DefaultExchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody("line1");
        BodyAggregator aggregate = new BodyAggregator();
        Exchange aggExchange = aggregate.aggregate(null, exchange);
        assertEquals(ArrayList.class, aggExchange.getIn().getBody().getClass());
        assertEquals(1, aggExchange.getIn().getBody(List.class).size());

        exchange = new DefaultExchange(context);
        exchange.getIn().setBody("line2");
        aggExchange = aggregate.aggregate(aggExchange, exchange);
        assertEquals(2, aggExchange.getIn().getBody(List.class).size());
    }

    @Test(timeout = 10000L)
    public void shutDownShouldDefer() throws Exception {
        context.addRoutes(aggRoute);

        template.sendBody("direct:start", ObjectUtils.NULL);
        CamelUtils.waitTillDone(context);

        mockEnd.expectedMessageCount(1);
        mockEnd.assertIsSatisfied();
    }

    @Test(timeout = 10000L)
    public void canShutDownDuringException() throws Exception {
        context.addRoutes(aggRoute);
        try {
            template.sendBody("direct:exception", ObjectUtils.NULL);
        } catch (CamelExecutionException camelExecutionException) {
            //do nothing
        }
        RoutesDefinition routes = aggRoute.getRouteCollection();
        for (RouteDefinition route : routes.getRoutes()) {
            context.stopRoute(route.getId(), 2, TimeUnit.SECONDS, true);
        }
    }

    RouteBuilder aggRoute = new RouteBuilder() {

        @Override
        public void configure() throws Exception {

            errorHandler(noErrorHandler());

            Processor processor = new Processor() {

                public void process(Exchange exchange) throws Exception {
                    throw new RuntimeException("meant to do that");
                }
            };

            from("direct:start").aggregate(constant(true), new InflightAggregationWrapper(new BodyAggregator()))
                    .completionTimeout(1000).to("mock:end");
            from("direct:exception").aggregate(constant(true), new InflightAggregationWrapper(new BodyAggregator()))
                    .completionTimeout(1000).process(processor);
        }
    };
}
