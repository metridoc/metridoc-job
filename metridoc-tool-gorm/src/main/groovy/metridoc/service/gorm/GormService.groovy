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



package metridoc.service.gorm

import groovy.util.logging.Slf4j
import metridoc.core.InjectArg
import metridoc.core.services.DataSourceService
import metridoc.iterators.Iterators
import metridoc.tool.gorm.GormIteratorWriter
import metridoc.tool.gorm.HibernateDatastoreFactoryBean
import metridoc.utils.DataSourceConfigUtil
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean
import org.codehaus.groovy.grails.orm.hibernate.EventTriggeringInterceptor
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateTransactionManager
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration
import org.codehaus.groovy.grails.orm.hibernate.cfg.HibernateUtils
import org.codehaus.groovy.grails.orm.hibernate.events.PatchedDefaultFlushEventListener
import org.codehaus.groovy.grails.orm.hibernate.events.SaveOrUpdateEventListener
import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor
import org.codehaus.groovy.grails.orm.hibernate.support.SpringLobHandlerDetectorFactoryBean
import org.codehaus.groovy.grails.orm.hibernate.validation.PersistentConstraintFactory
import org.codehaus.groovy.grails.orm.hibernate.validation.UniqueConstraint
import org.codehaus.groovy.grails.orm.support.GrailsTransactionTemplate
import org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.MappingContext
import org.hibernate.EmptyInterceptor
import org.hibernate.SessionFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.jdbc.support.lob.LobHandler
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.util.Assert

import javax.sql.DataSource

/**
 * @author Tommy Barker
 */
@Slf4j
class GormService extends DataSourceService {
    List<Class> entities = []

    static {
        Iterators.WRITERS << ["gorm": GormIteratorWriter]
    }

    @InjectArg(ignore = true)
    def listensers = [
            flush: new PatchedDefaultFlushEventListener(),
            "pre-load": eventTriggeringInterceptor,
            "post-load": eventTriggeringInterceptor,
            "save": eventTriggeringInterceptor,
            "save-update": eventTriggeringInterceptor,
            "pre-insert": eventTriggeringInterceptor,
            "post-insert": eventTriggeringInterceptor,
            "pre-update": eventTriggeringInterceptor,
            "post-update": eventTriggeringInterceptor,
            "pre-delete": eventTriggeringInterceptor,
            "post-delete": eventTriggeringInterceptor,
    ]

    @InjectArg(ignore = true)
    @Lazy(soft = true)
    ApplicationEventMulticaster applicationEventMulticaster = {
        return new SimpleApplicationEventMulticaster()
    }()

    @InjectArg(ignore = true)
    @Lazy(soft = true)
    SaveOrUpdateEventListener eventTriggeringInterceptor = {
        new ClosureEventTriggeringInterceptor(applicationContext: applicationContext)
    }()

    @InjectArg(ignore = true)
    @Lazy(soft = true)
    Datastore hibernateDatastore = {
        new HibernateDatastoreFactoryBean(
                sessionFactory: sessionFactory,
                grailsApplication: grailsApplication,
                mappingContext: grailsDomainClassMappingContext,
                interceptor: eventTriggeringInterceptor,
                applicationContext: applicationContext
        ).getObject()
    }()

    @InjectArg(ignore = true)
    @Lazy(soft = true)
    DataSource dataSource = {
        this.init()
        if (dataSourceHash[dataSourcePrefix]) return dataSourceHash[dataSourcePrefix]

        assert applicationContext.containsBean(dataSourcePrefix): "There is no dataSource configured"
        applicationContext.getBean(dataSourcePrefix)
    }()


    @InjectArg(ignore = true)
    @Lazy(soft = true)
    GrailsApplication grailsApplication = {
        def grailsApplication = new DefaultGrailsApplication(entities as Class[], entities[0].classLoader)
        grailsApplication.mainContext = applicationContext
        grailsApplication.initialise()

        return grailsApplication
    }()

    @InjectArg(ignore = true)
    @Lazy(soft = true)
    SessionFactory sessionFactory = {
        assert entities: "[SessionFactory] cannot be retrieved until [enableFor] is called for one or more entities"
        def configurableLocalSessionFactoryBean = new ConfigurableLocalSessionFactoryBean()
        configurableLocalSessionFactoryBean.dataSource = dataSource
        configurableLocalSessionFactoryBean.grailsApplication = grailsApplication
        configurableLocalSessionFactoryBean.configClass = GrailsAnnotationConfiguration
        configurableLocalSessionFactoryBean.lobHandler = lobHandler
        configurableLocalSessionFactoryBean.eventListeners = listensers
        configurableLocalSessionFactoryBean.hibernateProperties = DataSourceConfigUtil.getHibernatePoperties(config, dataSourcePrefix)
        configurableLocalSessionFactoryBean.entityInterceptor = new EmptyInterceptor()

        configurableLocalSessionFactoryBean.afterPropertiesSet()

        configurableLocalSessionFactoryBean.getObject()
    }()

    @InjectArg(ignore = true)
    @Lazy(soft = true)
    LobHandler lobHandler = {
        def lobHandlerDetectorFactoryBean = new SpringLobHandlerDetectorFactoryBean()
        lobHandlerDetectorFactoryBean.pooledConnection = true
        lobHandlerDetectorFactoryBean.dataSource = dataSource

        lobHandlerDetectorFactoryBean.getObject()
    }()

    @InjectArg(ignore = true)
    @Lazy(soft = true)
    PlatformTransactionManager transactionManager = {
        def transactionManager = new GrailsHibernateTransactionManager()
        transactionManager.sessionFactory = sessionFactory
        transactionManager.prepareConnection = false
        transactionManager.hibernateManagedSession = true
        return transactionManager
    }()

    @InjectArg(ignore = true)
    @Lazy(soft = true)
    MappingContext grailsDomainClassMappingContext = {
        new GrailsDomainClassMappingContext(grailsApplication)
    }()

    @InjectArg(ignore = true)
    @Lazy(soft = true)
    ApplicationContext applicationContext = {
        [
                containsBean: { String beanName ->
                    try {
                        //avoids a stackoverflow when
                        if (beanName == "sessionFactory") return true

                        this."$beanName"
                        return true
                    }
                    catch (MissingPropertyException e) {
                        return false
                    }
                },

                getAutowireCapableBeanFactory: {
                    [
                            autowireBeanProperties: { Object existingBean, int autowireMode, boolean dependencyCheck ->
                                //do nothing
                            }
                    ] as AutowireCapableBeanFactory
                },

                getBeansOfType: { Class clazz ->
                    if (clazz == SessionFactory) {
                        return [sessionFactory: sessionFactory]
                    }
                },

                getBean: { String beanName, Class type = null ->
                    try {
                        def bean = this."$beanName"
                        if (type) {
                            return bean.asType(type)
                        }

                        return bean
                    }
                    catch (Throwable throwable) {
                        throw new NoSuchBeanDefinitionException(beanName)
                    }
                },

                publishEvent: { ApplicationEvent event ->
                    applicationEventMulticaster.multicastEvent(event)
                },

                addApplicationListener: { ApplicationListener listener ->
                    applicationEventMulticaster.addApplicationListener(listener)
                },

        ] as ConfigurableApplicationContext
    }()

    /**
     * @deprecated
     * @param classes
     */
    void enableGormFor(Class... classes) {
        enableFor(classes)
    }

    @Override
    protected void doEnableFor(Class... classes) {
        //makes the meta class enhancements work properly
        ExpandoMetaClass.enableGlobally();
        entities = classes

        //unique constraint is not added by default, I assume since it requires a database lookup and gorm is not
        //specific to the dataStore
        ConstrainedProperty.registerNewConstraint(UniqueConstraint.UNIQUE_CONSTRAINT,
                new PersistentConstraintFactory(applicationContext, UniqueConstraint))

        //adds all the query and save methods
        DomainClassGrailsPlugin.enhanceDomainClasses(grailsApplication, applicationContext)

        classes.each { clazz ->
            GrailsDomainClass grailsDomainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, clazz.name)
            DomainClassGrailsPlugin.addValidationMethods(grailsApplication, grailsDomainClass, applicationContext)
        }

        HibernateUtils.enhanceSessionFactories(applicationContext, grailsApplication)

        //adds all the interceptor methods, beforeInsert and stuff like that.
        (applicationContext as ConfigurableApplicationContext)
                .addApplicationListener new EventTriggeringInterceptor(hibernateDatastore, grailsApplication.config)

        //fixes transactional methods to work properly
        classes.each { clazz ->
            clazz.metaClass.'static'.withTransaction = { Closure closure ->
                owner.withTransaction closure
            }

            clazz.metaClass.'static'.withNewTransaction = { Closure closure ->
                owner.withNewTransaction closure
            }
        }
    }

    @Override
    void withTransaction(Closure closure) {
        enableCurrentSession()

        if (!closure) {
            return
        }

        new GrailsTransactionTemplate(transactionManager).execute(closure)
    }

    protected void enableCurrentSession() {
        def transaction = sessionFactory.currentSession.getTransaction()
        if (!transaction.isActive()) {
            transaction.begin()
        }
        Assert.notNull transactionManager, "No transactionManager bean configured"
    }

    void withNewTransaction(Closure closure) {
        enableCurrentSession()

        if (!closure) {
            return
        }

        def session = sessionFactory.openSession()
        def holder = new SessionHolder(session)
        TransactionSynchronizationManager.bindResource(sessionFactory, holder);

        def transactionTemplate = new GrailsTransactionTemplate(transactionManager,
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW))
        transactionTemplate.execute(closure)
    }
}
