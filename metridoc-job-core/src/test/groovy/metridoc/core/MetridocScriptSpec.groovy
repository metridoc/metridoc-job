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



package metridoc.core

import metridoc.core.services.HibernateService
import org.slf4j.LoggerFactory
import spock.lang.Specification

class MetridocScriptSpec extends Specification {

    Script script = new MetridocScriptHelper()

    void "test initializing the targetManager"() {
        when:
        MetridocScript.initializeTargetManagerIfNotThere(script)

        then:
        script.stepManager
    }

    void "target manager can only be initialize once"() {
        when:
        MetridocScript.initializeTargetManagerIfNotThere(script)
        def stepManager = script.stepManager
        MetridocScript.initializeTargetManagerIfNotThere(script)

        then:
        stepManager == script.stepManager
    }

    void "include tool returns the tool that has been instantiated or instantiated in the past"() {
        when:
        def service = script.includeService(HibernateService)

        then:
        service
        service == script.includeService(HibernateService)
    }

    void "test adding tools with arguments"() {
        when:
        def service
        script.includeService(HibernateService, entityClasses: [this.class])
        service = script.binding.includeService(HibernateService, entityClasses: [this.class])

        then:
        noExceptionThrown()
        service.entityClasses
    }

    void "test injection with abstract classes as parents"() {
        given:
        def binding = new Binding()
        def targetManager = new StepManager()
        binding.foo = "bar"
        def bar = new Bar()
        targetManager.binding = binding

        when:
        targetManager.handlePropertyInjection(bar)

        then:
        noExceptionThrown()
        "bar" == bar.foo
    }

    void "test running a step based on method name"() {
        when:
        def helper = new MetridocScriptHelper()
        helper.step(runFoo: "runs foo")
        helper.runSteps("runFoo")

        then:
        noExceptionThrown()
        helper.fooRan

        when: "running via a closure"
        def barRan = false
        helper.bar = {
            barRan = true
        }
        helper.step(bar: "runs bar")
        helper.runSteps("bar")

        then:
        noExceptionThrown()
        barRan

        when: "adding a step that does not have a corresponding method or closure"
            helper.step(noMethod: "adding a bad step")

        then:
        def error = thrown(AssertionError)
        error.message.contains("Could not find a corresponding method or closure for step")
    }

    void "test logging extension"() {
        when:
        def logName
        new Script(){
            @Override
            Object run() {
                logName = log.name
            }
        }.run()

        then:
        "metridoc.script" == logName

        when:

        new Script(){
            @Override
            Object run() {
                log = LoggerFactory.getLogger("foo.bar")
                logName = log.name
            }
        }.run()

        then:
        "foo.bar" == logName
    }
}

class MetridocScriptHelper extends Script {

    boolean fooRan = false

    @Override
    Object run() {
        return null  //To change body of implemented methods use File | Settings | File Templates.
    }

    void runFoo() {
        fooRan = true
    }
}

abstract class Foo {
    def foo
}

class Bar extends Foo {

}