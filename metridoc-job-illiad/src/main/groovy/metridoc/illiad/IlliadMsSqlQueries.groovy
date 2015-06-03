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

/**
 * Created with IntelliJ IDEA on 4/18/13
 * @author Tommy Barker
 */
class IlliadMsSqlQueries {

    def groupSqlStmt = "select distinct GroupNumber as group_no, GroupName as group_name from Groups"

    def groupLinkSqlStmt = "select distinct GroupNumber as group_no, LenderString as lender_code from GroupsLink"
    
    /*BillingCategory was split into ArticleBillingCategory and LoanBillingCategory. 
    These two values are identical in almost all cases, so only ArticleBillingCategory is selected to preserve our database structure*/
    def lenderAddrSqlStmt = { String lenderTableName ->
        "select distinct LenderString as lender_code, LibraryName as library_name, " +
                " ArticleBillingCategory as billing_category, address1+'; '+address2+'; '+address3+'; '+address4 as address " +
                " from ${lenderTableName}"
    }

    def referenceNumberSqlStmt = "select distinct i.TransactionNumber as transaction_number, i.OCLCNumber as oclc, " +
            " i.Type as ref_type, i.Data as ref_number from WorldCatInformation i, Transactions t " +
            " where t.TransactionNumber = i.TransactionNumber and t.TransactionStatus in ('Request Finished','Cancelled by ILL Staff')"

    def transactionSqlStmt = { String startDate ->
        "select TransactionNumber as transaction_number, " +
                " SUBSTRING(sys.fn_sqlvarbasetostr(HASHBYTES('MD5',UserName)),3,32) as user_id, RequestType as request_type, " +
                " LoanAuthor as loan_author, LoanTitle as loan_title, LoanPublisher as loan_publisher, LoanPlace as loan_location, " +
                " LoanDate as loan_date, LoanEdition as loan_edition, PhotoJournalTitle as photo_journal_title, " +
                " PhotoJournalVolume as photo_journal_volume, PhotoJournalIssue as photo_journal_issue, " +
                " PhotoJournalMonth as photo_journal_month, PhotoJournalYear as photo_journal_year, " +
                " PhotoJournalInclusivePages as photo_journal_inclusive_pages, PhotoArticleAuthor as photo_article_author, " +
                " PhotoArticleTitle as photo_article_title, CitedIn as cited_in, TransactionStatus as transaction_status, " +
                " TransactionDate as transaction_date, ISSN, ESPNumber as ESP_number, LendingString as lender_codes, " +
                " LendingLibrary as lending_library, ReasonForCancellation as reason_for_cancellation, CallNumber as call_number, " +
                " Location as location, ProcessType as process_type, SystemID as system_id, IFMCost as IFM_cost, " +
                " OriginalNVTGC as original_nvtgc, BorrowerNVTGC as borrower_nvtgc, " +
                " InProcessDate as in_process_date, BillingAmount as billing_amount from Transactions " +
                " where TransactionStatus in ('Request Finished','Cancelled by ILL Staff') and convert(varchar(11), TransactionDate, 112) >= '${startDate}'"
    }

    def lendingSqlStmt = { String startDate ->
        "select t2.TransactionNumber as transaction_number, t1.RequestType as request_type, t2.ChangedTo as status, min(t2.DateTime) as transaction_date " +
                " from Transactions t1 join Tracking t2 on t2.TransactionNumber = t1.TransactionNumber and t1.ProcessType = 'Lending' " +
                " where convert(varchar(11), t1.TransactionDate, 112) >= '${startDate}' and " +
                " (t1.RequestType = 'Article' and t2.ChangedTo in ('Awaiting Lending Request Processing','Request Finished','Request Conditionalized','Cancelled by ILL Staff') or " +
                " t1.RequestType = 'Loan' and t2.ChangedTo in ('Awaiting Lending Request Processing','Awaiting Mailing', 'Item Shipped','Request Conditionalized','Cancelled by ILL Staff')) " +
                " group by t2.TransactionNumber, t1.RequestType, t2.ChangedTo"
    }

    def borrowingSqlStmt = { String startDate ->
        "select t2.TransactionNumber as transaction_number, t1.RequestType as request_type, " +
                " t2.ChangedTo as transaction_status, min(t2.DateTime) as transaction_date " +
                " from Transactions t1 join Tracking t2 on t2.TransactionNumber=t1.TransactionNumber " +
                " where t1.ProcessType = 'Borrowing' and t1.TransactionStatus in ('Request Finished','Request Conditionalized','Cancelled by ILL Staff') and " +
                " t2.ChangedTo in ('Awaiting Copyright Clearance','Awaiting Request Processing','Request Sent','Awaiting Post Receipt Processing','Delivered to Web') and " +
                " convert(varchar(11), t1.TransactionDate, 112) >= '${startDate}' " +
                " group by t2.TransactionNumber, t1.RequestType, t2.ChangedTo " +
                " UNION " +
                " select h.TransactionNumber as transaction_number, t.RequestType as request_type, " +
                " 'Shipped' as transaction_status, min(h.DateTime) as transaction_date " +
                " from Transactions t join History h on h.TransactionNumber = t.TransactionNumber " +
                " where t.ProcessType = 'Borrowing' and t.TransactionStatus in ('Request Finished','Request Conditionalized','Cancelled by ILL Staff') and " +
                " h.UserName = 'System' and CHARINDEX('shipped', entry) > 0 and convert(varchar(11), t.TransactionDate, 112) >= '${startDate}' " +
                " group by h.TransactionNumber, t.RequestType"
    }

    def userSqlStmt = { String userTableName ->
        "select distinct substring(sys.fn_sqlvarbasetostr(hashbytes('MD5',UserName)),3,32) as user_id, Department, nvtgc " +
                "from ${userTableName} where UserName in (select UserName from Transactions)"
    }


    //TODO: All queries from here and below are actually MySql queries.  Should migrate to the MySql class
    def orderDateSqlStmt = "update ill_tracking t set order_date = " +
            " (select transaction_date from ill_borrowing l where l.transaction_number = t.transaction_number and " +
            " transaction_status = 'Request Sent') where order_date is null"

    def shipDateSqlStmt = "update ill_tracking t set ship_date = " +
            " (select transaction_date from ill_borrowing l where l.transaction_number = t.transaction_number and " +
            " transaction_status = 'Shipped') where ship_date is null"

    def receiveDateSqlStmt = "update ill_tracking t set receive_date = " +
            " (select transaction_date from ill_borrowing l where l.transaction_number = t.transaction_number and " +
            " transaction_status = 'Awaiting Post Receipt Processing') where receive_date is null"

    def articleReceiveDateSqlStmt = "update ill_tracking t set receive_date = " +
            " (select transaction_date from ill_borrowing l where l.transaction_number = t.transaction_number and " +
            " transaction_status = 'Delivered to Web') where receive_date is null"

    def arrivalDateSqlStmt = "insert into ill_lending_tracking (transaction_number, request_type, arrival_date) " +
            " select transaction_number, request_type, transaction_date " +
            " from ill_lending where status = 'Awaiting Lending Request Processing'"

    def completionSqlStmt = "update ill_lending_tracking t, ill_lending l " +
            " set completion_date = transaction_date, completion_status = status " +
            " where l.transaction_number = t.transaction_number and status " +
            " not in ('Awaiting Lending Request Processing','Cancelled by ILL Staff')"

    //makes sure that any shipped Loan takes precedence over any other completion time, should be run after [completionSqlStmt]
    def shipSqlStmt = "update ill_lending_tracking t, ill_lending l " +
            " set completion_date = transaction_date, completion_status = status " +
            " where l.transaction_number = t.transaction_number and status = 'Item Shipped'"

    def cancelledSqlStmt = "update ill_lending_tracking t, ill_lending l " +
            " set completion_date = transaction_date, completion_status = status " +
            " where l.transaction_number = t.transaction_number and status = 'Cancelled by ILL Staff'"
}
