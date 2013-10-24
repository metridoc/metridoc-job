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



package metridoc.writers

import metridoc.core.StepManager
import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 7/5/13
 * @author Tommy Barker
 */
class WriteResponseSpec extends Specification {

    def response = new WriteResponse(body: [foo: "bar", bar: 5, foobar: 1.0, foobaz: 2.0])

    def "asType should return a representation of the response based on info stored in response"() {
        expect:
        b == response.asType(a)

        where:
        a       | b
        String  | "bar"
        Integer | 5
    }

    def "asType throws a GroovyCastException if there are multiple possibilities or no possibilities"() {
        when: "asType called on double"
        response as Double

        then: "an error is thrown"
        thrown GroovyCastException

        when: "asType is called when there is no possibility"
        response as StepManager //pick a random object

        then: "an error is thrown"
        thrown GroovyCastException
    }
}
