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

import com.google.common.collect.Table
import metridoc.iterators.Iterators
import spock.lang.Specification

class TableWriterSpec extends Specification {

    void "test basic table writing"() {
        given:
        def iterator = [
                [foo: "bar", bar: 5],
                [foo: "baz", bar: 7]
        ]

        when:
        def table = new TableIteratorWriter().write(Iterators.toRowIterator(iterator)).body.table as Table

        then:
        4 == table.size()
        2 == table.rowKeySet().size()
        "bar" == table.row(0).foo
        "baz" == table.row(1).foo
        5 == table.row(0).bar
        7 == table.row(1).bar
    }

    void "error thrown if iterator is null"() {
        when:
        new TableIteratorWriter().write(null)

        then:
        thrown AssertionError
    }
}
