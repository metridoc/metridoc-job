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



package metridoc.cli

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import metridoc.utils.ArchiveMethods

import java.util.concurrent.CountDownLatch

@Slf4j
class InstallMdocDependencies {

    static void downloadDependencies () {
        File destination = getDestination()
        Set<String> currentLibs = getCurrentLibs(destination)

        def classLoader = Thread.currentThread().contextClassLoader
        def dependencyFile = classLoader.getResourceAsStream("DEPENDENCY_URLS").getText("utf-8").trim()

        doGrabDependencies(destination, dependencyFile)
        addDependenciesToClassPath(destination, currentLibs)
    }

    protected static void doGrabDependencies(File destination, String dependencyUrls) {

        print "Downloading Dependencies"

        def latch = new CountDownLatch(1)

        Throwable badDownloadError
        Thread.start {
            try {
                dependencyUrls.eachLine {String line ->
                    new URL(line).openConnection().with {
                        def jarFile = new File(destination, getFileName(line))
                        jarFile << inputStream
                        inputStream.close()
                    }
                }
            }
            catch (Throwable throwable) {
                badDownloadError = throwable
            }

            latch.countDown()
        }

        while (latch.count != 0) {
            print "."
            Thread.sleep(200)
        }

        println "" //print blank space

        if (badDownloadError) {
            throw badDownloadError
        }
    }

    protected static Set<String> getCurrentLibs(File destination) {
        Set currentLibs = [] as Set<String>
        destination.eachFile {
            currentLibs << it.name
        }
        currentLibs
    }

    protected static File getDestination() {
        String classPathSeparator = System.getProperty("os.name").contains("indows") ? ";" : ":"
        def classpath = System.getProperty("java.class.path")
        log.debug "extracting destination for dependencies from classpath: [$classpath]"
        def destination = new File(classpath.split(classPathSeparator)[0]).parentFile
        log.debug "destination for dependencies is [$destination]"
        destination
    }

    protected static void addDependenciesToClassPath(File destination, currentLibs) {
        def classLoader = Sql.classLoader
        while (!(classLoader instanceof URLClassLoader)) {
            classLoader = classLoader.parent
        }

        destination.eachFile {
            if (!currentLibs.contains(it.name)) {
                classLoader.addURL(it.toURI().toURL())
            }
        }
    }

    protected static String getFileName(String url) {
        int index = url.lastIndexOf("/")
        url.substring(index + 1)
    }
}

