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



package metridoc.utils

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 12/16/12
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
class ApacheLogParserTest extends Specification {

    def "test parsing a common apache log"() {
        given: "a common log"
        def commonLog = '127.0.0.1 user-identifier frank [10/Oct/2000:13:55:36 -0700] "GET /apache_pb.gif HTTP/1.0" 200 2326'

        when: "parseCommon is called"
        def result = new ApacheLogParser().parseCommon(commonLog)

        then: "appropriate values are returned"
        "127.0.0.1" == result.ipAddress
        "user-identifier" == result.clientId
        "frank" == result.patronId
        result.logDate instanceof Date
        result.httpMethod == "GET"
        result.url == "/apache_pb.gif"
        result.httpStatus == 200
        result.fileSize == 2326
    }

    def "test parsing a combined apache log"() {
        given: "a combined apache log"
        def combinedLog = '127.0.0.1 user-identifier frank [10/Oct/2000:13:55:36 -0700] "GET /apache_pb.gif HTTP/1.0" 200 2326 "http://www.example.com/start.html" "Mozilla/4.08 [en] (Win98; I ;Nav)"'

        when: "parse combined is called"
        def result = new ApacheLogParser().parseCombined(combinedLog)

        then: "appropriate values are returned"
        "127.0.0.1" == result.ipAddress
        "user-identifier" == result.clientId
        "frank" == result.patronId
        result.logDate instanceof Date
        result.httpMethod == "GET"
        result.url == "/apache_pb.gif"
        result.httpStatus == 200
        result.fileSize == 2326
        result.refUrl == "http://www.example.com/start.html"
        result.agent == "Mozilla/4.08 [en] (Win98; I ;Nav)"

    }

    def "parsing apache dates should be able to handle brackets"() {
        when: "parsing a log date with brackets"
        def result = ApacheLogParser.parseLogDate("[10/Oct/2000:13:55:36 -0700]")

        then: "appropriate values are returned"
        def calendar = new GregorianCalendar()
        calendar.setTime(result)
        Calendar.OCTOBER == calendar.get(Calendar.MONTH)
        55 == calendar.get(Calendar.MINUTE)
        36 == calendar.get(Calendar.SECOND)
    }

}
