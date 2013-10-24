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

import org.junit.Test

/**
 * Created with IntelliJ IDEA on 5/29/13
 * @author Tommy Barker
 */
class XlsxIteratorTest {

    def file = new File("src/test/groovy/metridoc/iterators/locations.xlsx")
    def iterator = new XlsxIterator(inputStream: file.newInputStream())

    @Test
    void "testing basic iteration"() {
        assert "LOCATION_ID" == iterator.headers.get(0)
        def row = iterator.next()
        assert 1 == row.body.get("LOCATION_ID")
    }

    @SuppressWarnings("GroovyVariableNotAssigned")
    @Test
    void "if there is no more data an error is thrown"() {
        def next
        (1..358).each {
            next = iterator.next()
        }

        assert 359 == next.body.get("LOCATION_ID")
        assert !iterator.hasNext()

        try {
            iterator.next()
            assert false: "exception should have occurred"
        }
        catch (NoSuchElementException ignored) {

        }
    }

}
