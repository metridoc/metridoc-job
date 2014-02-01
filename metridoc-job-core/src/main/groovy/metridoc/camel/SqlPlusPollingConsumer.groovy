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

import org.apache.camel.PollingConsumer
import org.apache.camel.Exchange
import org.apache.camel.Endpoint

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 9/7/12
 * Time: 12:53 PM
 * To change this template use File | Settings | File Templates.
 */
class SqlPlusPollingConsumer implements PollingConsumer {

    SqlPlusEndpoint endpoint

    @Override
    Exchange receive() {
        def query = endpoint.getTableQuery()
        def sql = new SqlUnManagedResultSet(endpoint.dataSource)
        def resultSet = sql.executeQuery(query)
        def exchange = endpoint.createExchange()
        exchange.in.body = resultSet
        exchange.in.setHeader("connection", sql.connection)
        exchange.addOnCompletion(sql)
        return exchange
    }

    @Override
    Exchange receiveNoWait() {
        receive()
    }

    @Override
    Exchange receive(long timeout) {
        receive()
    }

    @Override
    void start() {
        //do nothing
    }

    @Override
    void stop() {
        //do nothing
    }
}
