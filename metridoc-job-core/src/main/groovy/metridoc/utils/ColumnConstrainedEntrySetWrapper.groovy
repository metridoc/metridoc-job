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

import java.util.AbstractMap.SimpleImmutableEntry
import java.util.Map.Entry

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 10/10/11
 * Time: 3:37 PM
 */
class ColumnConstrainedEntrySetWrapper extends AbstractSet<Entry<String, Object>> {

    private Set<Entry<String, Object>> wrappedSet
    private Set<String> columns

    ColumnConstrainedEntrySetWrapper(Set<Entry<String, Object>> wrappedSet, Set<String> columns) {
        Assert.notNull(wrappedSet, "set cannot be null")
        this.wrappedSet = wrappedSet
        this.columns = columns
    }

    @Override
    Iterator<Entry<String, Object>> iterator() {
        return new EntrySetIterator(columns: columns, wrappedIterator: wrappedSet.iterator())
    }

    @Override
    int size() {
        int result = 0
        def iterator = iterator()
        while (iterator.hasNext()) {
            result++
            iterator.next()
        }

        return result
    }
}

class EntrySetIterator implements Iterator<Entry<String, Object>> {

    Entry<String, Object> next
    Iterator<Entry<String, Object>> wrappedIterator
    Set<String> columns

    boolean hasNext() {
        if (next == null) {
            if (wrappedIterator.hasNext()) {
                def possibleNext = wrappedIterator.next()

                def isColumn = columns.contains(possibleNext.key) ||
                    columns.contains(possibleNext.key.toLowerCase()) ||
                    columns.contains(possibleNext.key.toUpperCase())

                if (isColumn) {
                    next = new SimpleImmutableEntry(possibleNext)
                } else {
                    //skip this one, not a column we want
                    return hasNext()
                }
            }
        }

        if (next) {
            return true
        }

        return false
    }

    Entry<String, Object> next() {
        if (hasNext()) {
            def result = next
            next = null
            return result
        }

        throw new NoSuchElementException("no more elements")
    }

    void remove() {
        throw new UnsupportedOperationException("not supported")
    }

}