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
