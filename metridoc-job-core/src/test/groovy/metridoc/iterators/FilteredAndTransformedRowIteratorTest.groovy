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



package metridoc.iterators

import spock.lang.Specification

import static metridoc.iterators.Iterators.toFilteredAndTransformedIterator
import static metridoc.iterators.Iterators.toRowIterator

/**
 * Created with IntelliJ IDEA on 6/14/13
 * @author Tommy Barker
 */
class FilteredAndTransformedRowIteratorTest extends Specification {

    def "simple filter and transform spec"() {
        given: "an iterator with items foo and bar"
        def fooBar = [
                [foo: "bar", bar: 5],
                [foo: "bar", bar: 2],
                [foo: "bar", bar: 3],
                [foo: "bar", bar: 4],
                [foo: "bar", bar: 1]
        ]

        and: "a filter transform record iterator that ignores odd numbers"
        def fooBarRow = toFilteredAndTransformedIterator(toRowIterator(fooBar)) { Record record ->

            if (record.body.bar % 2 == 0) {
                return record
            }

            return null
        }

        when: "all data is collected"
        def collection = fooBarRow.collect()

        then: "there are only two items with correct data"
        2 == collection.size()
        ["bar", "bar"] == collection.collect { it.body.foo }
        [2, 4] == collection.collect { it.body.bar }
    }

}
