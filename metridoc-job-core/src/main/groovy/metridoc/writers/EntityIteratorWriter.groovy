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

import metridoc.entities.MetridocRecordEntity
import metridoc.iterators.Record
import metridoc.iterators.RecordIterator
import org.hibernate.SessionFactory

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 * @deprecated
 */
class EntityIteratorWriter extends DefaultIteratorWriter {

    SessionFactory sessionFactory
    Class<? extends MetridocRecordEntity> recordEntityClass

    @Override
    WriteResponse write(RecordIterator recordIterator) {
        def session = sessionFactory.currentSession
        def transaction = session.beginTransaction()
        def response
        try {
            response = super.write(recordIterator)
            if (response.errorTotal) {
                throw response.fatalErrors[0]
            }
            transaction.commit()
            return response
        }
        catch (Throwable e) {
            transaction.rollback()
            response = new WriteResponse()
            response.addError(e)

            return response
        }
        finally {
            if (session.isOpen()) {
                session.close()
            }
        }
    }

    @Override
    boolean doWrite(int lineNumber, Record record) {
        def instance = recordEntityClass.newInstance()
        record.headers.sessionFactory = sessionFactory
        if (instance.acceptRecord(record)) {
            instance.populate(record)
            if (instance.shouldSave()) {
                sessionFactory.currentSession.save(instance)
                return true
            }
        }

        return false
    }
}
