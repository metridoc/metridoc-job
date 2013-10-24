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
class DelimitedLineStreamSpec extends Specification {
    def text = "blah|blam\nbloom|blim"
    def inputStream = new ByteArrayInputStream(text.bytes)
    def iterator = Stream.fromDelimited(inputStream, /\|/)

    void "test basic iteration"() {

        when:
        def next = iterator.next()

        then:
        "blah" == next[0]
        "blam" == next[1]

        when:
        next = iterator.next()

        then:
        "bloom" == next[0]
        "blim" == next[1]
        !iterator.hasNext()
    }

    void "delimiter must be set"() {
        when:
        Stream.fromDelimited(inputStream, null)

        then:
        thrown AssertionError
    }

    void "headers size must be the same as each line"() {
        given:
        def iterator = Stream.fromDelimited(inputStream, /\|/, [
                headers: ["foo"],
                delimitTill: 0,
        ])

        when:
        iterator.next()

        then:
        thrown IllegalStateException
    }

    void "if dirty data is specified, assertion error happens instead"() {
        given:
        def iterator = Stream.fromDelimited(inputStream, /\|/, [
                headers: ["foo"],
                delimitTill: 0,
                dirtyData: true
        ])

        when:
        iterator.next()

        then:
        thrown AssertionError
    }
}
