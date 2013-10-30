package metridoc.illiad

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
class IlliadMysqlQueries {

    def transactionCountsBorrowing = '''
                    select lg.group_no, g.group_name,
                    count(distinct t.transaction_number) transNum,
                    sum(billing_amount) as sumFees
                    from ill_transaction t
                        left join ill_lender_group lg on t.lending_library=lg.lender_code
                        left join ill_group g on lg.group_no=g.group_no
                        where t.process_type='Borrowing' and t.request_type=? and transaction_date between ? and ?
                        {add_condition}
                        group by group_no
    		'''

    def transactionCountsBorrowingAggregate = '''
                    select count(distinct t.transaction_number) transNum,
                    sum(billing_amount) as sumFees
                    from ill_transaction t
                        left join ill_lender_group lg on t.lending_library=lg.lender_code
                        left join ill_group g on lg.group_no=g.group_no
                        where t.process_type='Borrowing' and t.request_type=? and transaction_date between ? and ?
                        {add_condition}
            '''

    def transactionTurnaroundsBorrowing = '''
                    select lg.group_no,
                    AVG(bt.turnaround_shp_rec) as turnaroundShpRec,
                    AVG(bt.turnaround_req_shp) as turnaroundReqShp,
                    AVG(bt.turnaround_req_rec) as turnaroundReqRec
                    from ill_transaction t
                        left join ill_lender_group lg on t.lending_library=lg.lender_code
                        left join ill_tracking bt on t.transaction_number=bt.transaction_number
                        where t.process_type='Borrowing' and t.request_type=? and transaction_date between ? and ?
                        and request_date is not null and ship_date is not null and receive_date is not null
                        and transaction_status='Request Finished'
                        group by group_no
    		'''
    def transactionCountsLending = '''
                    select lg.group_no, g.group_name,
                    count(distinct t.transaction_number) transNum,
                    sum(billing_amount) as sumFees
                    from ill_transaction t
                        left join ill_lender_group lg on t.lending_library=lg.lender_code
                        left join ill_group g on lg.group_no=g.group_no
                        where t.process_type='Lending' and t.request_type=? and transaction_date between ? and ?
                        {add_condition}
                        group by group_no
    		'''

    def transactionCountsLendingAggregate = '''
                    select count(distinct t.transaction_number) transNum,
                    sum(billing_amount) as sumFees
                    from ill_transaction t
                        left join ill_lender_group lg on t.lending_library=lg.lender_code
                        left join ill_group g on lg.group_no=g.group_no
                        where t.process_type='Lending' and t.request_type=? and transaction_date between ? and ?
                        {add_condition}
            '''

    /* Need to get turnarounds for row Total separately, to avoid double counts
(because of joining with lending_group)*/
    def transactionTurnaroundsLending = '''
                    select lg.group_no,
                    AVG(lt.turnaround) as turnaround
                    from ill_transaction t
                        left join ill_lender_group lg on t.lending_library=lg.lender_code
                        left join ill_lending_tracking lt on t.transaction_number=lt.transaction_number
                        where t.process_type='Lending' and t.request_type=? and transaction_date between ? and ?
                        and lt.completion_date is not null and lt.arrival_date is not null
                        and transaction_status='Request Finished'
                        group by group_no
    		'''

    def transactionTotalTurnaroundsBorrowing = '''
                    select AVG(bt.turnaround_shp_rec) as turnaroundShpRec,
                    AVG(bt.turnaround_req_shp) as turnaroundReqShp,
                    AVG(bt.turnaround_req_rec) as turnaroundReqRec
                    from ill_transaction t
                        left join ill_tracking bt on t.transaction_number=bt.transaction_number
                        where t.process_type='Borrowing' and t.request_type=? and transaction_date between ? and ?
                        and transaction_status='Request Finished' and request_date is not null and ship_date is not null and receive_date is not null
    		'''

    def transactionTotalTurnaroundsLending = '''
                    select AVG(lt.turnaround) as turnaround
                    from ill_transaction t
                        left join ill_lending_tracking lt on t.transaction_number=lt.transaction_number
                        where t.process_type='Lending' and t.request_type=? and transaction_date between ? and ?
                        and transaction_status='Request Finished' and lt.completion_date is not null and lt.arrival_date is not null
    		'''

    def selectAllFromIllTransaction = { String type, boolean isBorrowing ->
        def processType = isBorrowing ? "Borrowing" : "Lending"
        """
            select *
            from ill_transaction
            where process_type = ${processType}
                and request_type= ${type}
        """
    }

}
