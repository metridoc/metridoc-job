/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */



package metridoc.writers

import metridoc.core.services.HibernateService
import metridoc.entities.MetridocRecordEntity
import metridoc.iterators.Iterators
import org.hibernate.Session
import spock.lang.Specification

import javax.persistence.Entity

import static metridoc.writers.WrittenRecordStat.Status.*

/**
 * Created with IntelliJ IDEA on 7/3/13
 * @author Tommy Barker
 */
class EntityIteratorWriterSpec extends Specification {

    def hService = new HibernateService(embeddedDataSource: true)

    void setup() {
        hService.enableFor(EntityHelper)
    }

    def "test basic entity writing workflow"() {
        when: "the data is written"
        def response = Iterators.fromMaps(
                [foo: "asd"],
                [foo: "sdf"],
                [foo: "fgd"],
                [foo: "dfgh"]
        ).toEntity(EntityHelper, hService)

        then: "appropriate data is returned"
        4 == response.aggregateStats[WRITTEN]
        0 == response.aggregateStats[IGNORED]
        0 == response.aggregateStats[ERROR]
        0 == response.aggregateStats[INVALID]

        hService.withTransaction { Session session ->
            4 == session.createQuery("from EntityHelper").list().size()
        }
    }

    def "test validation errors"() {
        given: "some invalid data"
        def rowIterator = Iterators.fromMaps(
                [foo: "asd"],
                [foo: "sdf"],
                [foo: "invalid"],
                [foo: "dfgh"]
        )

        when: "data is written"
        def writer = new EntityIteratorWriter(sessionFactory: hService.sessionFactory, recordEntityClass: EntityHelper)
        def response = writer.write(rowIterator)

        then: "three records are written and one is invalid"
        1 == response.invalidTotal
        3 == response.writtenTotal
        4 == response.getTotal()
    }

    def "test errors"() {
        given: "bad data"
        def rowIterator = Iterators.fromMaps(
                [foo: "asd"],
                [foo: "sdf"],
                [foo: "error"],
                [foo: "dfgh"]
        )

        when: "data is written"
        def writer = new EntityIteratorWriter(sessionFactory: hService.sessionFactory, recordEntityClass: EntityHelper)
        def response = writer.write(rowIterator)
        def throwables = response.fatalErrors

        then: "one error is recorded into the response"
        1 == response.errorTotal
        1 == response.getTotal()
        1 == throwables.size()
        throwables[0] instanceof RuntimeException
    }
}


@Entity
class EntityHelper extends MetridocRecordEntity {

    String foo

    @Override
    void validate() {
        assert foo != "invalid"
        if (foo == "error") {
            throw new RuntimeException("error")
        }
    }
}
