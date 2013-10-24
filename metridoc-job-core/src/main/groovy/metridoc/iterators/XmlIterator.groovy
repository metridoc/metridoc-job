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

import groovy.util.slurpersupport.GPathResult
import org.apache.camel.support.TokenXMLPairExpressionIterator
import groovy.util.slurpersupport.Node

/**
 * Built to convert large xml docs into an iterator
 * @author Tommy Barker
 * @deprecated
 */
class XmlIterator extends FileIterator {

    /**
     * beginning and end tag for an xml "record"
     */
    String tag
    String charSet = "utf-8"
    String inheritNamespaceToken
    Map namespaces = [:]
    private Iterator<String> xmlTokenPairIterator

    String getStartTag() {
        return "<$tag>"
    }

    String getEndTag() {
        return "</$tag>"
    }

    @Override
    protected Record computeNext() {
        if (!xmlTokenPairIterator) {
            initializeCamelIterator()
        }

        if (xmlTokenPairIterator.hasNext()) {
            def next = xmlTokenPairIterator.next()
            return convertToRecord(next)
        }

        endOfData()
    }

    Record convertToRecord(String xmlText) {
        GPathResult xmlResult
        if (namespaces) {
            xmlResult = new XmlSlurper().parseText(xmlText).declareNamespace(namespaces)
        }
        else {
            xmlResult = new XmlSlurper().parseText(xmlText)
        }
        Record record = new Record()
        xmlResult.childNodes().each {Node child ->
            record.body[child.name()] = child
        }
        record.body.root = xmlResult

        return record
    }

    void initializeCamelIterator() {
        assert tag: "tag must not be empty or null"
        assert inputStream: "file or stream has not been set"
        charSet = charSet ?: "utf-8" //just in case the user accidentally set it as null
        def iteratorCreator = new TokenXMLPairExpressionIterator(startTag, endTag, inheritNamespaceToken)
        xmlTokenPairIterator = iteratorCreator.createIterator(inputStream, charSet)
    }
}
