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

import metridoc.camel.aggregator.BodyAggregator
import metridoc.camel.aggregator.InflightAggregationWrapper
import metridoc.utils.CamelUtils
import org.apache.camel.EndpointInject
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.test.junit4.CamelTestSupport
import org.apache.commons.lang.ObjectUtils
import org.junit.Test

import java.util.concurrent.CountDownLatch

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 12/27/12
 * Time: 1:50 PM
 * To change this template use File | Settings | File Templates.
 */
class InflightExperimentsTests extends CamelTestSupport {

    @EndpointInject(uri = "mock:inflightTest")
    MockEndpoint mockEndpoint
    def enteredProcessor = new CountDownLatch(1)
    def exitingProcessor = new CountDownLatch(1)

    @Test
    void "test origin"() {
        mockEndpoint.reset()
        mockEndpoint.setExpectedMessageCount(1)
        template.requestBody("direct:inflightTest", ObjectUtils.NULL)
        assert context.inflightRepository.size("foo") == 1
        template.requestBody("direct:inflightTest", ObjectUtils.NULL)
        template.asyncRequestBody("direct:inflightTest", ObjectUtils.NULL)
        enteredProcessor.await()
        //it should be two since the the wrapper creates work and the aggregator creates work
        assert context.inflightRepository.size("foo") == 2
        exitingProcessor.countDown()
        mockEndpoint.assertIsSatisfied()
        CamelUtils.waitTillDone(context)
        assert context.inflightRepository.size("foo") == 0
        assert mockEndpoint.getExchanges().get(0).getFromEndpoint().getEndpointUri().contains("inflightTest")
        assert "foo" == mockEndpoint.getExchanges().get(0).getFromRouteId()
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        def final processor = new Processor() {
            void process(Exchange exchange) throws Exception {
                enteredProcessor.countDown()
                exitingProcessor.await()
            }
        }

        return new RouteBuilder() {

            @Override
            void configure() throws Exception {
                def aggregator = new InflightAggregationWrapper(new BodyAggregator())
                from("direct:inflightTest").routeId("foo").aggregate(constant("continue"), aggregator).
                        completionSize(3).process(processor).to("mock:inflightTest")
            }
        }
    }
}
