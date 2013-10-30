/*
  *Copyright 2013 Trustees of the University of Pennsylvania. Licensed under the
  *	Educational Community License, Version 2.0 (the "License"); you may
  *	not use this file except in compliance with the License. You may
  *	obtain a copy of the License at
  *
  *http://www.osedu.org/licenses/ECL-2.0
  *
  *	Unless required by applicable law or agreed to in writing,
  *	software distributed under the License is distributed on an "AS IS"
  *	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  *	or implied. See the License for the specific language governing
  *	permissions and limitations under the License.
  */

package metridoc.cli

import groovy.io.FileType
import metridoc.utils.ArchiveMethods
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.SystemUtils
import org.slf4j.LoggerFactory

/**
 * @author Tommy Barker
 */
class InstallJobCommand implements Command {

    public static final String LONG_JOB_PREFIX = "metridoc-job-"
    MetridocMain main

    @Override
    boolean run(OptionAccessor options) {
        assert main && main.jobPath : "main and main.jobPath must be set"
        assert options != null : "options must not be null"
        def cliArgs = options.arguments()

        def command = cliArgs[0]
        if (command == "install") {
            assert cliArgs.size() == 2 || cliArgs.size() == 3: "when installing a job, " +
                    "[install] requires a location and optionally a sub directory in a zip file"
            if (cliArgs.size() == 1) {
                installJob(cliArgs[1], null)
            }
            else {
                installJob(cliArgs[1], cliArgs[2])
            }

            return true
        }

        return false
    }

    void installJob(String urlOrPath, String optionSubDirectory) {
        def file = new File(urlOrPath)
        def index = urlOrPath.lastIndexOf("/")
        if (file.exists()) {
            urlOrPath = file.canonicalPath
            index = urlOrPath.lastIndexOf(SystemUtils.FILE_SEPARATOR)
        }
        def fileName = urlOrPath.substring(index + 1)
        def destinationName = fileName

        if (destinationName == "master.zip") {
            def m = urlOrPath =~ /\/metridoc-job-(\w+)\//
            if (m.find()) {
                destinationName = "$LONG_JOB_PREFIX${m.group(1)}"
            }
        }

        if (!destinationName.startsWith(LONG_JOB_PREFIX)) {
            destinationName = "$LONG_JOB_PREFIX$destinationName"
        }

        def jobPathDir = new File("$main.jobPath")
        if (!jobPathDir.exists()) {
            jobPathDir.mkdirs()
        }

        def m = destinationName =~ /(metridoc-job-\w+)(-v?[0-9])?/
        def destinationExists = m.lookingAt()
        if (destinationExists) {
            jobPathDir.eachFile(FileType.DIRECTORIES) {
                def unversionedName = m.group(1)
                if (it.name.startsWith(unversionedName)) {
                    println "upgrading $destinationName"
                    assert it.deleteDir(): "Could not delete $it"
                }
            }
        }
        else {
            println "$destinationName does not exist, installing as new job"
        }

        def destination = new File(jobPathDir, destinationName)
        def fileToInstall

        try {
            fileToInstall = new URL(urlOrPath)
        }
        catch (Throwable ignored) {
            fileToInstall = new File(urlOrPath)
            if (fileToInstall.exists() && fileToInstall.isDirectory()) {
                installDirectoryJob(fileToInstall, destination)
                return
            }

            def supported = fileToInstall.exists() && fileToInstall.isFile() && fileToInstall.name.endsWith(".zip")
            if (!supported) {
                println ""
                println "$fileToInstall is not a zip file"
                println ""
                System.exit(2)
            }
        }

        if (!destinationName.endsWith(".zip")) {
            destinationName += ".zip"
        }
        destination = new File(jobPathDir, destinationName)
        fileToInstall.withInputStream { inputStream ->
            BufferedOutputStream outputStream = destination.newOutputStream()
            try {
                outputStream << inputStream
            }
            finally {
                IOUtils.closeQuietly(outputStream)
                //required so windows can successfully delete the file
                System.gc()
            }
        }

        ArchiveMethods.unzip(destination, jobPathDir, optionSubDirectory)
        def filesToDelete = []

        jobPathDir.eachFile {
            if (it.isFile() && it.name.endsWith(".zip")) {
                filesToDelete << it
            }
        }

        def log = LoggerFactory.getLogger(MetridocMain)

        if(filesToDelete) {
            log.debug "deleting [$filesToDelete]"
        }
        else {
            log.debug "there are no files to delete"
        }

        filesToDelete.each {File fileToDelete ->
            boolean successfulDelete = fileToDelete.delete()
            if(successfulDelete) {
                log.debug "successfully deleted ${fileToDelete}"
            }
            else {
                log.warn "could not delete [${fileToDelete}], marking it for deletion after jvm shutsdown"
                fileToDelete.deleteOnExit()
            }
        }
    }

    private static void installDirectoryJob(File file, File destination) {
        FileUtils.copyDirectory(file, destination)
    }
}
