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

import org.apache.camel.Endpoint
import org.apache.camel.Exchange
import org.apache.camel.impl.DefaultProducer

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 1/9/12
 * Time: 3:08 PM
 */
class SqlPlusProducer extends DefaultProducer {

    def processor

    SqlPlusProducer(Endpoint endpoint) {
        super(endpoint)
        processor = new SqlPlusProcessor(endpoint: endpoint)
    }

    void process(Exchange exchange) {
        processor.process(exchange)
    }
}
