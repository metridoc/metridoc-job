package metridoc.funds.entities

import grails.persistence.Entity

/**
 * Created by tbarker on 1/2/14.
 */
@Entity
class FundsLoad {
    Integer allocId
    Integer ledgerId
    Integer invLineItemId
    Long loadId
    Integer repfundId
    String repfundName
    String allocName
    String sumfundName
    Integer sumfundId
    Integer bibId
    String title
    String publisher
    String status
    String month
    String vendor
    Double percent
    Integer quantity
    String poNo
    Integer alloNet
    Double cost
    Double commitTotal
    Double expendTotal
    Double availableBal
    Double pendingExpen
    Date loadTime

    static mapping = {
        id name: "loadId"
        version false
        loadTime sqlType: "timestamp", defaultValue: "CURRENT_TIMESTAMP"
        status index: "funds_load_status_inx"
        vendor index: "funds_load_vendor_inx"
        ledgerId index: "funds_load_ledger_id_inx"
    }

    static constraints = {
        repfundId nullable: true
        repfundName nullable: true, maxSize: 32
        allocName nullable: true, maxSize: 32
        sumfundName nullable: true, maxSize: 32
        sumfundId nullable: true
        bibId nullable: true
        title nullable: true
        publisher nullable: true, maxSize: 200
        status nullable: true, maxSize: 32
        month nullable: true, maxSize: 3
        vendor nullable: true, maxSize: 32
        percent nullable: true
        quantity nullable: true
        poNo nullable: true, maxSize: 32
        alloNet nullable: true
        cost nullable: true
        commitTotal nullable: true
        expendTotal nullable: true
        availableBal nullable: true
        pendingExpen nullable: true
        allocId nullable: true
        ledgerId nullable: true
        invLineItemId nullable: true
    }
}
