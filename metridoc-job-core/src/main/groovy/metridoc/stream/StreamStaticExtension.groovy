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



package metridoc.stream

import groovy.stream.Stream
import metridoc.iterators.Iterators

import java.sql.ResultSet

/**
 * Created with IntelliJ IDEA on 10/22/13
 * @author Tommy Barker
 */
class StreamStaticExtension {
    public static Stream<Map> fromResultSet(Stream self, ResultSet resultSet) {
        assert resultSet != null: "resultSet cannot be null"
        new ResultSetStream(resultSet: resultSet).toStream()
    }

    public static Stream<Map> fromDelimited(Stream self, File file, String delimiter) {
        fromDelimited(self, file, delimiter, [:])
    }

    public static Stream<Map> fromDelimited(Stream self, File file, String delimiter, Map options) {
        assert file: "file cannot be null"
        return fromDelimited(self, file.newInputStream(), delimiter, options)
    }

    public static Stream<Map> fromDelimited(Stream self, InputStream stream, String delimiter) {
        fromDelimited(self, stream, delimiter, [:])
    }

    public static Stream<Map> fromDelimited(Stream self, InputStream stream, String delimiter, Map options) {
        assert stream: "stream cannot be null"
        assert delimiter: "delimiter cannot be null"
        def params = [delimiter: delimiter, inputStream: stream] as LinkedHashMap
        params.putAll(options)
        new DelimitedLineStream(params).toStream()
    }

    public static Stream<Map> fromXls(Stream self, File file) {
        fromXls(self, file, [:])
    }

    public static Stream<Map> fromXls(Stream self, File file, Map options) {
        fromXls(self, file.newInputStream(), options)
    }

    public static Stream<Map> fromXls(Stream self, InputStream inputStream) {
        fromXls(self, inputStream, [:])
    }

    public static Stream<Map> fromXls(Stream self, InputStream inputStream, Map options) {
        def params = [inputStream: inputStream]
        params.putAll(options)
        new XlsStream(params).toStream()
    }

    public static Stream<Map> fromCsv(Stream self, File file) {
        fromCsv(self, file, [])
    }

    public static Stream<Map> fromCsv(Stream self, File file, List headers) {
        fromCsv(self, file.newInputStream(), headers)
    }

    public static Stream<Map> fromCsv(Stream self, InputStream inputStream) {
        fromCsv(self, inputStream, [])
    }

    public static Stream<Map> fromCsv(Stream self, InputStream inputStream, List headers) {
        def stream = new CsvStream(inputStream: inputStream)
        if(headers) {
            stream.headers = headers
        }

        stream.toStream()
    }

    public static Stream<Map> fromXlsx(Stream self, File file) {
        fromXlsx(self, file, [:])
    }

    public static Stream<Map> fromXlsx(Stream self, File file, Map options) {
        fromXlsx(self, file.newInputStream(), options)
    }

    public static Stream<Map> fromXlsx(Stream self, InputStream inputStream) {
        fromXlsx(self, inputStream, [:])
    }

    public static Stream<Map> fromXlsx(Stream self, InputStream inputStream, Map options) {
        def params = [inputStream: inputStream]
        params.putAll(options)
        new XlsxStream(params).toStream()
    }

    public static Stream<Map> fromXml(Stream self, File file, String tag) {
        fromXml(self, file, tag, [:])
    }

    public static Stream<Map> fromXml(Stream self, File file, String tag, Map options) {
        fromXml(self, file.newInputStream(), tag, options)
    }

    public static Stream<Map> fromXml(Stream self, InputStream inputStream, String tag) {
        fromXml(self, inputStream, tag, [:])
    }

    public static Stream<Map> fromXml(Stream self, InputStream inputStream, String tag, Map options) {
        def params = [inputStream: inputStream, tag: tag]
        params.putAll(options)
        new XmlStream(params).toStream()
    }
}
