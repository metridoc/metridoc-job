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

import org.apache.commons.io.LineIterator

/**
 * Created with IntelliJ IDEA on 10/22/13
 * @author Tommy Barker
 */
class DelimitedLineStream extends FileStream<Map> {
    @Lazy(soft = true)
    Reader reader = { new InputStreamReader(inputStream) }()

    @Lazy
    LineIterator lineIterator = { new LineIterator(getReader()) }()


    /**
     * if set to true, a missmatch between header count and split line count will throw assertion error,
     * otherwise IllegalStateException
     */
    boolean dirtyData = false

    /**
     * if headers are not specified, column numbers are used
     */
    List headers
    /**
     * regex of the delimiter, for instance if you split text with | you would pass /\|/
     */
    String delimiter

    @Lazy(soft = true)
    Integer delimitTill = { headers ? headers.size() : 0 }()

    @Override
    void close() {
        getLineIterator().close()
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    @Override
    protected Map computeNext() {
        if (delimiter == null) {
            throw new IllegalArgumentException("delimiter must be specified")
        }

        if (getLineIterator().hasNext()) {
            def line = getLineIterator().nextLine()
            def splitLine
            if (delimitTill) {
                splitLine = line.split(delimiter, delimitTill)
            } else {
                splitLine = line.split(delimiter)
            }

            def result = [:]
            if (headers) {
                if (headers.size() != splitLine.size()) {
                    def errorMessage = "headers $headers and line $splitLine do not have the same number of arguments, \n" +
                            "headers count = ${headers.size()}\n"
                    "splitLine count = ${splitLine.size()}"
                    if(dirtyData) {
                        assert headers.size() == splitLine.size() : errorMessage
                    }

                    throw new IllegalStateException(errorMessage)
                }
            } else {
                headers = []
                (0..splitLine.size() - 1).each {
                    headers << it
                }
            }

            (0..splitLine.size() - 1).each {
                result[headers[it]] = splitLine[it]
            }

            return result
        }

        close()
        return endOfData()
    }
}
