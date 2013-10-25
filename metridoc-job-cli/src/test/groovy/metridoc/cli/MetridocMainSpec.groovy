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

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.context.ApplicationContext
import spock.lang.IgnoreRest
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class MetridocMainSpec extends Specification {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    /**
     * helps make things work in intellij
     *
     * @param directory
     * @return
     */
    String normalizePath(String directory) {
        boolean inIntellij = !new File(directory).exists()
        if (inIntellij) {
            def directoryInIntellij = "metridoc-job-cli/$directory"
            assert new File(directoryInIntellij).exists(): "directory [$directory] or [$directoryInIntellij] cannot " +
                    "be found"
            return directoryInIntellij
        }

        return directory
    }

    void "test running a script"() {
        given:
        def args = ["--stacktrace", normalizePath("src/test/testJobs/script/simpleScript.groovy"), "--mergeMetridocConfig=false",
                "--embeddedDataSource"]
        def main = new MetridocMain(args: args)

        when:
        def result = main.run()

        then:
        noExceptionThrown()
        "simpleScript ran" == result
    }

    void "test running a simple job"() {
        given:
        def args = ["--stacktrace", normalizePath("src/test/testJobs/simpleJob"), "--mergeMetridocConfig=false",
                "--embeddedDataSource"]
        def main = new MetridocMain(args: args)

        when:
        def result = main.run()

        then:
        noExceptionThrown()
        "foo ran" == result
    }

    void "test running a complex job"() {
        given:
        def args = ["foo", "--stacktrace"]
        def main = new MetridocMain(args: args, jobPath: normalizePath("src/test/testJobs/complexJob"))

        when:
        def result = main.run()

        then:
        noExceptionThrown()
        "complex foo project ran" == result
    }

    void "test running a complex job from a directory"() {
        given:
        def args = [normalizePath("src/test/testJobs/complexJob/metridoc-job-foo-0.1")]
        def main = new MetridocMain(args: args)

        when:
        def result = main.run()

        then:
        noExceptionThrown()
        "complex foo project ran" == result
    }

    void "test installing and running a job"() {
        given:
        def args = ["install", new File(normalizePath("src/test/testJobs/metridoc-job-bar-0.1.zip")).toURI().toURL().toString()]
        def main = new MetridocMain(args: args, jobPath: folder.getRoot().toString())

        when:
        main.run()

        then:
        noExceptionThrown()
        folder.root.listFiles().find { it.name == "metridoc-job-bar-0.1" }
        0 < folder.root.listFiles().size()
        if (!System.getProperty("os.name").contains("indows")) {
            //cannot for the life of me figure out how to delete the copied zip file
            1 == folder.root.listFiles().size()
        }
    }

    void "test extracting short name from long name"() {
        expect:
        a == MetridocMain.getShortName(b)

        where:
        a     | b
        "foo" | "metridoc-job-foo"
        "foo" | "metridoc-job-foo-1.0"
        "foo" | "foo"
    }

    void "test grabbing a file from a directory"() {
        when:
        def readme = new MetridocMain().getFileFromDirectory(new File(normalizePath("src/test/testJobs/complexJob/metridoc-job-foo-0.1")), "README")

        then:
        readme.exists()
        readme.text.contains("complex foo's README")
    }

    void "any args using -D get pushed to system properties"() {
        when:
        new MetridocMain(args: ["-Dfoo=bar", "-DfooBar"] as String[]).run() //it will print help

        then:
        "bar" == System.getProperty("foo")
        System.getProperties().containsKey("fooBar")
    }

    void "stacktrace should be usable anywhere"() {
        when:
        new MetridocMain(
                exitOnFailure: false,
                args: [
                        normalizePath("src/test/testJobs/script/errorScript.groovy"),
                        "--stacktrace"
                ] as String[]).run()

        then: "exception will be thrown instead of just printing it"
        thrown(RuntimeException)
    }
}
