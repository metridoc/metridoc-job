/*
  *Copyright 2013 Trustees of the University of Pennsylvania. Licensed under the
  *	Educational Community License, Version 2.0 (the "License"); you may
  *	not use this file except in compliance with the License. You may
  *	obtain a copy of the License at
  *
  *http://www.osedu.org/licenses/ECL-2.0
  *
  *	Unless required by applicable law or agreed to in writing,
  *	software distributed under the License is distributed on an "AS IS"
  *	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  *	or implied. See the License for the specific language governing
  *	permissions and limitations under the License.
  */

package metridoc.illiad

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import metridoc.illiad.entities.IllCache
import metridoc.illiad.entities.IllGroup

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
@Slf4j
class IlliadHelper {

    IlliadService illiadTool

    static final int GROUP_ID_OTHER = -2
    static final int GROUP_ID_TOTAL = -1

    def storeCache() {
        def data = [
                basicStatsData: getBasicStatsData(null),
                groups: getGroupList()
        ]
        log.info("The following data will be stored in the cache: ${data.toString()}");
        IllCache.update(data)
    }

    def getBasicStatsData(fiscalYear) {
        def dataSource = illiadTool.dataSource
        Sql sql = new Sql(dataSource);
        def result = ['books': [:], 'articles': [:]];
        def reportFiscalYear = fiscalYear != null ? fiscalYear : DateUtil.getCurrentFiscalYear();

        Date fiscalYearStart = DateUtil.getFiscalYearStartDate(reportFiscalYear)
        Date fiscalYearEnd = DateUtil.getFiscalYearEndDate(reportFiscalYear)
        result.books.borrowing = loadSectionData(sql, true, true, fiscalYearStart, fiscalYearEnd);
        result.books.lending = loadSectionData(sql, true, false, fiscalYearStart, fiscalYearEnd);
        result.articles.borrowing = loadSectionData(sql, false, true, fiscalYearStart, fiscalYearEnd);
        result.articles.lending = loadSectionData(sql, false, false, fiscalYearStart, fiscalYearEnd);
        return result;
    }

    def getGroupList() {
        def result = []
        IllGroup.list().each {
            result.add([groupName:it.groupName, groupNo: it.groupNo])
        }

        return result
    }

    def loadSectionData(sql, isBooks, isBorrowing, startDate, endDate) {

        log.info("loading sectional data with isBooks: $isBooks, isBorrowing: $isBorrowing, startDate: $startDate, endDate: $endDate")

        def pickQuery = { borrowingQuery, lendingQuery ->
            isBorrowing ? borrowingQuery : lendingQuery
        }

        def toIlliadSqlStatements = illiadTool.toIlliadSqlStatements
        def genQuery = pickQuery(toIlliadSqlStatements.transactionCountsBorrowing, toIlliadSqlStatements.transactionCountsLending)
        def genQueryAgg = pickQuery(toIlliadSqlStatements.transactionCountsBorrowingAggregate, toIlliadSqlStatements.transactionCountsLendingAggregate)
        def turnaroundQuery = pickQuery(toIlliadSqlStatements.transactionTotalTurnaroundsBorrowing, toIlliadSqlStatements.transactionTotalTurnaroundsLending)
        def turnaroundPerGroupQuery = pickQuery(toIlliadSqlStatements.transactionTurnaroundsBorrowing, toIlliadSqlStatements.transactionTurnaroundsLending)

        def requestType = isBooks ? 'Loan' : 'Article';

        def result = [:];

        def sqlParams = [requestType, startDate, endDate];
        String queryFilled = getAdjustedQuery(genQuery,
                ['add_condition': ' and transaction_status=\'Request Finished\'']);

        String queryExhausted = getAdjustedQuery(genQuery,
                ['add_condition': ' and not (transaction_status<=>\'Request Finished\')']);

        String queryFilledAgg = getAdjustedQuery(genQueryAgg,
                ['add_condition': ' and transaction_status=\'Request Finished\'']);

        String queryExhaustedAgg = getAdjustedQuery(genQueryAgg,
                ['add_condition': ' and not (transaction_status is not null and transaction_status = \'Request Finished\')']);

        profile("Running query for filledQueries (borrowing=${isBorrowing}, book=${isBooks}): " + queryFilled + " params=" + sqlParams) {
            sql.eachRow(queryFilled, sqlParams, {
                int groupId = it.getAt(0)
                def groupData = getGroupDataMap(groupId, result)
                groupData.filledRequests = it.transNum
                groupData.sumFees = it.sumFees != null ? it.sumFees : 0
            })

            def row = sql.firstRow(queryFilledAgg, sqlParams)
            def groupData = getGroupDataMap(GROUP_ID_TOTAL, result)
            groupData.filledRequests = row.transNum
            groupData.sumFees = row.sumFees != null ? row.sumFees : 0
        }

        profile("Running query for turnaroundPerGroupQuery (borrowing=${isBorrowing}, book=#{isBook}): " + turnaroundPerGroupQuery + " params=" + sqlParams) {
            sql.eachRow(turnaroundPerGroupQuery, sqlParams, {
                int groupId = it.getAt(0);
                def groupData = getGroupDataMap(groupId, result)
                setTurnarounds(isBorrowing, groupData, it)
            })
        }

        profile("Running query for turnaroundQuery (total) (borrowing=${isBorrowing}, book=#{isBook}): " + turnaroundQuery + " params=" + sqlParams) {
            def totalGroupTurnarounds = sql.firstRow(turnaroundQuery, sqlParams);
            setTurnarounds(isBorrowing, getGroupDataMap(GROUP_ID_TOTAL, result), totalGroupTurnarounds)
        }


        profile("Running query for exhausted requests (borrowing=${isBorrowing}, book=#{isBook}): " + queryExhausted + " params=" + sqlParams) {
            sql.eachRow(queryExhausted, sqlParams, {
                int groupId = it.getAt(0)
                def groupData = getGroupDataMap(groupId, result)
                groupData.exhaustedRequests = it.transNum
                groupData.sumFees += it.sumFees != null ? it.sumFees : 0
            })
            def row = sql.firstRow(queryExhaustedAgg, sqlParams)
            def groupData = getGroupDataMap(GROUP_ID_TOTAL, result)
            groupData.exhaustedRequests = row.transNum
            groupData.sumFees += row.sumFees != null ? row.sumFees : 0
        }


        return result
    }

    private String getAdjustedQuery(query, stringMap) {
        String result = query;
        stringMap.each() { key, value ->
            result = result.replaceAll("\\{${key}\\}", value)
        };
        return result;
    }

    private getGroupDataMap(groupId, container) {
        if (container.get(groupId) == null) {
            container.put(groupId, ['filledRequests': 0, 'sumFees': 0, 'exhaustedRequests': 0]);
        }
        return container.get(groupId)
    }

    private void setTurnarounds(isBorrowing, groupData, srcRow) {
        if (isBorrowing) {
            groupData.turnaroundShpRec = srcRow.turnaroundShpRec
            groupData.turnaroundReqShp = srcRow.turnaroundReqShp
            groupData.turnaroundReqRec = srcRow.turnaroundReqRec
        } else {
            groupData.turnaround = srcRow.turnaround
        }
    }

    def profile(String message, Closure closure) {
        log.info "Profiling: [${message}] START"
        def start = new Date().time
        closure.call()
        def end = new Date().time
        log.info "Profiling: [${message}] END took ${end - start} ms"
    }
}
