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

import java.sql.ResultSet

/**
 * Created with IntelliJ IDEA on 9/11/13
 * @deprecated
 *
 * @author Tommy Barker
 */
public class IteratorsExtension {

    public static WrappedIterator fromSql(Iterators self, ResultSet resultSet) {
        assert resultSet != null: "resultSet cannot be null"
        Iterators.createIterator([resultSet: resultSet] as LinkedHashMap, "sql")
    }

    public static WrappedIterator fromDelimited(Iterators self, File file, String delimiter, Map options = [:]) {
        assert file: "file cannot be null"
        def result = fromDelimited(self, file.newInputStream(), delimiter)
        result.wrappedIterator.file = file

        return result
    }

    public static WrappedIterator fromDelimited(Iterators self, InputStream stream, String delimiter, Map options = [:]) {
        assert stream: "stream cannot be null"
        def params = [delimiter: delimiter, inputStream: stream] as LinkedHashMap
        params.putAll(options)
        Iterators.createIterator(params, "delimited")
    }

    public static WrappedIterator fromMaps(Iterators self, Map... data) {
        Iterators.toRecordIterator(data.toList())
    }

    public static WrappedIterator fromCsv(Iterators self, InputStream inputStream) {
        Iterators.createIterator([inputStream: inputStream] as LinkedHashMap, "csv")
    }

    public static WrappedIterator fromCsv(Iterators self, InputStream inputStream, List headers) {
        Iterators.createIterator([inputStream: inputStream, headers: headers] as LinkedHashMap, "csv")
    }

    public static WrappedIterator fromXml(Iterators self, InputStream inputStream, String tag) {
        Iterators.createIterator([inputStream: inputStream, tag: tag] as LinkedHashMap, "xml")
    }

    public static WrappedIterator fromXml(Iterators self, File file, String tag) {
        Iterators.createIterator([file: file, tag: tag] as LinkedHashMap, "xml")
    }

    public static WrappedIterator fromXml(Iterators self, InputStream inputStream, String tag, Map inheritedNamespaces) {
        Iterators.createIterator([inputStream: inputStream, tag: tag, inheritedNamespaces: inheritedNamespaces] as LinkedHashMap, "xml")
    }

    public static WrappedIterator fromXml(Iterators self, File file, String tag, Map inheritedNamespaces) {
        Iterators.createIterator([file: file, tag: tag, inheritedNamespaces: inheritedNamespaces] as LinkedHashMap, "xml")

    }

    public static WrappedIterator fromXml(Iterators self, InputStream inputStream, String tag, Map inheritedNamespaces, String charSet) {
        Iterators.createIterator([inputStream: inputStream, tag: tag, inheritedNamespaces: inheritedNamespaces, charSet: charSet] as LinkedHashMap, "xml")
    }

    public static WrappedIterator fromXml(Iterators self, File file, String tag, Map inheritedNamespaces, String charSet) {
        Iterators.createIterator([file: file, tag: tag, inheritedNamespaces: inheritedNamespaces, charSet: charSet] as LinkedHashMap, "xml")
    }
}
