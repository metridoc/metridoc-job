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

import org.apache.camel.CamelContext
import org.apache.camel.Endpoint
import org.apache.camel.impl.DefaultComponent
import org.apache.commons.lang.StringUtils

import javax.sql.DataSource

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 8/4/11
 * Time: 10:38 AM
 *
 * This will eventually be the groovy response based component that will replace the current sqlplus component.  The
 * goal is to remove the dependency on spring so we can reduce our dependency madness we are dealing with right now
 *
 */
class SqlPlusComponent extends DefaultComponent {

    static final String FETCH_SIZE_MIN = "min";
    static final int DEFAULT_FETCH_SIZE = 10;
    static final int DEFAULT_BATCH_SIZE = 50;

    SqlPlusComponent() {
    }

    SqlPlusComponent(CamelContext context) {
        super(context)
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) {
        int batchSize = getBatchSize(parameters)
        int fetchSize = getFetchSize(parameters)
        DataSource dataSource = getDataSource(parameters)

        String noDuplicateColumn = getAndRemoveParameter(parameters, "noDuplicateColumn", String)
        String[] columns = getColumns(parameters)

        return new SqlPlusEndpoint(uri, this, dataSource, batchSize, fetchSize, remaining, false, noDuplicateColumn,
                columns as Set<String>)
    }

    private String[] getColumns(Map<String, Object> parameters) {
        def rawColumns = getAndRemoveParameter(parameters, "columns", String)

        if (rawColumns) {
            String[] columns = rawColumns.split(",")

            (0..columns.size() - 1).each {
                columns[it] = StringUtils.trim(columns[it])
            }

            return columns
        }

        return null
    }

    private int getBatchSize(Map<String, Object> parameters) {
        return getAndRemoveParameter(parameters, "batchSize", Integer, DEFAULT_BATCH_SIZE)
    }

    private int getFetchSize(Map<String, Object> parameters) {
        String fetchSize = getAndRemoveParameter(parameters, "fetchSize", String, DEFAULT_FETCH_SIZE)
        int result
        if (FETCH_SIZE_MIN.equals(fetchSize)) {
            result = Integer.MIN_VALUE
        } else {
            result = Integer.valueOf(fetchSize)
        }

        return result
    }

    private DataSource getDataSource(Map<String, Object> parameters) {
        def reference = getDataSourceReference(parameters)
        def dataSource = getCamelContext().registry.lookupByNameAndType(reference, DataSource.class)
        assert dataSource: "a ${DataSource} could not be found for reference ${reference}"

        return dataSource
    }

    private String getDataSourceReference(Map<String, Object> parameters) {
        if (!parameters.containsKey("dataSource")) {
            throw new IllegalArgumentException("A DataSource must be specified for the ${SqlPlusComponent} to run")
        }

        return getAndRemoveParameter(parameters, "dataSource", String)
    }
}
