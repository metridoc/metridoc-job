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
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 10/22/13
 * @author Tommy Barker
 */
class XmlStreamSpec extends Specification {

    String xml = '''
    <records>
      <car name='HSV Maloo' make='Holden' year='2006'>
        <country>Australia</country>
        <record type='speed'>Production Pickup Truck with speed of 271kph</record>
      </car>
      <car name='P50' make='Peel' year='1962'>
        <country>Isle of Man</country>
        <record type='size'>Smallest Street-Legal Car at 99cm wide and 59 kg in weight</record>
      </car>
      <car name='Royale' make='Bugatti' year='1931'>
        <country>France</country>
        <record type='price'>Most Valuable Car at $15 million</record>
      </car>
    </records>
  '''
    InputStream xmlStream = new ByteArrayInputStream(xml.getBytes("utf-8"))

    void "basic xml tests"() {
        given:
        def iterator = Stream.fromXml(xmlStream, "car")

        when:
        def next = iterator.next()

        then:
        noExceptionThrown()
        "Australia" == next.country.text()

        when:
        next = iterator.next()

        then:
        noExceptionThrown()
        "Isle of Man" == next.country.text()

        when:
        next = iterator.next()

        then:
        noExceptionThrown()
        "France" == next.country.text()
        !iterator.hasNext()
    }

    void "tag has to be set"() {
        given:
        def iterator = new XmlStream(inputStream: xmlStream)

        when:
        iterator.next()

        then:
        thrown(AssertionError)
    }

    void "stream has to be set"() {
        given:
        def iterator = new XmlStream(tag: "car")

        when:
        iterator.next()

        then:
        thrown(AssertionError)
    }

    void "full workflow to a list of maps"() {
        when:
        List<Map> table = Stream.fromXml(xmlStream, "car").map {Map record ->
            def result = [:]
            result.name = record.root.@name.text()
            result.country = record.country.text()

            return result
        }.collect()

        then:
        3 == table.size()
        "Australia" == table.get(0).country
        "HSV Maloo" == table.get(0).name

        "France" == table.get(2).country
        "Royale" == table.get(2).name
    }
}
