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

import spock.lang.Specification

/**
 * @author Tommy Barker
 */
class StepSpec extends Specification {

    void "test step workflow from annotated class"() {
        given:
        def binding = new Binding()

        when:
        Foo foo
        foo = binding.includeService(Foo)
        binding.runStep("foo")

        then:
        foo.fooRan && foo.barRan
    }

    class Foo {

        boolean barRan = false
        boolean fooRan = false

        @Step(description = "runs bar")
        void bar() {
            assert !fooRan: "foo should run first"
            barRan = true
        }

        @Step(description = "runs foo", depends = ["bar"])
        void foo() {
            fooRan = true
        }
    }
}

