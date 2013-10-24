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



package metridoc.stream

import groovy.stream.Stream
import org.apache.poi.ss.usermodel.Row
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 10/22/13
 * @author Tommy Barker
 */
class XlsStreamSpec extends Specification {
    def file = new File("src/test/groovy/metridoc/stream/locations.xls")
    def iterator = Stream.fromXls(file)

    void "testing a location file we use in another program where we found errors"() {
        when:
        def xlsStream = new XlsStream(file: file)
        def header = xlsStream.headers.get(0)
        then:
        "LOCATION_ID" == header
    }

    void "testing cell conversion issues we were seeing with formulas"() {
        when:
        iterator.next()
        def row = iterator.next()

        then:
        2 == row.get("LOCATION_ID")
    }

    void "if a row has no data, then the row should be null"() {
        when:
        def xlsStream = new XlsStream(file: file)
        Row row = xlsStream.sheet.getRow(358)

        then:
        row

        when:
        row = xlsStream.sheet.getRow(359)

        then:
        row == null
    }

    void "if there is no more data an error is thrown"() {
        when:
        def next
        (1..358).each {
            next = iterator.next()
        }

        then:
        359 == next.get("LOCATION_ID")
        !iterator.hasNext()
    }
}
