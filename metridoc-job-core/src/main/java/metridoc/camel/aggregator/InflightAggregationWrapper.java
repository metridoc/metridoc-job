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

package metridoc.camel.aggregator;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.spi.Synchronization;

/**
 * @author tbarker
 * @deprecated we are no longer supporting routing features since straight groovy
 *             or camel can handle this just fine
 */
public class InflightAggregationWrapper implements AggregationStrategy {

    private Exchange currentExchange;
    private AggregationStrategy wrappedStrategy;
    private static final String METRIDOC_SYNCHRONIZOR = "MetridocSynchronizor";


    public InflightAggregationWrapper(AggregationStrategy wrappedStrategy) {
        this.wrappedStrategy = wrappedStrategy;
    }

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        Exchange aggExchange = wrappedStrategy.aggregate(oldExchange, newExchange);
        if (aggExchange != null) {
            if (aggExchange != currentExchange) {
                String routeId = aggExchange.getFromRouteId();
                if (routeId != null) {
                    aggExchange.getContext().getInflightRepository().add(aggExchange, routeId);
                } else {
                    aggExchange.getContext().getInflightRepository().add(aggExchange);
                }
                Synchronization synchronizor;
                if (currentExchange != null) {
                    synchronizor = currentExchange.getProperty(METRIDOC_SYNCHRONIZOR, Synchronization.class);
                    synchronizor.onComplete(currentExchange);
                }
                currentExchange = aggExchange;
                synchronizor = new AggregationSynchronizor();
                currentExchange.setProperty(METRIDOC_SYNCHRONIZOR, synchronizor);
                currentExchange.addOnCompletion(synchronizor);
            }
        }

        return aggExchange;
    }
}
