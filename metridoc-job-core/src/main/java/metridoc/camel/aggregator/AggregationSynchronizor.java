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
import org.apache.camel.spi.Synchronization;

/**
 * @author tbarker
 * @deprecated we are no longer supporting routing features since straight groovy
 *             or camel can handle this just fine
 */
public class AggregationSynchronizor implements Synchronization {

    private boolean haveNotCalledCompletion = true;

    public void onComplete(Exchange exchange) {
        synchronize(exchange);
    }

    public void onFailure(Exchange exchange) {
        synchronize(exchange);
    }

    private void synchronize(Exchange exchange) {
        if (haveNotCalledCompletion) {
            String routeId = exchange.getFromRouteId();
            if (routeId != null) {
                exchange.getContext().getInflightRepository().remove(exchange, routeId);
            } else {
                exchange.getContext().getInflightRepository().remove(exchange);
            }
            haveNotCalledCompletion = false;
        }
    }

}
