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

package metridoc.writers;

import java.util.Iterator;

/**
 * @deprecated
 * @param <T>
 */
public interface IteratorWriter<T extends Iterator> {
    /**
     * Should iterate over the iterator and write all the records.  With the exception of checking
     * if the iterator is null, this method should never return an exception, but instead be wrapped
     * in the {@link WriteResponse}.  If an error does occur, the writer has the option of discontinuing
     * writing.  {@link AssertionError} is considered an invalid record and the writer should continue
     * to write regardless.
     *
     * @param iterator to write
     * @return response with data related to record written success for failure
     */
    WriteResponse write(T iterator);
}
