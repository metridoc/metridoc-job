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

import org.junit.Rule
import org.junit.contrib.java.lang.system.StandardOutputStreamLog
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 7/25/13
 * @author Tommy Barker
 */
class MainServiceSpec extends Specification {

    @Rule
    public final StandardOutputStreamLog log = new StandardOutputStreamLog()

    void "if the main tool is run for a none existant tool, error is thrown"() {
        given:
        def mainTool = new MainService()
        mainTool.defaultService = "foo"
        mainTool.runnableServices.camelService = CamelService

        when:
        mainTool.execute()

        then:
        thrown(AssertionError)
    }

    void "run tool spec"() {
        given: "binding containing args"
        def binding = new Binding()
        binding.args = ["foo"] as String[]

        and: "a service using that binding"
        def tool = new MainService(binding: binding)

        and: "the service contains a simple runnable service"
        tool.runnableServices = [
                foo: FooService
        ] as Map

        when:
        tool.execute()

        then:
        binding.fooService.fooRan
    }

    void "MainTool has to have runnableTools"() {
        when: "execute is called on the service with no tools set"
        new MainService().execute()

        then:
        thrown(AssertionError)
    }

    void "MainTool has to have params"() {
        when: "execute is called while having runnableTools but no params"
        new MainService(runnableTools: [foo: RunnableService]).execute()

        then:
        thrown(AssertionError)
    }

    void "if a param isn't available then the default tool is run"() {
        when:
        def mainTool = new MainService(runnableServices: [foo: FooService], defaultTool: "foo")
        mainTool.execute()

        then:
        mainTool.binding.fooService.fooRan
        noExceptionThrown()
    }
}


class FooService extends RunnableService {

    boolean fooRan = false
    String usage = "foo service"

    @Override
    def configure() {
        fooRan = true
    }
}