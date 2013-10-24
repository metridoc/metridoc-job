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

/**
 * Created with IntelliJ IDEA on 6/14/13
 * @author Tommy Barker
 * @deprecated
 */
class FilteredAndTransformedRecordIterator extends RecordIterator {

    RecordIterator iterator
    /**
     * should return null if we should not collect it
     */
    Closure<Record> transformer

    @Override
    protected Record computeNext() {
        assert transformer: "transformer cannot be null"
        assert iterator != null: "iterator cannot be null"

        while (iterator.hasNext()) {
            def next = iterator.next()
            def response = transformer.call(next.clone())

            if (response) {
                return response
            }
        }

        endOfData()
    }
}
