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

import au.com.bytecode.opencsv.CSVReader

/**
 * Created with IntelliJ IDEA on 10/22/13
 * @author Tommy Barker
 */
class CsvStream extends FileStream<Map> {
    @Lazy(soft = true)
    CSVReader csvReader = { new CSVReader(new InputStreamReader(inputStream)) }()

    @Lazy(soft = true)
    List headers = { csvReader.readNext() as List }()

    @Override
    protected Map computeNext() {

        def headersSize = headers.size()
        def next = csvReader.readNext()

        if (next != null && next.size() != headersSize) {
            def errorMessage = "headers ${headers} and result ${next} do not have the same number of arguments"
            throw new IllegalStateException(errorMessage)
        }

        def result = [:]

        if (next) {
            (0..next.size() - 1).each {
                result[headers[it]] = next[it]
            }
        }

        if (next == null) {
            csvReader.close()
            return endOfData()
        }

        return result
    }
}
