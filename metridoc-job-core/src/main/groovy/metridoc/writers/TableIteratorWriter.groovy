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

import com.google.common.collect.TreeBasedTable
import metridoc.iterators.Record
import metridoc.iterators.RecordIterator
import org.apache.commons.lang.ObjectUtils

/**
 * @deprecated
 */
class TableIteratorWriter extends DefaultIteratorWriter {

    @Override
    WriteResponse write(RecordIterator recordIterator) {
        assert recordIterator != null: "rowIterator cannot be null"
        recordIterator.recordHeaders.table = TreeBasedTable.create()
        def response = super.write(recordIterator)
        response.body.table = response.headers.table

        return response
    }

    @Override
    boolean doWrite(int lineNumber, Record record) {
        def headers = record.headers
        def table = headers.table
        record.body.each { columnKey, value ->
            table.put(lineNumber, columnKey, value ?: ObjectUtils.NULL)
        }

        return true
    }
}
