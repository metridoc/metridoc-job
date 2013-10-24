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


package metridoc.sql

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 6/25/12
 * Time: 2:37 PM
 */
class BulkSql {

    String getBulkInsert(String from, String to, Map<String, String> columnMap) {

        return insertHelper(from, to, false) {insert, select ->
            columnMap.each {key, value ->
                insert.append("${value}, ")
                select.append("${key}, ")
            }
        }
    }

    String getBulkInsert(String from, String to, List<String> columns) {
        return getBulkInsert(from, to, false, columns)
    }

    String getBulkInsert(String from, String to, boolean noDup, List<String> columns) {
        return insertHelper(from, to, noDup) {insert, select ->
            columns.each {
                insert.append("${it}, ")
                select.append("${it}, ")
            }
        }
    }

    String getNoDuplicateBulkInsert(String from, String to, String noDuplicateColumn, Map<String, String> columnMap) {

        String noDup = " on duplicate key update ${to}.${noDuplicateColumn} = ${to}.${noDuplicateColumn}"
        String bulkInsert = insertHelper(from, to, true) {insert, select ->
            columnMap.each {key, value ->

                insert.append("${value}, ")
                select.append("${key}, ")
            }
        }

        if (noDuplicateColumn) {
            bulkInsert += noDup
        }

        return bulkInsert
    }

    String getNoDuplicateBulkInsert(String from, String to, String noDuplicateColumn, List<String> columns) {
        String noDup = " on duplicate key update ${to}.${noDuplicateColumn} = ${to}.${noDuplicateColumn}"
        String bulkInsert = getBulkInsert(from, to, true, columns)
        if (noDuplicateColumn) {
            bulkInsert += noDup
        }

        return bulkInsert
    }

    protected static String insertHelper(String from, String to, boolean distinctSelect, Closure closure) {

        def insert = new StringBuilder("insert into ${to}(")
        def select = new StringBuilder("select ")

        closure.setDirective(Closure.TO_SELF)
        closure.setResolveStrategy(Closure.TO_SELF)

        closure.call(insert, select)

        insert.delete(insert.length() - 2, insert.length())
        select.delete(select.length() - 2, select.length())
        select.append(" from ${from}")
        insert.append(") ${select.toString()}")

        return insert.toString()
    }

}
