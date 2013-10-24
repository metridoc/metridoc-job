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

import groovy.sql.Sql

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 9/16/12
 * Time: 2:28 PM
 * To change this template use File | Settings | File Templates.
 * @deprecated
 */
class StreamingTableList extends AbstractList {
    def id = "id"

    @Override
    Iterator iterator() {
        return new StreamingNoNullIterator(iterator: super.iterator())
    }

    def table
    def dataSource
    def batchSize
    def currentCache = [:]
    private size

    def selectSize = {id, table ->
        "select max(${id}) as total from $table" as String
    }

    def selectAllWithIdBetween = {id, table, low, hi ->
        "select * from ${table} where ${id} >= ${low} and ${id} <= ${hi}" as String
    }

    private Sql _sql

    @Override
    def synchronized get(int i) {

        if(currentCache.containsKey(i)) {
            return currentCache.get(i)
        }

        if(i >= size()) {
            throw new ArrayIndexOutOfBoundsException("$i is greater than or equal to ${size()}")
        }

        def hi = Math.min(size(), i + batchSize)

        currentCache.clear()
        (i..batchSize).each {
            currentCache[it] = null
        }

        sql.eachRow(selectAllWithIdBetween(id, table, i, hi)) {
            currentCache[it."$id"] = it
        }

        return currentCache.get(i)
    }

    @Override
    int size() {
        if(size) return size

        size = sql.firstRow(selectSize(id, table)).total as Integer
    }

    def getSql() {
        if(_sql) return _sql
        assert dataSource: "dataSource must not be null"

        _sql = new Sql(dataSource)
    }
}
