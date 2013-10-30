package metridoc.illiad.entities

import grails.persistence.Entity

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
@Entity
class IllUserInfo {

    String userId
    String rank
    String department
    String org
    String nvtgc

    static mapping = {
        version(defaultValue: '0')
    }

    static constraints = {
        rank(nullable: true)
        department(nullable: true)
        org(nullable: true)
        nvtgc(nullable: true)
        userId(unique: true)
    }
}
