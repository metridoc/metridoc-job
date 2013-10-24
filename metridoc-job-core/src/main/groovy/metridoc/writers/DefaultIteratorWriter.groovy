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

import metridoc.iterators.Record
import metridoc.iterators.RecordIterator
import org.slf4j.LoggerFactory

import static metridoc.writers.WrittenRecordStat.Status.*

/**
 * Created with IntelliJ IDEA on 6/5/13
 * @author Tommy Barker
 * @deprecated
 */
abstract class DefaultIteratorWriter implements IteratorWriter<RecordIterator> {

    /**
     * if <code>true</code>, results are logged at <code>info</code> level, otherwise
     * <code>debug</code> level
     */
    boolean logResult = true
    /**
     * When logging, this name is used.  Defaults to the short class name
     */
    String name = this.getClass().simpleName
    int printAt = 10000

    WriteResponse write(RecordIterator recordIterator) {
        assert recordIterator != null: "record iterator cannot be null"
        def totals = new WriteResponse()
        def log = LoggerFactory.getLogger(this.getClass())
        if (!recordIterator.hasNext()) {
            log.warn "iterator does not have anymore values, there is nothing to write"
            return totals
        }
        try {
            def headers = recordIterator.recordHeaders //headers are persisted through the entire write
            Throwable throwable
            recordIterator.eachWithIndex { Record record, int lineNumber ->
                headers.putAll(record.headers)
                record.headers = headers
                def response
                if (!throwable) {
                    response = writeRecord(lineNumber, record)
                    response.each {
                        if (it.fatalError && it.status != INVALID) {
                            throwable = it.fatalError
                        }
                    }
                    if (throwable) {
                        log.error "the error [$throwable.message] occurred, no more records will be written"
                    }
                    handleResponse(response)
                    totals.addAll(response)
                    if (lineNumber % printAt == 0) {
                        log.info "processed $lineNumber records with stats $totals"
                    }
                }
            }

            totals.headers = headers

            if (logResult) {
                log.info("total records processed are: $totals.total")
                log.info("written results for ${name} are: $totals")
            }
            else {
                log.debug("written results for ${name} are: $totals")
            }
            return totals
        }
        finally {
            if (recordIterator instanceof Closeable) {
                silentClose(recordIterator as Closeable)
            }

            if (this instanceof Closeable) {
                silentClose(this as Closeable)
            }
        }
    }

    protected void handleResponse(List<WrittenRecordStat> writeResponses) {
        def log = LoggerFactory.getLogger(this.getClass())
        writeResponses.each { response ->
            switch (response.status) {
                case INVALID:
                    log.warn "" +
                            "Invalid record\n" +
                            "   --> line: $response.line\n" +
                            "   --> record: $response.record\n" +
                            "   --> message: $response.validationError.message\n" +
                            "   --> scope: $response.scope.simpleName"
                    break
                case ERROR:
                    log.error "" +
                            "Unexpected exception occurred processing record\n" +
                            "   --> line: $response.line\n" +
                            "   --> record: $response.record\n" +
                            "   --> message: $response.fatalError.message\n" +
                            "   --> scope: $response.scope.simpleName"
            }
        }
    }

    protected void silentClose(Closeable closeable) {
        try {
            closeable.close()
        }
        catch (Exception e) {
            def log = LoggerFactory.getLogger(this.getClass())
            log.warn "could not close $closeable properly, ignoring", e
        }
    }

    protected List<WrittenRecordStat> writeRecord(int line, Record record) {
        def response = new WrittenRecordStat(scope: this.getClass(), record: record, line: line)
        try {
            if (record.throwable) {
                throw record.throwable
            }
            boolean written = doWrite(line, record)
            if (written) {
                response.status = WRITTEN
            }
            else {
                response.status = IGNORED
            }
        }
        catch (AssertionError error) {
            response.status = INVALID
            response.validationError = error
        }
        catch (Throwable throwable) {
            response.status = ERROR
            response.fatalError = throwable
        }

        return [response]
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected void validateState(field, String message) {
        if (field == null) {
            throw new IllegalStateException(message)
        }
    }

    abstract boolean doWrite(int lineNumber, Record record)
}
