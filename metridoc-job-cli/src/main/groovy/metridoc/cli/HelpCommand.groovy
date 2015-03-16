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

/**
 * Created with IntelliJ IDEA on 10/25/13
 * @author Tommy Barker
 */
class HelpCommand implements Command {

    CliBuilder cliBuilder
    MetridocMain main

    @Override
    boolean run(OptionAccessor options) {
        def arguments = options.arguments()
        File readme
        if (arguments[0] == "help" && arguments.size() > 1) {
            def jobName = arguments[1]
            def file = new File(jobName)
            def jobDir
            if (file.exists()) {
                if (file.isFile()) {
                    jobDir = file.parentFile
                }
                else {
                    jobDir = file
                }
            }
            else {
                jobDir = main.getJobDir(arguments[1])
            }

            readme = main.getFileFromDirectory(jobDir, "README")
            if (readme) {
                println readme.text
            }
            else {
                println "README does not exist for $jobName"
            }
            return true
        }

        if (askingForHelp(options)) {
            println ""
            cliBuilder.usage()
            println ""
            println "\n\nCommand to create a new job:\n\n" +
                    "    git clone -bmetridoc-job-template https://github.com/metridoc/metridoc-job.git && \\\n" +
                    "    (cd metridoc-job && git remote rm origin) && \\\n" +
                    "    mv metridoc-job <job name>\n"

            def mdocVersion = this.class.classLoader.getResourceAsStream("MDOC_VERSION").getText("utf-8").trim()
            println "Currently using mdoc $mdocVersion"
            try {
                def slurper = new XmlSlurper()
                def metaData = slurper.parse("http://dl.bintray.com/upennlib/metridoc/com/github/metridoc/metridoc-job-core/maven-metadata.xml")
                def version = metaData.versioning.release
                if(version != mdocVersion) {
                    println ""
                    println "version [$version] is available, to install run: "
                    println "    curl -s https://raw.githubusercontent.com/metridoc/metridoc-job/master/install-mdoc.sh | sh"
                }
            }
            catch (Exception ignore) {
            }

            println ""
            return true
        }

        return false
    }

    protected static boolean askingForHelp(OptionAccessor options) {
        !options.arguments() || options.help || options.arguments().contains("help")
    }
}
