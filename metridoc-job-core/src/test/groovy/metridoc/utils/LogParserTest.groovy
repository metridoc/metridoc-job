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

import org.junit.Test

class LogParserTest {

    @Test
    void "testing basic ordered parsing of logs"() {
        def logLine = 'blah [blammo] "foo\" bar\"baz" stuff"'
        def parser = [
                item1: /([^\s]+)\s/,
                item2: /\[([^\s]+)\]\s/,
                item6: /foobar/,
                item3: /"(((.+?)[^\\](\\")?)+?)"\s/,
                item4: /([^\s]+)/,
                item5: /blah/
        ]
        def logParser = new LogParser(regexMap: parser)
        def data = logParser.parseGetMatcher(logLine)
        assert data
        assert "blah" == data.item1.group(1)
        assert "blammo" == data.item2.group(1)
        assert 'foo" bar"baz' == data.item3.group(1)
        assert 'stuff"' == data.item4.group(1)
        assert null == data.item5
        assert null == data.item6

        data = logParser.parse(logLine)
        assert data
        assert "blah" == data.item1
        assert "blammo" == data.item2
        assert 'foo" bar"baz' == data.item3
        assert 'stuff"' == data.item4
        assert null == data.item5
        assert null == data.item6
    }
}
