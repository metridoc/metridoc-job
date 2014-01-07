package metridoc.funds.entities

import grails.persistence.Entity

/**
 * Created by tbarker on 1/2/14.
 */
@Entity
class FundsList {
    Long sumfundId
    String sumfundName

    static constraints = {
        sumfundName nullable: true, maxSize: 32
    }

    static mapping = {
        id name: "sumfundId"
        version false
    }
}
