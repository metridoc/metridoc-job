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

import groovy.sql.Sql
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.impl.DefaultConsumer
import org.apache.camel.spi.Synchronization

import javax.sql.DataSource
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

import metridoc.sql.SqlPlus

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 8/4/11
 * Time: 3:40 PM
 */
class SqlPlusConsumer extends DefaultConsumer {

    SqlPlus sql
    Boolean release = false
    ResultSet resultSet
    SqlUnManagedResultSet sqlUnManaged

    SqlPlusConsumer(SqlPlusEndpoint endpoint, Processor processor) {
        super(endpoint, processor)
    }

    SqlUnManagedResultSet getSqlUnManaged() {

        if (sqlUnManaged) {
            return sqlUnManaged
        }

        sqlUnManaged = new SqlUnManagedResultSet(getDataSource())
        sqlUnManaged.withStatement {
            it.setFetchSize(fetchSize)
        }

        return sqlUnManaged
    }

    String getTableQuery() {
        endpoint.tableQuery
    }

    @Override
    protected void doStart() {
        super.doStart()
        def sql = getSqlUnManaged()
        String command = getTableQuery()
        def exchange = endpoint.createExchange()
        sql.query(command) {ResultSet resultSet ->
            exchange.in.setBody(resultSet)
        }
        def processor = getProcessor()
        exchange.addOnCompletion(sql)
        processor.process(exchange)
    }

    @Override
    protected void doStop() {
        getSqlUnManaged().close()
    }

    SqlPlus getSql() {
        if (sql) {
            return sql
        }

        sql = new SqlPlus(getDataSource())
        sql.withStatement {Statement statement ->
            statement.setFetchSize(getFetchSize())
        }

        return sql
    }

    DataSource getDataSource() {
        return endpoint.dataSource
    }

    int getFetchSize() {
        return endpoint.fetchSize
    }

    int getBatchSize() {
        return endpoint.batchSize
    }

    boolean getLogBatches() {
        return endpoint.logBatches
    }

    String getQuery() {
        return endpoint.query
    }
}

class SqlUnManagedResultSet extends Sql implements Synchronization {

    Connection connection
    Statement statement
    ResultSet results

    SqlUnManagedResultSet(DataSource dataSource) {
        super(dataSource)
    }

    /**
     * overrides the default implementation so that the {@link ResultSet} does not get closed too early
     * @param connection
     * @param statement
     * @param results
     */
    @Override
    protected void closeResources(Connection connection, Statement statement, ResultSet results) {
        this.connection = connection
        this.statement = statement
        this.results = results
    }

    @Override
    protected void closeResources(Connection connection, Statement statement) {
        this.connection = connection
        this.statement = statement
    }

    @Override
    void close() {
        if(results) {
            super.closeResources(connection, statement, results)
        } else {
            super.closeResources(connection, statement)
        }
        super.close()
    }

    @Override
    void onComplete(Exchange exchange) {
        closeResource(results)
        closeResource(statement)
        closeResource(connection)
    }

    private void closeResource(resource) {
        try {
            resource.close()
        } catch (Exception e) {

        }
    }

    @Override
    void onFailure(Exchange exchange) {

    }
}
