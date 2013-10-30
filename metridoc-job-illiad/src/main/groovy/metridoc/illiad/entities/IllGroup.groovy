package metridoc.illiad.entities

import grails.persistence.Entity

/**
 * @author Tommy Barker
 */
@Entity
class IllGroup {

    String groupName
    Integer groupNo

    static mapping = {
        version(defaultValue: '0')
    }

    static constraints = {
        groupNo(unique: true)
    }
}
