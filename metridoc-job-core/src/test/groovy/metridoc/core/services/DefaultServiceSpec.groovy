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

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 3/13/13
 * Time: 10:19 AM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("GroovyAccessibility")
class DefaultServiceSpec extends Specification {

    def service = new DefaultServiceHelper()

    void "check enabling mergeMetridocConfig"() {

        when:
        def binding = new Binding()
        binding.args = ["-mergeMetridocConfig=false"] as String[]
        binding.includeService(ConfigService)
        binding.includeTool(DefaultServiceHelper)

        then:
        !binding.configService.mergeMetridocConfig
    }

    void "converting a map to a map just returns the original config"() {
        given:
        Map expected = [bar: "foobar"]

        when:
        def foo = DefaultService.convertConfig(expected)

        then:
        foo == DefaultService.convertConfig(expected)
    }

    void "converting a config object flattens it"() {
        given:
        ConfigObject foo = new ConfigObject()
        foo.bar.baz = "foobar"

        when:
        def foobar = DefaultService.convertConfig(foo)["bar.baz"]

        then:
        "foobar" == foobar
    }

    void "converting a binding just returns the variable map"() {
        given:
        Binding foo = new Binding()
        foo.bar = "bam"

        when:
        def bam = DefaultService.convertConfig(foo)["bar"]

        then:
        "bam" == bam
    }

    void "get variable directly returns the value if the expected type is null"() {
        given:
        Map config = [bar: "foobar"]

        when:
        def foobar = DefaultService.getVariableHelper(config, "bar", null)

        then:
        "foobar" == foobar
    }

    void "if the variable does not exist, variable helper returns null"() {
        given:
        Map config = [bar: "foobar"]

        when:
        def blam = DefaultService.getVariableHelper(config, "blam", null)

        then:
        blam == null
    }

    void "if the variable exists and expected type is provided, then the converted value is provided, otherwise null is returned"() {
        given:
        Map config = [bar: "foobar"]

        when:
        def bar = DefaultService.getVariableHelper(config, "bar", Integer)

        then:
        null == bar
        "foobar" == DefaultService.getVariableHelper(config, "bar", String)
    }

    void "argsMap trumps binding when getting a variable"() {
        given:
        def binding = new Binding()
        service.setBinding(binding)
        binding.argsMap = [bar: "foo"]
        binding.bar = "foobar"

        when:
        def bar = service.getVariable("bar")

        then:
        "foo" == bar
    }

    void "binding trumps config"() {
        given:
        def config = new ConfigObject()
        config.foo = "bar"
        def binding = new Binding()
        binding.foo = "foobar"
        binding.config = config
        service.setBinding(binding)

        when:
        def foo = service.getVariable("foo")

        then:
        "foobar" == foo
    }

    void "if a config is set and a non available variable is searched for, null is returned"() {
        given:
        def config = new ConfigObject()
        def binding = new Binding()
        binding.config = config
        service.setBinding(binding)

        when:
        def bar = service.getVariable("bar")

        then:
        null == bar
    }

    void "test including tool with args"() {
        when:
        service.includeService(HibernateService, entityClasses: [this.class])

        then:
        noExceptionThrown()
    }

    void "test step via method or binding"() {
        when:
        service.step(foo: "runs foo")
        service.depends("foo")

        then:
        service.fooRan
    }
}


class DefaultServiceHelper extends DefaultService {
    boolean fooRan = false

    void foo() {
        fooRan = true
    }
}