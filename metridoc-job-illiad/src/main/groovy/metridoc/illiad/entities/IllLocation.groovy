package metridoc.illiad.entities

import grails.persistence.Entity

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
@Entity
class IllLocation {
    String location
    String abbrev

    static mapping = {
        version(defaultValue: '0')
        id(generator: "assigned")
    }

    static constraints = {
    }
}
