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

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.xssf.model.SharedStringsTable
import org.apache.poi.xssf.usermodel.XSSFRichTextString
import org.slf4j.LoggerFactory

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader

/**
 * Created with IntelliJ IDEA on 10/22/13
 * @author Tommy Barker
 */
class XlsxStream extends BaseExcelStream {
    private static log = LoggerFactory.getLogger(XlsxStream)
    public static final String ROW = "row"

    @Lazy
    XMLStreamReader reader = {
        def sheet = xssfReader.getSheet(sheetReference)
        XMLInputFactory.newInstance().createXMLStreamReader(sheet)
    }()
    @Lazy
    XSSFReader xssfReader = { new XSSFReader(OPCPackage.open(inputStream)) }()
    @Lazy
    SharedStringsTable stringLookup = { xssfReader.sharedStringsTable }()
    @Lazy
    String sheetReference = {
        if (sheetName) {
            return getSheetReferenceByName(getWorkbookReader(), sheetName)
        }

        return getSheetReferenceByIndex(getWorkbookReader(), sheetIndex)
    }()

    @Lazy(soft = true)
    List headers = { getRow(reader).collect { it.getFormattedValue() } }()
    @Lazy
    XMLStreamReader workbookReader = {
        def workbook = xssfReader.workbookData
        return XMLInputFactory.newInstance().createXMLStreamReader(workbook)
    }()

    private static closeXmlStreamReader(XMLStreamReader xmlReader) {
        try {
            xmlReader.close()
        }
        catch (Exception e) {
            log.warn("An exception occurred closing the xml reader", e)
        }
    }

    private List<XlsxCell> getNextRow(XMLStreamReader reader) {
        getToNextRow(reader)

        if (reader.hasNext()) {
            return getRow(reader)
        }

        return null
    }

    private static void getToNextRow(XMLStreamReader reader) {
        while (!atRowOrEnd(reader)) {
            reader.next()
        }
    }

    private static boolean atRowOrEnd(XMLStreamReader reader) {
        boolean atEnd = !reader.hasNext()
        boolean atRow = false
        if (reader.startElement) {
            atRow = reader.localName == ROW
        }
        return atEnd || atRow
    }

    private static Map<String, String> getAttributeMap(XMLStreamReader reader) {
        int attributeCount = reader.attributeCount
        def result = [:]
        for (int i = 0; i < attributeCount; i++) {
            result.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i))
        }

        return result
    }

    static String getSheetReference(XMLStreamReader workbookReader, Closure closure) {
        def run = true

        try {
            while (run) {
                if (workbookReader.startElement) {
                    String localName = workbookReader.localName
                    if (localName == "sheet") {
                        def attributeMap = getAttributeMap(workbookReader)
                        String result = closure.call(attributeMap)
                        if (result) {
                            return result
                        }
                    }
                }

                boolean hasNext = workbookReader.hasNext()
                if (!hasNext) {
                    run = false
                } else {
                    workbookReader.next()
                }
            }
        }
        finally {
            closeXmlStreamReader(workbookReader)
        }
        return null
    }

    private static String getSheetReferenceByName(XMLStreamReader workbookReader, String name) {
        return getSheetReference(workbookReader) { Map attributeMap ->
            def sheetName = attributeMap.name
            if (sheetName == name) {
                return attributeMap.id
            }
        }
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    private static String getSheetReferenceByIndex(XMLStreamReader workbookReader, int index) {
        return getSheetReference(workbookReader) { Map attributeMap ->
            def oneBaseIndex = index + 1
            def sheetId = Integer.valueOf(attributeMap.sheetId)
            if (oneBaseIndex == sheetId) {
                return attributeMap.id
            }
        }
    }

    @SuppressWarnings("GroovyVariableNotAssigned")
    private List<XlsxCell> getRow(XMLStreamReader reader) {
        boolean gettingCells = true
        def result = []

        def cell
        while (gettingCells) {
            if (reader.startElement) {
                def name = reader.localName

                switch (name) {
                //c comes before v, so XlsxCell should be instantiated
                    case "c":
                        cell = new XlsxCell(stringLookup: getStringLookup())
                        def attributes = getAttributeMap(reader)
                        cell.reference = attributes.r
                        cell.type = attributes.t
                        break
                    case "v":
                        def cellValue = Double.valueOf(reader.getElementText())
                        cell.value = cellValue
                        result.add(cell)
                        break
                }
            }

            if (reader.endElement) {
                def name = reader.localName
                if (name == ROW) {
                    gettingCells = false
                }
            }
            reader.next()
        }

        return result
    }

    @Override
    protected Map computeNext() {
        def headerSize = headers.size()
        def row = getNextRow(getReader())
        def result = [:]
        if (row) {
            def data = row.collect { it.getFormattedValue() }
            (0..headerSize - 1).each {
                result[headers[it]] = data[it]
            }
            return result
        }

        close()
        return endOfData()
    }
}

class XlsxCell {
    String reference
    String type
    double value
    SharedStringsTable stringLookup

    def getFormattedValue() {
        def result = value
        if (type == "s") {
            int reference = value
            def entry = stringLookup.getEntryAt(reference)
            result = new XSSFRichTextString(entry).toString();
        }

        return result
    }

    int getColumnIndex() {
        XlsxIterator.convertColumnToNumber(reference)
    }
}

