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

import groovy.util.logging.Slf4j
import org.apache.poi.ss.usermodel.*

/**
 * @deprecated
 */
@Slf4j
class XlsIterator extends BaseExcelIterator {

    @Lazy(soft = true)
    Workbook workbook = { WorkbookFactory.create(getInputStream()) }()
    @Lazy(soft = true)
    Sheet sheet = { sheetName ? workbook.getSheet(sheetName) : workbook.getSheetAt(sheetIndex) }()

    @Lazy(soft = true)
    List headers = {
        def row = getRow(0)
        rowNum++

        return row
    }()

    int rowStart = 0
    @Lazy(soft = true)
    private Integer rowNum = { rowStart }()

    private static Object getCellValue(Cell cell) {

        if (cell == null) {
            return null
        }

        switch (cell.getCellType()) {

            case Cell.CELL_TYPE_STRING:
                return cell.richStringCellValue.getString()
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.dateCellValue
                } else {
                    return cell.numericCellValue
                }
                break;
            case Cell.CELL_TYPE_BOOLEAN:
                return cell.booleanCellValue
            case Cell.CELL_TYPE_FORMULA:
                return cell.cellFormula
            default:
                return null
        }
    }

    protected Record computeNext() {
        def result = [:]

        log.debug("retrieving record {} for sheet {}", rowNum, getSheet().sheetName)
        def row = getRow(rowNum)

        if (row == null) {
            return endOfData()
        }

        (0..headers.size() - 1).each {
            result[headers[it]] = null
            if (it < row.size() - 1) {
                result[headers[it]] = row[it]
            }
        }

        rowNum++
        return new Record(body: result)
    }

    private List getRow(int rowNumber) {
        def row = getSheet().getRow(rowNumber)

        if (row) {
            def lastCellIndex = row.lastCellNum
            def result = []

            (0..(lastCellIndex - 1)).each {
                def cell = row.getCell(it)
                result.add(getCellValue(cell))
            }

            return result
        }

        return null
    }
}
