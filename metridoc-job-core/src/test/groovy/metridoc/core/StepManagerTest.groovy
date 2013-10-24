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

class StepManagerTest {

    def stepManager = new StepManager()

    @Test
    void "when a job is interrupted it should throw an exception"() {
        stepManager.interrupt()

        try {
            stepManager.profile("do something") {

            }
            assert false: "exception should have occurred"
        }
        catch (JobInterruptionException ignored) {
        }
    }

    @Test
    void "test general functionality of including and using targets"() {
        stepManager.includeSteps(MetridocJobTestTargetHelper)
        stepManager.defaultStep = "bar"
        stepManager.runDefaultStep()
        assert stepManager.binding.fooRan
        assert stepManager.binding.foobarRan
        assert stepManager.stepsRan.contains("foo")
        assert stepManager.stepsRan.contains("bar")
    }

    @Test
    void "include tool returns the tool it instantiates or has already instantiated"() {
        def tool = stepManager.includeService(HibernateService)
        assert tool
        assert tool instanceof HibernateService
        assert tool == stepManager.includeService(HibernateService)
    }

    @Test
    void "test target manager interruption"() {
        assert !stepManager.interrupted
        stepManager.interrupt()
        assert stepManager.interrupted
        assert stepManager.binding.interrupted
    }

    @Test
    void "if the binding has an interrupted value set to true, then it is interrupted"() {
        stepManager.binding.interrupted = true
        assert stepManager.interrupted
    }

    @Test
    void "test property injection"() {
        def binding = stepManager.binding
        binding.bar = "foo"
        binding.bam = "foo"
        binding.foobar = "55" //requires conversion
        binding.blammo = "55" //does not exist in service
        binding.something = "foobar" //wrong status

        stepManager.includeService(FooToolHelper)
        FooToolHelper helper = binding.fooToolHelper
        assert "foo" == helper.bar
        assert "foo" == helper.bam
        assert 55 == helper.foobar
        assert null == helper.something
    }

    @Test
    void "injection uses getVariable if the class extends DefaultService"() {
        def binding = stepManager.binding
        binding.config = new ConfigObject()
        binding.config.foo = "bar"
        def helper = stepManager.includeService(FooBarServiceHelper)
        assert helper.foo == "bar"
    }

    @Test
    void "property injection should override already set properties"() {
        def binding = stepManager.binding
        binding.foo = "bam"
        def helper = new PropertyInjectionHelper()
        stepManager.handlePropertyInjection(helper)
        assert "bam" == helper.foo

        //check that the current properties are maintained
        assert "foo" == helper.bar
    }

    @Test
    void "fine grain injection can be controlled by InjectArg annotation"() {
        def binding = stepManager.binding
        binding.config = new ConfigObject()
        binding.config.foo.bar = "fromConfig"
        binding.baz = "shouldNotInject"
        def helper = new PropertyInjectionHelper()
        stepManager.handlePropertyInjection(helper)
        assert "fromConfig" == helper.fooBar
        assert null == helper.baz

        binding.argsMap = [fooBaz: "bam"]
        stepManager.handlePropertyInjection(helper)
        assert "bam" == helper.fooBar

        helper = binding.includeService(PropertyInjectionHelper)
        assert "bam" == helper.fooBar
    }

    @Test
    void "test injection with a base"() {
        def binding = stepManager.binding
        binding.config = new ConfigObject()
        binding.config.foo.bar = "bam"
        def helper = new PropertyInjectorHelperWithBase()
        stepManager.handlePropertyInjection(helper)
        assert "bam" == helper.bar
    }

    @Test
    void "target flag should also work in MetridocScript"() {
        def binding = new Binding()
        binding.args = ["--target=foo"]
        def helper = new MetridocJobTestTargetHelperWithDefaultTarget()
        helper.binding = binding
        helper.run()
        assert helper.fooRan
        assert !helper.barRan
    }

    @Test
    void "includeService should perform injection on previously loaded services"() {
        def foobar = stepManager.includeService(FooBar)
        def foo = stepManager.includeService(Foo)
        def bar = stepManager.includeService(Bar)

        assert foo.bar == bar
        assert bar.foo == foo
        assert !foobar.foo
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
