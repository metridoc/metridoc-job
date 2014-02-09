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

import grails.persistence.Entity
import grails.validation.ValidationException
import groovy.sql.Sql
import metridoc.core.services.ConfigService
import metridoc.tool.gorm.User
import org.apache.commons.dbcp.BasicDataSource
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 8/1/13
 * @author Tommy Barker
 */
class GormServiceSpec extends Specification {

    def service = new GormService(embeddedDataSource: true)
    Sql sql

    void setup() {
        sql = new Sql(service.dataSource)
    }

    void cleanup() {
        dropTable("user")
        dropTable("foo_with_date")
    }

    void dropTable(String tableName) {
        try {
            sql.execute("drop table if exists $tableName" as String)
        }
        catch (Throwable throwable) {
            //do nothing
        }
    }

    void "enableGorm should fail on more than one call"() {
        when:
        service.enableFor(User)

        then:
        noExceptionThrown()

        when:
        service.enableFor(User)

        then:
        thrown(IllegalStateException)
    }

    void "test basic gorm operations"() {
        setup:
        service.enableFor(User)
        def sql = new Sql(service.dataSource)
        def user = new User(name: "joe", age: 7)

        when:
        service.withTransaction {
            assert user.save(failOnError: true)
        }
        def total = sql.firstRow("select count(*) as total from user").total

        then:
        noExceptionThrown()
        User.list().size() == 1
        total == 1
    }

    void "test gorm with dataSource properties"() {
        setup:
        service.includeService(mergeMetridocConfig: false, ConfigService)
        ConfigObject configObject = service.config
        configObject.dataSource.driverClassName = "org.h2.Driver"
        configObject.dataSource.username = "sa"
        configObject.dataSource.password = ""
        configObject.dataSource.url = "jdbc:h2:mem:devDbManual;MVCC=TRUE;LOCK_TIMEOUT=10000"
        service.enableFor(User)
        def dataSource = service.dataSource
        def sql = new Sql(dataSource)
        def user = new User(name: "joe", age: 7)

        when:
        service.withTransaction {
            user.save()
        }
        def total = sql.firstRow("select count(*) as total from user").total

        then:
        dataSource.connection.metaData.getURL().startsWith("jdbc:h2:mem:devDb")
        noExceptionThrown()
        total == 1
    }

    void "test within the scope of MetridocScript"() {

        when:
        new GormServiceScriptHelper().run()

        then:
        noExceptionThrown()
    }

    void "everything should work as a script"() {

        given:
        def path = "src/test/resources/testScripts"
        def scriptDir = new File(path)

        if (!scriptDir.exists()) {
            scriptDir = new File("metridoc-tool-gorm/$path")
        }

        def scriptFile = new File("foobar.groovy", scriptDir)
        def shell = new GroovyShell()

        when:
        shell.evaluate(scriptFile)

        then:
        noExceptionThrown()
    }

    void "lets test invalid data"() {
        given: "empty user"
        def user = new User()
        service.enableFor(User)

        when:
        def valid
        service.withTransaction { valid = user.validate() }

        then:
        !valid
        "nullable" == user.errors.getFieldError("name").code
    }

    void "test retrieving the session factory"() {
        when:
        service.enableFor(User)
        def sessionFactory = service.sessionFactory

        then:
        noExceptionThrown()
        sessionFactory
    }

    void "test for error when getting sessionFactory and enableFor has not been called"() {
        when:
        service.sessionFactory

        then:
        def error = thrown(AssertionError)
        error.message.contains("[SessionFactory] cannot be retrieved until [enableFor] is called for one or more entities")
    }

    void "the dataSource in the binding should be the same as the one in the binding"() {
        given:
        def binding = new Binding()
        def dataSource = new BasicDataSource(
                username: "sa",
                password: "",
                url: "jdbc:h2:mem:checkForSame;MVCC=TRUE;LOCK_TIMEOUT=10000",
                driverClassName: "org.h2.Driver"
        )
        binding.dataSource = dataSource

        when:
        def gormService = binding.includeService(GormService)
        gormService.enableFor(User)

        then: "the dataSources are the same"
        dataSource == gormService.applicationContext.getBean("dataSource")
        dataSource.url == gormService.applicationContext.getBean("dataSource").url
    }

    void "test date generation"() {
        when:
        service.enableFor(FooWithDate)
        Date now = new Date()
        FooWithDate.withTransaction {
            new FooWithDate(name: "foo").save(failOnError: true)
            //let's embed something
            FooWithDate.withTransaction {
                new FooWithDate(name: "bar").save(failOnError: true)
            }
        }

        then:
        FooWithDate.first().dateCreated > now
        FooWithDate.first().lastUpdated > now
        2 == FooWithDate.count

        when:
        def before = FooWithDate.findByName("bar").lastUpdated
        FooWithDate.withTransaction {
            def first = FooWithDate.findByName("bar")
            first.name = "baz"
            first.save(failOnError: true)
        }

        then:
        noExceptionThrown()
        before < FooWithDate.findByName("baz").lastUpdated
        !FooWithDate.findByName("bar")
    }

    void "test failure"() {
        when:
        service.enableFor(FooWithDate)
        FooWithDate.withNewTransaction {
            def fooWithData = new FooWithDate()
            fooWithData.save(failOnError: true)
        }

        then:
        def ex = thrown(ValidationException)
        "nullable" == ex.errors.getFieldError("name").code
    }

    void "test transaction failures"() {
        when: "saving in a new transaction within a transaction"
        service.enableFor(FooWithDate)
        service.withTransaction {
            FooWithDate.withNewTransaction {
                new FooWithDate(name: "bar").save()
            }

            throw new RuntimeException("forced failure")
        }

        then: "the save in the NEW transaction gets saved despite the error"
        thrown(RuntimeException)
        1 == FooWithDate.count

        when: "an error happens in a transaction with a save"
        service.withTransaction {
            FooWithDate.list()*.delete()
        }
        service.withTransaction {
            new FooWithDate(name: "baz").save()

            throw new RuntimeException("forced failure")
        }

        then: "nothing is saved"
        thrown(RuntimeException)
        0 == FooWithDate.count
    }

    void "test uniqueness constraint" () {
        when:
        boolean valid
        service.enableFor(FooWithDate)
        service.withTransaction {
            new FooWithDate(name: "foobar").save()
            valid = new FooWithDate(name: "foobar").validate()
        }

        then:
        !valid
    }

    void "multiple newTransaction calls should not return IllegalStateException"() {
        when:
        new GormMultipleNewTransactionCallsHelper().run()

        then:
        noExceptionThrown()
    }
}

class GormServiceScriptHelper extends Script {

    @Override
    def run() {
        def gorm = includeService(embeddedDataSource: true, GormService)
        gorm.enableFor(User)
    }
}

class GormMultipleNewTransactionCallsHelper extends Script {

    def run() {
        def gorm = includeService(embeddedDataSource: true, GormService)
        gorm.enableFor(FooWithDate)

        FooWithDate.withNewTransaction {}
        FooWithDate.withNewTransaction {}
    }
}

@Entity
class FooWithDate {

    static constraints = {
        name(unique: true)
    }

    String name
    Date dateCreated
    Date lastUpdated
}
