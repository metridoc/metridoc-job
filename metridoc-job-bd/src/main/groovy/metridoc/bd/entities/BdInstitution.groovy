package metridoc.bd.entities

import grails.persistence.Entity

/**
 * Created by tbarker on 12/5/13.
 */
@Entity
class BdInstitution {
    String catalogCode
    String institution
    Long libraryId


    static constraints = {
        catalogCode(maxSize: 1, unique: true)
        institution(maxSize: 64, unique: true)
        libraryId(unique: true)
    }
}
