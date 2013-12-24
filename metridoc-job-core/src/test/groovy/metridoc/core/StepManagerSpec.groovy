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

import metridoc.core.services.DefaultService
import metridoc.core.services.HibernateService
import metridoc.core.services.Service
import org.junit.Test
import spock.lang.Specification

class StepManagerSpec extends Specification {

    def stepManager = new StepManager()

    void "when a job is interrupted it should throw an exception"() {
        when:
        stepManager.interrupt()
        stepManager.profile("do something") {

        }

        then:
        thrown(JobInterruptionException)
    }

    void "test general functionality of including and using targets"() {
        when:
        stepManager.includeSteps(MetridocJobTestTargetHelper)
        stepManager.defaultStep = "bar"
        stepManager.runDefaultStep()

        then:
        stepManager.binding.fooRan
        stepManager.binding.foobarRan
        stepManager.stepsRan.contains("foo")
        stepManager.stepsRan.contains("bar")
    }

    void "include tool returns the tool it instantiates or has already instantiated"() {
        when:
        def tool = stepManager.includeService(HibernateService)

        then:
        tool
        tool instanceof HibernateService
        tool == stepManager.includeService(HibernateService)
    }

    void "test target manager interruption"() {
        expect:
        assert !stepManager.interrupted

        when:
        stepManager.interrupt()

        then:
        stepManager.interrupted
        stepManager.binding.interrupted
    }

    void "if the binding has an interrupted value set to true, then it is interrupted"() {
        when:
        stepManager.binding.interrupted = true

        then:
        stepManager.interrupted
    }

    void "test property injection"() {
        when:
        def binding = stepManager.binding
        binding.bar = "foo"
        binding.bam = "foo"
        binding.foobar = "55" //requires conversion
        binding.blammo = "55" //does not exist in service
        binding.something = "foobar" //wrong status
        stepManager.includeService(FooToolHelper)
        FooToolHelper helper = binding.fooToolHelper

        then:
        "foo" == helper.bar
        "foo" == helper.bam
        55 == helper.foobar
        null == helper.something
    }

    void "injection uses getVariable if the class extends DefaultService"() {
        when:
        def binding = stepManager.binding
        binding.config = new ConfigObject()
        binding.config.foo = "bar"
        def helper = stepManager.includeService(FooBarServiceHelper)

        then:
        helper.foo == "bar"
    }

    void "property injection should override already set properties"() {
        when:
        def binding = stepManager.binding
        binding.foo = "bam"
        def helper = new PropertyInjectionHelper()
        stepManager.handlePropertyInjection(helper)

        then:
        "bam" == helper.foo
        //check that the current properties are maintained
        "foo" == helper.bar
    }

    void "fine grain injection can be controlled by InjectArg annotation"() {
        when:
        def binding = stepManager.binding
        binding.config = new ConfigObject()
        binding.config.foo.bar = "fromConfig"
        binding.baz = "shouldNotInject"
        def helper = new PropertyInjectionHelper()
        stepManager.handlePropertyInjection(helper)

        then:
        "fromConfig" == helper.fooBar
        null == helper.baz

        when:
        binding.argsMap = [fooBaz: "bam"]
        stepManager.handlePropertyInjection(helper)

        then:
        "bam" == helper.fooBar

        when:
        helper = binding.includeService(PropertyInjectionHelper)

        then:
        "bam" == helper.fooBar
    }

    void "test injection with a base"() {
        when:
        def binding = stepManager.binding
        binding.config = new ConfigObject()
        binding.config.foo.bar = "bam"
        def helper = new PropertyInjectorHelperWithBase()
        stepManager.handlePropertyInjection(helper)

        then:
        "bam" == helper.bar
    }

    void "target flag should also work in MetridocScript"() {
        when:
        def binding = new Binding()
        binding.args = ["--target=foo"]
        def helper = new MetridocJobTestTargetHelperWithDefaultTarget()
        helper.binding = binding
        helper.run()

        then:
        helper.fooRan
        !helper.barRan
    }

    void "includeService should perform injection on previously loaded services"() {
        when:
        def foobar = stepManager.includeService(FooBar)
        def foo = stepManager.includeService(Foo)
        def bar = stepManager.includeService(Bar)

        then:
        foo.bar == bar
        bar.foo == foo
        !foobar.foo
    }

    void "when injecting args, lazy fields should not be activated"() {
        when:
        def fooLazy = new FooLazy()
        new StepManager().handlePropertyInjection(fooLazy)

        then:
        !fooLazy.lazyCalled
    }

    class FooLazy {
        boolean lazyCalled = false

        @Lazy
        String bar = {
            lazyCalled = true
            return "bar"
        }()

    }

    class FooToolHelper implements Service {
        def bar
        String bam
        Integer foobar
        Integer something

        @Override
        void setBinding(Binding binding) {

        }
    }

    class PropertyInjectionHelper {
        def bar = "foo"
        def foo = "bar"
        @InjectArg(config = "foo.bar", cli = "fooBaz", injectByName = false)
        def fooBar
        @InjectArg(config = "foo.baz", injectByName = false)
        def baz

        def binding
    }

    @InjectArgBase("foo")
    class PropertyInjectorHelperWithBase {
        def bar
    }

    class FooBarServiceHelper extends DefaultService {
        String foo
    }

    class MetridocJobTestTargetHelperWithDefaultTarget extends Script {
        @Override
        Object run() {
            includeSteps(MetridocJobTestTargetHelper)
            defaultStep = "bar"
            runDefaultStep()
        }
    }

    class MetridocJobTestTargetHelper extends Script {

        @Override
        Object run() {
            fooRan = false
            barRan = false
            foobarRan = false

            step(foobar: "runs foobar") {
                foobarRan = true
            }

            step(foo: "runs foo") {
                fooRan = true
                assert this.stepManager instanceof StepManager
            }

            step(bar: "runs bar", depends: "foobar") {
                barRan = true
                depends("foo")
            }
        }
    }

    class Foo {
        Bar bar
    }

    class Bar {
        Foo foo
    }

    class FooBar {
        String foo
    }
}
