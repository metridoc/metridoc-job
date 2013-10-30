package metridoc.illiad.entities

import grails.persistence.Entity

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
@Entity
class IllLenderInfo {
    String lenderCode
    String libraryName
    String billingCategory
    String address

    static mapping = {
        version(defaultValue: '0')
    }
    static constraints = {
        libraryName(nullable: true)
        billingCategory(nullable: true)
        address(maxSize: 328, nullable: true)
    }
}
