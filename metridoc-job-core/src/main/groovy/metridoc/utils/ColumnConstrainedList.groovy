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

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 10/10/11
 * Time: 12:46 PM
 */
class ColumnConstrainedList extends AbstractList<Map<String, Object>> {

    private List<Map<String, Object>> wrappedList
    private Set<String> columns

    ColumnConstrainedList(List<Map<String, Object>> wrappedList, Set<String> columns) {
        Assert.notNull(columns, "the columns cannot be null")
        Assert.notNull(wrappedList, "the wrapped list cannot be null")
        Assert.isTrue(!wrappedList.isEmpty(), "The wrapped map cannot be empty")
        Assert.isTrue(!columns.isEmpty(), "The columns cannot be empty")

        this.wrappedList = wrappedList
        this.columns = columns
    }

    @Override
    Map<String, Object> get(int index) {
        return new ColumnConstrainedMap(wrappedList, columns)
    }

    @Override
    int size() {
        return wrappedList.size()
    }
}
