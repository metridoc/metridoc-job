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

import groovy.transform.ToString

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 10/7/11
 * Time: 1:54 PM
 *
 * Creates a read only map which wraps another map and will only return values where keys are both in the wrapped map and the
 * set of columns.  So even though a key might be in the wrapped map, if it is not in the set of columns it will
 * appear as if the map does not contain it
 *
 */
@ToString(includeNames = true)
class ColumnConstrainedMap extends AbstractMap<String, Object> {

    private Map<String, Object> wrappedMap
    private Set<String> columns

    ColumnConstrainedMap(Map<String, Object> wrappedMapToSet, Set<String> columnsToSet) {
        assert columnsToSet: "the columns cannot be null"
        assert wrappedMapToSet: "the wrapped map cannot be null"
        assert !wrappedMapToSet.isEmpty(): "The wrapped map cannot be empty"
        assert !columnsToSet.isEmpty(): "The columns cannot be empty"
        wrappedMap = wrappedMapToSet
        columns = columnsToSet
    }

    /**
     * contains key as long as it is in the columns and the wrapped map
     * @param key
     * @return
     */
    boolean containsKey(Object key) {

        def keyText = key as String

        def caseCheck = {object, String method ->
            return object."${method}"(keyText) || object."${method}"(keyText.toLowerCase()) || object."${method}"(keyText.toUpperCase())
        }

        def keyIsColumn = caseCheck(columns, "contains")
        def keyIsInWrappedMap = caseCheck(wrappedMap, "containsKey")

        keyIsColumn && keyIsInWrappedMap
    }

    Object get(Object key) {
        containsKey(key) ? wrappedMap.get(key) : null
    }

    Set<Map.Entry<String, Object>> entrySet() {
        return new ColumnConstrainedEntrySetWrapper(wrappedMap.entrySet(), columns)
    }
}
