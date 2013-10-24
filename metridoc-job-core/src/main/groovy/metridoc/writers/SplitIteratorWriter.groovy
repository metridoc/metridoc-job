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

import metridoc.iterators.QueueBasedRecordIterator
import metridoc.iterators.QueueRecordIteratorManager
import metridoc.iterators.Record
import metridoc.iterators.RecordIterator

import javax.naming.OperationNotSupportedException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

import static metridoc.writers.WrittenRecordStat.Status.*

/**
 * Created with IntelliJ IDEA on 7/5/13
 * @author Tommy Barker
 * @deprecated
 */
class SplitIteratorWriter extends DefaultIteratorWriter {

    List<DefaultIteratorWriter> writers = []
    /**
     * {@inheritDoc}
     *
     * <p>as opposed to other writers, this is set to false by default for the split writer</p>
     */
    boolean logResult = false
    int threads = 10

    @Override
    WriteResponse write(RecordIterator recordIterator) {
        try {
            assert recordIterator: "record iterator should not be null or empty"
            def service = Executors.newFixedThreadPool(threads)
            List<QueueBasedRecordIterator> queueRowIterators = []
            List<Future<WriteResponse>> futures = []
            def iteratorManager = new QueueRecordIteratorManager(wrappedIterator: recordIterator)
            writers.each { writer ->
                def queueRowIterator = iteratorManager.createChild()
                queueRowIterators << queueRowIterator

                futures << service.submit({
                    writer.write(queueRowIterator)
                } as Callable)
            }

            //noinspection GroovyAssignabilityCheck
            iteratorManager.each {}

            def splitResponse = new SplitWriteResponse()
            futures.each {
                future ->
                    def response = future.get()
                    splitResponse.addResponse(response)
            }

            return splitResponse
        }
        finally {
            if (recordIterator instanceof Closeable) {
                silentClose(recordIterator)
            }
        }
    }

    @Override
    boolean doWrite(int lineNumber, Record record) {
        //do nothing, everything handled in write above
        throw new OperationNotSupportedException("not supported")
    }

}

/**
 * @deprecated
 */
class SplitWriteResponse extends WriteResponse {

    @Delegate
    private List<WriteResponse> responses = []

    void addResponse(WriteResponse writeResponse) {
        def stats = writeResponse.aggregateStats
        aggregateStats[ERROR] += stats[ERROR]
        aggregateStats[IGNORED] += stats[IGNORED]
        aggregateStats[WRITTEN] += stats[WRITTEN]
        aggregateStats[INVALID] += stats[INVALID]
        responses.add(writeResponse)
        if (writeResponse.fatalErrors) {
            this.fatalErrors.addAll(writeResponse.fatalErrors)
        }
    }
}