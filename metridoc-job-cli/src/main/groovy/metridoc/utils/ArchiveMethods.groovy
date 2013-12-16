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



package metridoc.utils

import groovy.transform.CompileStatic
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile

/**
 * This class contains some static utility methods defined in such a way that
 * they can be used as Groovy extension methods. The zip-related methods have
 * been shamelessly borrowed from Tim Yate's Groovy Common Extensions library
 * but modified to support retention of file permissions.
 */
@CompileStatic
class ArchiveMethods {

    static Collection<File> unzip (File self, File destination, String topDirectory, Closure<Boolean> filter = null) {
        checkUnzipFileType(self)
        checkUnzipDestination(destination)

        def zipFile = new ZipFile(self)
        //let's make sure top directory exists
        List entries = zipFile.entries as List<ZipArchiveEntry>
        if (topDirectory) {
            boolean foundTopDir = entries.find {ZipArchiveEntry entry ->
                def name = entry.name
                name.endsWith("$topDirectory/")
            } != null

            assert foundTopDir : "Could not find job [$topDirectory] in zipFile [$self]"
        }

        // if destination directory is not given, we'll fall back to the parent directory of 'self'
        if (destination == null) destination = new File(self.parent)

        def unzippedFiles = []

        // The type coercion here is down to http://jira.codehaus.org/browse/GROOVY-6123
        for (ZipArchiveEntry entry in (zipFile.entries as List<ZipArchiveEntry>)) {
            def usedName = entry.name

            if(topDirectory) {
                if(!usedName.contains(topDirectory)) {
                    continue
                }
                else {
                    def index = usedName.indexOf(topDirectory)
                    usedName = usedName.substring(index)
                }
            }
            def file = new File(destination, usedName)
            if (filter == null || filter(file)) {
                if (!entry.isDirectory())  {
                    file.parentFile?.mkdirs()

                    def output = new FileOutputStream(file)
                    output.withStream {
                        InputStream stream = zipFile.getInputStream(entry)
                        try {
                            output << stream
                        }
                        finally {
                            IOUtils.closeQuietly(stream)
                        }
                    }
                }
                else {
                    file.mkdirs()
                }

                unzippedFiles << file
                updateFilePermissions(file, entry.unixMode)
            }
        }

        return unzippedFiles
    }

    /**
     * Unzips a file to a target directory, retaining the file permissions where
     * possible. You can also provide a closure that acts as a filter, returning
     * {@code true} if you want the file or directory extracted, {@code false}
     * otherwise.
     * @param self The zip file to extract.
     * @param destination The directory to extract the zip to. Of course, it must
     * be a directory, otherwise this method throws an IllegalArgumentException.
     * @param filter (optional) A closure that acts as a filter. It must accept a
     * single argument that is a File and return {@code true} if that zip entry
     * should be extracted, or {@code false} otherwise.
     */
    static Collection<File> unzip (File self, File destination, Closure<Boolean> filter = null) {
        unzip(self, destination, null, filter)
    }

    /**
     * <p>Sets appropriate Unix file permissions on a file based on a 'mode'
     * number, such as 0644 or 0755. Note that those numbers are in octal
     * format!</p>
     * <p>The left-most number represents the owner permission (1 = execute,
     * 2 = write, 4 = read, 5 = read/exec, 6 = read/write, 7 = read/write/exec).
     * The middle number represents the group permissions and the last number
     * applies to everyone. In reality, because of limitations in the underlying
     * Java API this method will only honour owner and everyone settings. The
     * group permissions will be set to the same as those for everyone.</p>
     */
    static void updateFilePermissions(File self, long unixMode) {
        if (self.name == "gradlew") {
            self.setExecutable(true)
        }
    }

    /**
     * Checks that the given file is both a file (not a directory, link, etc)
     * and that its name has a .zip extension.
     */
    private static void checkUnzipFileType(File self) {
        if (!self.isFile()) throw new IllegalArgumentException("File#unzip() has to be called on a *.zip file.")

        def filename = self.name
        if (!filename.toLowerCase().endsWith(".zip")) throw new IllegalArgumentException("File#unzip() has to be called on a *.zip file.")
    }

    /**
     * Checks that the given file is a directory.
     */
    private static void checkUnzipDestination(File file) {
        if (file && !file.isDirectory()) throw new IllegalArgumentException("'destination' has to be a directory.")
    }

    static File convertZipNameToDirectory(File parent, File zipFile) {
        def zipName = zipFile.name
        def directoryName = zipName

        if(zipName.endsWith(".zip")) {
            def index = zipName.lastIndexOf(".zip")
            directoryName = zipName.substring(0, index)
        }

        def directory = new File(parent, directoryName)
        if (!directory.exists()) {
            assert directory.mkdir() : "Could not create directory [$directory]"
        }

        return directory
    }
}

