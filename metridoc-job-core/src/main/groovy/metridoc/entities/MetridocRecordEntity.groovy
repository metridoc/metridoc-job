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



package metridoc.entities

import metridoc.core.RecordLoader
import metridoc.iterators.Record

import javax.persistence.MappedSuperclass

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
@MappedSuperclass
abstract class MetridocRecordEntity extends MetridocEntity implements RecordLoader {
    abstract void validate()

    boolean acceptRecord(Record record) {
        return true
    }

    boolean shouldSave() {
        return true
    }

    void populate(Record record) {
        def dataOfInterest = record.body.findAll { this.properties.keySet().contains(it.key) }
        try {
            dataOfInterest.each {
                this."$it.key" = it.value
            }
            validate()
        }
        catch (ClassCastException e) {
            throw new AssertionError("Cast error setting values", e)
        }
    }
}
