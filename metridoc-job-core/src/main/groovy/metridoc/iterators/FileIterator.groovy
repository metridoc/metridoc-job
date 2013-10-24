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

import metridoc.utils.IOUtils

/**
 * @deprecated
 */
abstract class FileIterator extends RecordIterator implements Closeable {

    String path

    @Lazy(soft = true)
    File file = {
        if (path) {
            return new File(path)
        }

        return null
    }()

    @Lazy
    String fileName = {
        if (file) {
            return file.name
        }

        return null
    }()

    @Lazy(soft = true)
    InputStream inputStream = {
        assert file : "file needs to be set in FileIterator"
        file.newInputStream()
    }()

    void close() {
        IOUtils.closeQuietly(inputStream)
    }
}
