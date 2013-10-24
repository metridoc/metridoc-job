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

import org.apache.camel.Component
import org.apache.camel.Consumer
import org.apache.camel.Processor
import org.apache.camel.Producer
import org.apache.camel.impl.DefaultEndpoint
import org.apache.camel.util.UnsafeUriCharactersEncoder

import javax.sql.DataSource
import org.apache.camel.PollingConsumer

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 8/4/11
 * Time: 1:39 PM
 */
class SqlPlusEndpoint extends DefaultEndpoint {
    DataSource dataSource
    int batchSize
    int fetchSize
    String query
    String noDuplicateColumn
    Set<String> columns

    /**
     * whether we should log the batches at info level (true) or debug level (false) default is false
     */
    boolean logBatches = false

    SqlPlusEndpoint() {
    }

    SqlPlusEndpoint(String endpointUri, Component component, DataSource dataSource, int batchSize, int fetchSize,
                    String query, boolean logBatches, String noDuplicateColumn, Set<String> columns) {

        super(endpointUri, component)
        this.dataSource = dataSource
        this.batchSize = batchSize
        this.fetchSize = fetchSize
        this.query = query
        this.logBatches = logBatches
        this.noDuplicateColumn = noDuplicateColumn
        this.columns = columns
    }

    Producer createProducer() {
        return new SqlPlusProducer(this)
    }

    Consumer createConsumer(Processor processor) {
        return new SqlPlusConsumer(this, processor)
    }

    boolean isSingleton() {
        return true
    }

    @Override
    protected String createEndpointUri() {
        return "sqlplus:" + UnsafeUriCharactersEncoder.encode(query);
    }

    @Override
    PollingConsumer createPollingConsumer() {
        return new SqlPlusPollingConsumer(endpoint: this)
    }

    String getTableQuery() {
        def result = query
        if (!result.startsWith("select")) {
            result = "select * from ${query}"
        }

        return result
    }
}
