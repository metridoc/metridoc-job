package metridoc.funds.entities

import grails.persistence.Entity

/**
 * Created by tbarker on 2/4/14.
 */
@Entity
class Vendor {
    String vendorCode
    String vendorName

    static constraints = {
        vendorCode maxSize: 20
        vendorName maxSize: 100
    }

    static mapping = {
        vendorCode index: "vendor_v_code_inx"
    }
}
