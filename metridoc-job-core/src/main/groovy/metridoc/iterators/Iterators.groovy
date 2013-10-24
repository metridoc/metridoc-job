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

import metridoc.writers.*

/**
 * Created with IntelliJ IDEA on 5/31/13
 * @author Tommy Barker
 * @deprecated
 */
class Iterators {

    public static final ITERATORS = [
            delimited: DelimitedLineIterator,
            sql: SqlIterator,
            xls: XlsIterator,
            xlsx: XlsxIterator,
            csv: CsvIterator,
            xml: XmlIterator
    ]

    public static final WRITERS = [
            sql: DataSourceIteratorWriter,
            table: TableIteratorWriter,
            sqlTable: DataSourceIteratorWriter,
            guavaTable: TableIteratorWriter,
            entity: EntityIteratorWriter,
            split: SplitIteratorWriter
    ]

    static WrappedIterator toRecordIterator(Iterator iterator) {
        def iteratorToWrap = toRowIterator(iterator)
        new WrappedIterator(wrappedIterator: iteratorToWrap)
    }

    /**
     * please use {@link Iterators#toRowIterator(java.util.Iterator)}  instead
     *
     * @deprecated
     * @param iterator
     * @return
     */
    static RecordIterator toRowIterator(Iterator iterator) {
        assert iterator != null: "iterator must not be null"
        new RecordIterator() {
            @Override
            protected Record computeNext() {
                if (iterator.hasNext()) {
                    def next = iterator.next()
                    if (next instanceof Record) {
                        return next
                    }
                    assert next instanceof Map: "$next is neither a Record nor a Map"
                    return new Record(body: next)
                }

                return endOfData()
            }
        }
    }

    static WrappedIterator toRecordIterator(List<Map> iterator) {
        new WrappedIterator(wrappedIterator: toRowIterator(iterator))
    }

    /**
     * Please use {@link Iterators#toRecordIterator(java.util.List)}
     *
     * @deprecated
     * @param iterator
     * @return
     */
    static RecordIterator toRowIterator(List<Map> iterator) {
        assert iterator != null: "iterator must not be null"
        toRowIterator(iterator.iterator())
    }

    /**
     * @deprecated
     * @param iterator
     * @param filter
     * @return
     */
    static RecordIterator toFilteredRowIterator(RecordIterator iterator, Closure<Boolean> filter) {
        assert iterator: "iterator must not be null"
        assert filter: "filter must not be null"

        new FilteredRecordIterator(
                filter: filter,
                iterator: iterator
        )
    }

    /**
     * @deprecated
     * @param rowIterator
     * @param transformer should return null if it should not be collected, otherwise should return a Map
     * @return
     */
    static RecordIterator toFilteredAndTransformedIterator(RecordIterator rowIterator, Closure<Record> transformer) {
        new FilteredAndTransformedRecordIterator(
                iterator: rowIterator,
                transformer: transformer
        )
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    static WrappedIterator createIterator(LinkedHashMap properties, Class<RecordIterator> iteratorClass) {
        def iterator = iteratorClass.newInstance(properties)
        new WrappedIterator(wrappedIterator: iterator)
    }

    static WrappedIterator createIterator(LinkedHashMap properties, String name) {
        createIterator(properties, ITERATORS[name] as Class<RecordIterator>)
    }

    static IteratorWriter createWriter(LinkedHashMap properties, String name) {
        createWriter(properties, WRITERS[name] as Class<IteratorWriter>)
    }

    static IteratorWriter createWriter(LinkedHashMap properties, Class<IteratorWriter> writerClass) {
        writerClass.newInstance(properties)
    }

    static IteratorWriter createWriter(String name) {
        createWriter([:], WRITERS[name] as Class<IteratorWriter>)
    }
}

/**
 * @deprecated
 */
class WrappedIterator extends RecordIterator {
    RecordIterator wrappedIterator

    WrappedIterator filter(Closure closure) {
        Iterators.toRecordIterator(wrappedIterator.toStream().filter(closure))
    }

    WrappedIterator map(Closure closure) {
        Iterators.toRecordIterator(wrappedIterator.toStream().map { Record record ->
            def response = closure.call(record)
            if (response && response instanceof Record) {
                return response
            }
            return record
        })
    }

    WriteResponse writeTo(LinkedHashMap properties, Class<IteratorWriter> writerClass) {
        def writer = Iterators.createWriter(properties, writerClass)
        writer.write(wrappedIterator)
    }

    WriteResponse writeTo(LinkedHashMap properties, String name) {
        def writer = Iterators.createWriter(properties, name)
        writer.write(wrappedIterator)
    }

    WriteResponse writeTo(String name) {
        writeTo([:], name)
    }

    @Override
    protected Record computeNext() {
        if(wrappedIterator.hasNext()) {
            return wrappedIterator.next()
        }

        return endOfData()
    }
}

