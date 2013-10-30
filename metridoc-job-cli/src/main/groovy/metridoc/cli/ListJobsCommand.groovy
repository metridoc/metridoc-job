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

/**
 * Created with IntelliJ IDEA on 10/25/13
 * @author Tommy Barker
 */
class ListJobsCommand implements Command {

    MetridocMain main

    @Override
    boolean run(OptionAccessor options) {
        def cliArgs = options.arguments()
        if ("list-jobs" == cliArgs[0]) {
            def jobDir = new File(main.jobPath)
            println ""

            if (jobDir.listFiles()) {
                println "Available Jobs:"
            }
            else {
                println "No jobs have been installed"
                return true
            }

            jobDir.eachFile(FileType.DIRECTORIES) {
                def m = it.name =~ /metridoc-job-(\w+)-(.+)/
                if (m.matches()) {
                    def name = m.group(1)
                    def version = m.group(2)
                    println " --> $name (v$version)"
                }
                m = it.name =~ /metridoc-job-(\w+)/
                if (m.matches()) {
                    def name = m.group(1)
                    println " --> $name"
                }
            }
            println ""

            return true
        }

        return false
    }
}
