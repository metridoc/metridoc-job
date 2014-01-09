package metridoc.bd.entities

import grails.persistence.Entity

/**
 * Created by tbarker on 1/9/14.
 */
@Entity
class BdPatronType {
    String patronType
    String patronTypeDesc

    static constraints = {
        patronType(maxSize: 1, unique: true)
        patronTypeDesc(maxSize: 50)
    }

    static mapping = {
        version(false)
    }
}
