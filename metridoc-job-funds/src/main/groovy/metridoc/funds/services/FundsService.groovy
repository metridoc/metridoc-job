package metridoc.funds.services

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import metridoc.core.Step
import metridoc.core.services.DefaultService
import metridoc.funds.entities.FundsList
import metridoc.funds.entities.FundsLoad
import metridoc.service.gorm.GormService

/**
 * Created by tbarker on 1/6/14.
 */
@Slf4j
class FundsService extends DefaultService {

    int ledgerId
    Sql sql_voyager
    Sql sql

    @Step(description = "drops tables")
    void dropTables() {
        sql.execute("drop table funds_list")
        sql.execute("drop table funds_load")
    }

    @Step(description = "runs the entire funds workflow", depends = ["createTables", "extractFunds", "createFundsList"])
    void runFunds() {}

    @Step(description = "creates tables if they don't already exist")
    void createTables() {
        includeService(GormService).enableFor(FundsList, FundsLoad)
    }

    @Step(description = "delete data for ledger_id")
    void deleteFundDataForLedger() {
        log.info "deleting data for ledger $ledgerId"
        sql.execute("delete from funds_load where ledger_id = " + ledgerId)
    }

    @Step(description = "extracts funds data for a ledger number", depends = ["createTables", "deleteFundDataForLedger"])
    void extractFunds() {
        String query = "SELECT 0 as repfund_id, '-' as repfund_name, FA.fund_id as alloc_id, replace(FA.fund_name,'''','\\''') as alloc_name, FS.fund_id as sumfund_id, replace(FS.fund_name,'''','\\''') as sumfund_name, replace(BM.bib_id,'''','\\''') as Bib_ID, replace(BT.title_brief,'''','\\''') as title, replace(BT.publisher,'''','\\''') as publisher, To_Char(INV.invoice_status_date, 'YYYY-MM-DD') as status, To_Char(INV.invoice_status_date, 'Mon') as Month, replace(V.vendor_code,'''','\\''') as vendor, ILIF.percentage as percent, ILI.quantity as quantity, replace(PO.po_number,'''','\\''') as po_no, round(((FA.original_allocation + FA.allocation_increase - FA.allocation_decrease)/100),0) as allo_net, round((ILI.quantity*(ILIF.amount/100)),0) as Cost, round((FA.commitments/100),0) as commit_total, round((FA.expenditures/100),0) as expend_total, round(((FA.original_allocation + FA.allocation_increase - FA.allocation_decrease - FA.commitments - FA.expenditures)/100),0) as available_bal, round((FA.expend_pending/100),0) as pending_expen, ILI.inv_line_item_id as inv_line_item_id FROM bib_master BM, bib_text BT, invoice INV, invoice_line_item ILI, invoice_line_item_funds ILIF, line_item LI, purchase_order PO, fund FA, fund FS, invoice_status INVSTAT, vendor V WHERE BM.bib_id = BT.bib_id and BM.bib_id = LI.bib_id and LI.line_item_id = ILI.line_item_id and LI.po_id = PO.po_id and ILI.invoice_id = INV.invoice_id and ILI.inv_line_item_id = ILIF.inv_line_item_id and INV.vendor_id = V.vendor_id and INV.invoice_status = INVSTAT.invoice_status and ILIF.ledger_id = FA.ledger_id and ILIF.fund_id = FA.fund_id and FA.ledger_id = FS.ledger_id and FA.parent_fund = FS.fund_id and FS.parent_fund IN (0,1) and INVSTAT.invoice_status IN (1,4) and ILIF.ledger_id = " + ledgerId +
                " UNION " +
                "SELECT FR.fund_id as repfund_id, replace(FR.fund_name,'''','\\''') as repfund_name, FA.fund_id as alloc_id, replace(FA.fund_name,'''','\\''') as alloc_Name, FS.fund_id as sumfund_id, replace(FS.fund_name,'''','\\''') as sumfund_name, replace(BM.bib_id,'''','\\''') as Bib_ID, replace(BT.title_brief,'''','\\''') as title , replace(BT.publisher,'''','\\''') as publisher, To_Char(INV.invoice_status_date, 'YYYY-MM-DD') as status, To_Char(INV.invoice_status_date, 'Mon') as Month, replace(V.vendor_code,'''','\\''') as vendor, ILIF.percentage as percent, ILI.quantity as quantity, replace(PO.po_number,'''','\\''') as po_no, round(((FA.original_allocation + FA.allocation_increase - FA.allocation_decrease)/100),0) as allo_net, round((ILI.quantity*(ILIF.amount/100)),0) as Cost, round((FA.commitments/100),0) as commit_total, round((FA.expenditures/100),0) as expend_total, round(((FA.original_allocation + FA.allocation_increase - FA.allocation_decrease - FA.commitments - FA.expenditures)/100),0) as available_bal, round((FA.expend_pending/100),0) as pending_expen, ILI.inv_line_item_id as inv_line_item_id FROM bib_master BM, bib_text BT, invoice INV, invoice_line_item ILI, invoice_line_item_funds ILIF, line_item LI, purchase_order PO, fund FR, fund FA, fund FS, invoice_status INVSTAT, vendor V WHERE BM.bib_id = BT.bib_id and INV.invoice_id = ILI.invoice_id and ILI.inv_line_item_id = ILIF.inv_line_item_id and ILI.line_item_id = LI.line_item_id and BM.bib_id = LI.bib_id and LI.po_id = PO.po_id and INV.vendor_id = V.vendor_id and INVSTAT.invoice_status = INV.invoice_status and FR.ledger_id = ILIF.ledger_id and FR.fund_id = ILIF.fund_id and FR.ledger_id = FA.ledger_id and FR.parent_fund = FA.fund_id and FA.ledger_id = FS.ledger_id and FA.parent_fund = FS.fund_id and FS.parent_fund IN (0,1) and INVSTAT.invoice_status IN (1,4) and ILIF.ledger_id = " + ledgerId +
                " UNION " +
                "SELECT FC.fund_id as repfund_id, replace(FC.fund_name,'''','\\''') as repfund_name, FA.fund_id as alloc_id, replace(FA.fund_name,'''','\\''') as alloc_Name, FS.fund_id as sumfund_id, replace(FS.fund_name,'''','\\''') as sumfund_name, replace(BM.bib_id,'''','\\''') as Bib_ID, replace(BT.title_brief,'''','\\''') as title, replace(BT.publisher,'''','\\''') as publisher, To_Char(INV.invoice_status_date, 'YYYY-MM-DD') as status, To_Char(INV.invoice_status_date, 'Mon') as Month, replace(V.vendor_code,'''','\\''') as vendor, ILIF.percentage as percent, ILI.quantity as quantity,  replace(PO.po_number,'''','\\''') as po_no, round(((FA.original_allocation + FA.allocation_increase - FA.allocation_decrease)/100),0) as allo_net, round((ILI.quantity*(ILIF.amount/100)),0) as Cost, round((FA.commitments/100),0) as commit_total, round((FA.expenditures/100),0) as expend_total, round(((FA.original_allocation + FA.allocation_increase - FA.allocation_decrease - FA.commitments - FA.expenditures)/100),0) as available_bal, round((FA.expend_pending/100),0) as pending_expen, ILI.inv_line_item_id as inv_line_item_id FROM bib_master BM, bib_text BT, invoice INV, invoice_line_item ILI, invoice_line_item_funds ILIF, line_item LI, purchase_order PO, fund FC, fund FR, fund FA, fund FS, invoice_status INVSTAT, vendor V WHERE BM.bib_id = BT.bib_id and INV.invoice_id = ILI.invoice_id and ILI.inv_line_item_id = ILIF.inv_line_item_id and ILI.line_item_id = LI.line_item_id and BM.bib_id = LI.bib_id and LI.po_id = PO.po_id and INV.vendor_id = V.vendor_id and INVSTAT.invoice_status = INV.invoice_status and FC.ledger_id = ILIF.ledger_id and FC.fund_id = ILIF.fund_id and FC.ledger_id = FR.ledger_id and FC.parent_fund = FR.fund_id and FR.ledger_id = FA.ledger_id and FR.parent_fund = FA.fund_id and FA.ledger_id = FS.ledger_id and FA.parent_fund = FS.fund_id and FS.parent_fund IN (0,1) and INVSTAT.invoice_status IN (1,4) and ILIF.ledger_id = " + ledgerId +
                " UNION " +
                "SELECT FC.fund_id as repfund_id, replace(FC.fund_name,'''','\\''') as repfund_name, FA.fund_id as alloc_id, replace(FA.fund_name,'''','\\''') as alloc_Name, FS.fund_id as sumfund_id, replace(FS.fund_name,'''','\\''') as sumfund_name, replace(BM.bib_id,'''','\\''') as Bib_ID, replace(BT.title_brief,'''','\\''') as title, replace(BT.publisher,'''','\\''') as publisher, To_Char(INV.invoice_status_date, 'YYYY-MM-DD') as status, To_Char(INV.invoice_status_date, 'Mon') as Month, replace(V.vendor_code,'''','\\''') as vendor, ILIF.percentage as percent, ILI.quantity as quantity,  replace(PO.po_number,'''','\\''') as po_no, round(((FA.original_allocation + FA.allocation_increase - FA.allocation_decrease)/100),0) as allo_net, round((ILI.quantity*(ILIF.amount/100)),0) as Cost, round((FA.commitments/100),0) as commit_total, round((FA.expenditures/100),0) as expend_total, round(((FA.original_allocation + FA.allocation_increase - FA.allocation_decrease - FA.commitments - FA.expenditures)/100),0) as available_bal, round((FA.expend_pending/100),0) as pending_expen, ILI.inv_line_item_id as inv_line_item_id FROM bib_master BM, bib_text BT, invoice INV, invoice_line_item ILI, invoice_line_item_funds ILIF, line_item LI, purchase_order PO, fund FC, fund FCP, fund FR, fund FA, fund FS, invoice_status INVSTAT, vendor V WHERE BM.bib_id = BT.bib_id and INV.invoice_id = ILI.invoice_id and ILI.inv_line_item_id = ILIF.inv_line_item_id and ILI.line_item_id = LI.line_item_id and BM.bib_id = LI.bib_id and LI.po_id = PO.po_id and INV.vendor_id = V.vendor_id and INVSTAT.invoice_status = INV.invoice_status and FC.ledger_id = ILIF.ledger_id and FC.fund_id = ILIF.fund_id and FC.ledger_id = FCP.ledger_id and FC.parent_fund = FCP.fund_id and FCP.ledger_id = FR.ledger_id and FCP.parent_fund = FR.fund_id and FR.ledger_id = FA.ledger_id and FR.parent_fund = FA.fund_id and FA.ledger_id = FS.ledger_id and FA.parent_fund = FS.fund_id and FS.parent_fund IN (0,1) and INVSTAT.invoice_status IN (1,4) and ILIF.ledger_id = " + ledgerId +
                " UNION " +
                "SELECT FC.fund_id as repfund_id, replace(FC.fund_name,'''','\\''') as repfund_name, FA.fund_id as alloc_id, replace(FA.fund_name,'''','\\''') as alloc_Name, FS.fund_id as sumfund_id, replace(FS.fund_name,'''','\\''') as sumfund_name, replace(BM.bib_id,'''','\\''') as Bib_ID, replace(BT.title_brief,'''','\\''') as title, replace(BT.publisher,'''','\\''') as publisher, To_Char(INV.invoice_status_date, 'YYYY-MM-DD') as status, To_Char(INV.invoice_status_date, 'Mon') as Month, replace(V.vendor_code,'''','\\''') as vendor, ILIF.percentage as percent, ILI.quantity as quantity,  replace(PO.po_number,'''','\\''') as po_no, round(((FA.original_allocation + FA.allocation_increase - FA.allocation_decrease)/100),0) as allo_net, round((ILI.quantity*(ILIF.amount/100)),0) as Cost, round((FA.commitments/100),0) as commit_total, round((FA.expenditures/100),0) as expend_total, round(((FA.original_allocation + FA.allocation_increase - FA.allocation_decrease - FA.commitments - FA.expenditures)/100),0) as available_bal, round((FA.expend_pending/100),0) as pending_expen, ILI.inv_line_item_id as inv_line_item_id FROM bib_master BM, bib_text BT, invoice INV, invoice_line_item ILI, invoice_line_item_funds ILIF, line_item LI, purchase_order PO, fund FC, fund FCP, fund FCGP, fund FR, fund FA, fund FS, invoice_status INVSTAT, vendor V WHERE BM.bib_id = BT.bib_id and INV.invoice_id = ILI.invoice_id and ILI.inv_line_item_id = ILIF.inv_line_item_id and ILI.line_item_id = LI.line_item_id and BM.bib_id = LI.bib_id and LI.po_id = PO.po_id and INV.vendor_id = V.vendor_id and INVSTAT.invoice_status = INV.invoice_status and FC.ledger_id = ILIF.ledger_id and FC.fund_id = ILIF.fund_id and FC.ledger_id = FCP.ledger_id and FC.parent_fund = FCP.fund_id and FCP.ledger_id = FCGP.ledger_id and FCP.parent_fund = FCGP.fund_id and FCGP.ledger_id = FR.ledger_id and FCGP.parent_fund = FR.fund_id and FR.ledger_id = FA.ledger_id and FR.parent_fund = FA.fund_id and FA.ledger_id = FS.ledger_id and FA.parent_fund = FS.fund_id and FS.parent_fund IN (0,1) and INVSTAT.invoice_status IN (1,4) and ILIF.ledger_id = " + ledgerId



        String base = "replace into funds_load (repfund_id, repfund_name, alloc_id, alloc_name, sumfund_id, sumfund_name, " +
                "bib_id, title, publisher, status, month, vendor, percent, quantity, po_no, allo_net, " +
                "cost, commit_total, expend_total, available_bal, pending_expen, ledger_id, inv_line_item_id) values ("
        int rowNum = 0
        sql_voyager.eachRow( query ) {
            row ->
                if (row != null) {
                    def ins = row.repfund_id + ", '"
                    ins += row.repfund_name + "', "
                    ins += row.alloc_id + ", '"
                    ins += row.alloc_name + "', "
                    ins += row.sumfund_id + ", '"
                    ins += row.sumfund_name + "', "
                    ins += row.bib_id + ", '"
                    ins += (row.title==null?'':row.title) + "', '"
                    ins += (row.publisher==null?'':row.publisher) + "', '"
                    ins += row.status + "', '"
                    ins += row.month + "', '"
                    ins += row.vendor + "', "
                    ins += row.percent + ", "
                    ins += row.quantity + ", '"
                    ins += row.po_no + "', "
                    ins += row.allo_net + ", "
                    ins += row.cost + ", "
                    ins += row.commit_total + ", "
                    ins += row.expend_total + ", "
                    ins += row.available_bal + ", "
                    ins += row.pending_expen + ", "
                    ins += ledgerId + ", "
                    ins += row.inv_line_item_id + ")"

                    def stmt = base + ins
                    sql.execute( stmt )
                    rowNum++
                }
                if ( rowNum%10000==0 ) log.info "$rowNum rows loaded"
        }
        log.info "Done for ledger $ledgerId"
    }

    @Step(description = "populates the funds list table", depends = ["createTables"])
    void createFundsList() {
        def query = """
        INSERT INTO funds_list (sumfund_id, sumfund_name)
            SELECT distinct sumfund_id, sumfund_name
            FROM funds_load fl WHERE fl.sumfund_id != 1
            ON DUPLICATE KEY UPDATE sumfund_name = fl.sumfund_name
        """
        log.info "Executing query: " + query
        sql.execute(query);
    }
}
