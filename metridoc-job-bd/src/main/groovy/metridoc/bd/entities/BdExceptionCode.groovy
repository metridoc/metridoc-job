package metridoc.bd.entities

import grails.persistence.Entity

/**
 * Created by tbarker on 12/5/13.
 */
@Entity
class BdExceptionCode {
    String exceptionCode
    String exceptionCodeDesc

    static constraints = {
        exceptionCode(maxSize: 3, unique: true)
        exceptionCodeDesc(maxSize: 64, unique: true)
    }

}
