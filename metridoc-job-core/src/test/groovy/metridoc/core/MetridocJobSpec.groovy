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
 * Created with IntelliJ IDEA on 6/7/13
 * @author Tommy Barker
 */
class MetridocJobSpec extends Specification {

    def "basic to and from spec"() {
        given:
        def metridocJob = new HelperJob()

        when: "when data is sent and consumed from a seda endpoint by calling execute"
        metridocJob.execute()

        then: "consumption of the data should happen"
        metridocJob.fromCalled
    }
}

class HelperJob extends MetridocJob {

    boolean fromCalled = false
    String usage = "helper job"

    @Override
    def configure() {
        asyncSend("seda:test", "testBody")
        consume("seda:test") {
            assert "testBody" == it
            fromCalled = true
        }
    }
}