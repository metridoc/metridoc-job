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

import spock.lang.Specification

import static metridoc.writers.WrittenRecordStat.Status.*

/**
 * Created with IntelliJ IDEA on 7/9/13
 * @author Tommy Barker
 */
class SplitWriteResponseSpec extends Specification {

    def "test treating the response as an array"() {
        given: "two responses in split response"
        def splitResponse = new SplitWriteResponse()
        def response1 = new WriteResponse()
        def stats1 = response1.aggregateStats
        stats1[ERROR] = 2
        stats1[INVALID] = 5
        stats1[IGNORED] = 20
        stats1[WRITTEN] = 13

        def response2 = new WriteResponse()
        def stats2 = response2.aggregateStats
        stats2[ERROR] = 23
        stats2[INVALID] = 2
        stats2[IGNORED] = 2
        stats2[WRITTEN] = 10

        splitResponse.addResponse(response1)
        splitResponse.addResponse(response2)

        when: "retrieving the responses as an array from split response"
        def response3 = splitResponse[0]
        def response4 = splitResponse[1]

        then:
        response1 == response3
        response2 == response4

        and: "data should be aggregated"
        def splitStats = splitResponse.aggregateStats
        splitStats[ERROR] == 25
        splitStats[INVALID] == 7
        splitStats[IGNORED] == 22
        splitStats[WRITTEN] == 23
    }


}
