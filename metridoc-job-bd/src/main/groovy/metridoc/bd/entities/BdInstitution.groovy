package metridoc.bd.entities

import grails.persistence.Entity

/**
 * Created by tbarker on 12/11/13.
 */
@Entity
class BdInstitution {
    String catalogCode
    String institution
    Integer libraryId

    static constraints = {
        libraryId unique: true
        institution unique: true, maxSize: 64
        catalogCode maxSize: 1
    }

    static mapping = {
        version defaultValue: '0'
        catalogCode defaultValue: ''
    }
}
