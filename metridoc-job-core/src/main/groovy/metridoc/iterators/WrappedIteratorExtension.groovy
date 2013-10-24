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



package metridoc.iterators

import metridoc.core.services.HibernateService
import metridoc.writers.WriteResponse
import org.hibernate.SessionFactory

import javax.sql.DataSource

/**
 * Created with IntelliJ IDEA on 9/11/13
 * @author Tommy Barker
 * @deprecated
 *
 */
class WrappedIteratorExtension {

    public static WriteResponse toDataSource(WrappedIterator iterator, DataSource dataSource, String tableName) {
        def writer = Iterators.createWriter([dataSource: dataSource, tableName: tableName], "sql")
        writer.write(iterator.wrappedIterator)
    }

    public static WriteResponse toGuavaTable(WrappedIterator iterator) {
        def writer = Iterators.createWriter("guavaTable")
        writer.write(iterator.wrappedIterator)
    }

    public static WriteResponse toEntity(WrappedIterator iterator, Class entity, SessionFactory sessionFactory) {
        Iterators.createWriter(sessionFactory: sessionFactory, recordEntityClass: entity, "entity").write(iterator.wrappedIterator)
    }

    public static WriteResponse toEntity(WrappedIterator iterator, Class entity, HibernateService hibernateService) {
        Iterators.createWriter(sessionFactory: hibernateService.sessionFactory, recordEntityClass: entity, "entity").write(iterator.wrappedIterator)
    }
}
