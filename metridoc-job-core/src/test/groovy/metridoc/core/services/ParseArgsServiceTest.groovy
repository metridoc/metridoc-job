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
 * @author Tommy Barker
 */
class ParseArgsServiceTest extends Specification {

    Binding binding = new Binding()
    ParseArgsService service

    void "test parsing basic arguments"() {
        when:
        def argsMap = primeService(["-foo=bar", "--bar=foo"])

        then:
        "bar" == argsMap.foo
        "foo" == argsMap.bar
    }

    void "test args with no equals"() {
        when:
        def argsMap = primeService(["-foo", "--bar", "-foobar=bar"])

        then:
        testArgsWithNoEquals(argsMap)
    }

    void "test parameters"() {
        when:
        def argsMap = primeService(["-foo", "--bar", "blah", "-foobar=bar", "bammo"])

        then:
        //do previous tests since this is an expansion
        testArgsWithNoEquals(argsMap)
        def params = argsMap.params
        2 == params.size()
        "blah" == params[0]
        "bammo" == params[1]
    }

    void testArgsWithNoEquals(Map argsMap) {
        assert argsMap.foo
        assert argsMap.bar
        assert "bar" == argsMap.foobar
    }

    void "test just the environment parameter"() {
        when:
        def argsMap = primeService(["-env=dev"])

        then:
        "dev" == argsMap.env
    }

    void "ParseArgs should be able to accept a Map"() {
        given:
        Binding binding = new Binding()
        binding.args = ["foo", "bar"]

        when:
        binding.includeService(ParseArgsService)

        then:
        def params = binding.argsMap.params
        params.contains("foo")
        params.contains("bar")
    }

    Map primeService(List args) {
        binding.args = args as String[]
        service = binding.includeService(ParseArgsService)
        return binding.argsMap
    }
}
