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



package metridoc.core.services

import org.hibernate.Session
import org.hibernate.SessionFactory

import javax.sql.DataSource
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Tommy Barker
 */
abstract class DataSourceService extends DefaultService {
    boolean embeddedDataSource
    boolean localMysql
    boolean localMySql
    ConfigObject config = new ConfigObject()
    String dataSourcePrefix = "dataSource"
    private boolean enableForRan = false

    protected static dataSourceHash = [:]

    static DataSource getDataSoruce(String dataSourceName) {
        def trimmedName = dataSourceName.trim()
        assert dataSourceHash[trimmedName] : "dataSource [${trimmedName}] does not exist"
        dataSourceHash[trimmedName]
    }

    void init() {
        def dataSourceInBinding = binding.variables.find {String key, value -> key.startsWith("dataSource") &&
                value instanceof DataSource}

        if(dataSourceInBinding) {
            binding.variables.each {String key, value ->
                if(key.startsWith("dataSource") && value instanceof DataSource) {
                    dataSourceHash[key] = value
                }
            }
            return
        }

        def dataSource = config."$dataSourcePrefix"
        if (embeddedDataSource) {
            dataSource.url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000"
            dataSource.username = "SA"
            dataSource.password = ""
            dataSource.driverClassName = "org.h2.Driver"
            dataSource.dialect = "org.hibernate.dialect.H2Dialect"
        }

        if (localMysql || localMySql) {
            dataSource.url = "jdbc:mysql://localhost:3306/test"
            dataSource.username = "root"
            if (!dataSource.password) {
                dataSource.password = ""
            }
            dataSource.driverClassName = "com.mysql.jdbc.Driver"
            dataSource.dialect = "org.hibernate.dialect.MySQL5InnoDBDialect"
        }
    }

    synchronized void enableFor(Class... entities) {
        if(!enableForRan) {
            doEnableFor(entities)
            enableForRan = true
        }
        else {
            throw new IllegalStateException("Could not run [enableFor] since it has already ran")
        }
    }

    static void withTransaction(Session session, Closure closure) {
        def transaction = session.beginTransaction()
        try {
            closure.call(session)
            transaction.commit()
        }
        catch (Exception e) {
            transaction.rollback()
            throw e
        }
    }

    void withTransaction(Closure closure) {
        assert sessionFactory : "session factory has not been set yet"
        def session = sessionFactory.currentSession
        withTransaction(session, closure)
    }

    abstract protected void doEnableFor(Class... classes)
    abstract SessionFactory getSessionFactory()
}
