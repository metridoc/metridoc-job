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

import groovy.util.logging.Slf4j
import metridoc.utils.DataSourceConfigUtil
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration

/**
 * @author Tommy Barker
 */
@Slf4j
class HibernateService extends DataSourceService {

    Properties hibernateProperties = [:]

    SessionFactory sessionFactory
    private List<Class> entityClasses = []

    @SuppressWarnings("GroovyVariableNotAssigned")
    SessionFactory createSessionFactory() {
        if (!hibernateProperties) {
            init()
        }
        def configuration = new Configuration()
        def result
        this.entityClasses.each {
            configuration.addAnnotatedClass(it)
        }
        configuration.addProperties(hibernateProperties)
        log.info "hibernate service is connecting to ${hibernateProperties.'hibernate.connection.url'}"
        result = configuration.buildSessionFactory()

        return result
    }

    @Override
    void init() {
        super.init()
        def hibernateProperties = convertDataSourcePropsToHibernate(config)
        this.hibernateProperties.putAll(hibernateProperties)
    }

    @Override
    void doEnableFor(Class... classes) {
        entityClasses = classes
        sessionFactory = createSessionFactory()
    }

    SessionFactory getSessionFactory() {
        if (sessionFactory) {
            return sessionFactory
        }

        sessionFactory = createSessionFactory()
    }

    @SuppressWarnings("GroovyMissingReturnStatement")
    Properties convertDataSourcePropsToHibernate(ConfigObject configObject) {
        DataSourceConfigUtil.getHibernateOnlyProperties(configObject, dataSourcePrefix)
    }
}
