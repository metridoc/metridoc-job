package metridoc.bd.entities

import grails.persistence.Entity

/**
 * Created by tbarker on 12/4/13.
 */
@Entity
class BdBibliography extends BaseBibliography{
    String oclcText

    static constraints = {
        def parentConstraint = BaseBibliography.constraints
        parentConstraint.delegate = delegate
        parentConstraint.call()
        oclcText(nullable: true, maxSize: 25)
    }
}
