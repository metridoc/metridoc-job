package metridoc.illiad.entities

import grails.persistence.Entity

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
@Entity
class IllLenderGroup {
    Integer groupNo
    String lenderCode
    Integer demographic

    static mapping = {
        version(defaultValue: '0')
        lenderCode(index: "idx_ill_lender_group_lender_code")
    }

    static constraints = {
        demographic(nullable: true)
    }
}
