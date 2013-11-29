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

import ch.qos.logback.classic.Level
import groovy.io.FileType
import org.apache.commons.lang.exception.ExceptionUtils
import org.slf4j.LoggerFactory

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class MetridocMain {

    public static final String LONG_JOB_PREFIX = "metridoc-job-"
    public static boolean PLAIN_TEXT = false
    public static Level LEVEL = null
    public static boolean EXTENDED_LOG = false
    private static slash = System.getProperty("file.separator")
    def home = System.getProperty("user.home")
    String jobPath = "$home${slash}.metridoc${slash}jobs"
    def libDirectories = ["$home${slash}.groovy${slash}lib", "$home${slash}.grails${slash}drivers", "$home${slash}.metridoc${slash}lib", "$home${slash}.metridoc${slash}drivers"]
    boolean exitOnFailure = true
    String[] args

    public static void main(String[] args) {
        new MetridocMain(args: args).run()
    }

    @SuppressWarnings(["GroovyAccessibility", "GroovyAssignabilityCheck", "GroovyUnnecessaryReturn"])
    def run() {

        def (OptionAccessor options, CliBuilder cli) = parseArgs()
        try {
            setPropertyValues(options)

            if (doHelp(cli, options)) return

            if (doListJobs(options)) return

            setupLogging(options)

            if (doInstallDeps(options)) return

            checkForAndInstallDependencies(options)

            if (doInstall(options)) return

            return runJob(options)
        }
        catch (Throwable throwable) {
            if (args.contains("-stacktrace") || args.contains("--stacktrace")) {
                def log = LoggerFactory.getLogger("metridoc.simple")
                log.error ExceptionUtils.getStackTrace(throwable)
            }
            else {
                printSimpleErrors(throwable)
            }
            if (exitOnFailure) {
                System.exit(1)
            } else {
                throw throwable
            }
        }
    }

    static void printSimpleErrors(Throwable ignored){

        def printMessage = "ERROR:\n"

        def exceptionMessages = []
        exceptionMessages.add([ignored.getClass().getSimpleName(), ignored.message])

        Throwable deeper = ignored
         while((deeper = ExceptionUtils.getCause(deeper)) != null){
            exceptionMessages.add([deeper.getClass().getSimpleName(), deeper.message])
        }

        for(error in exceptionMessages){
            printMessage+="\tCaused by ${error[0]}: ${error[1]}\n"
        }
        printMessage+="\nuse --stacktrace to see more details\n"
        def simpleLogger = LoggerFactory.getLogger("metridoc.simple")
        simpleLogger.error(printMessage)
    }

    void setPropertyValues(OptionAccessor options) {
        def commandLine = options.getInner()
        def cliOptions = commandLine.options

        cliOptions.each {
            if ("D" == it.opt) {
                def values = it.values
                if (values.size() == 1) {
                    System.setProperty(values[0], "")
                }
                else {
                    System.setProperty(values[0], values[1])
                }
            }
        }

        if(options.jobPath) {
            jobPath = options.jobPath.startsWith("=") ? options.jobPath.substring(1) : options.jobPath
            assert new File(jobPath).exists() : "jobPath [$jobPath] does not exist"
        }
    }

    boolean doListJobs(OptionAccessor options) {
        new ListJobsCommand(main: this).run(options)
    }

    protected static boolean isUrl(String possibleUrl) {
        try {
            new URL(possibleUrl).toURI()
            return true
        }
        catch (Throwable ignore) {
            return false
        }
    }

    @SuppressWarnings(["GroovyAccessibility", "GroovyVariableNotAssigned"])
    def protected runJob(OptionAccessor options) {
        RunJobCommand command = new RunJobCommand(main: this)
        command.run(options)
        command.lastResult
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected void setupLogging(OptionAccessor options) {
        if (options.plainText) {
            PLAIN_TEXT = true
        }

        if (options.logLevel) {
            LEVEL = Level.toLevel((options.logLevel as String).toUpperCase(), Level.ERROR)
        }

        if (options.logLineExt) {
            EXTENDED_LOG = true
        }
    }

    protected static File getFileFromDirectory(File directory, String fileName) {

        def response

        response = new File(directory, fileName)
        if (response.exists()) {
            return response
        }

        def groovyDir = new File(directory, "src${slash}main${slash}groovy")
        if (groovyDir.exists()) {
            response = new File(groovyDir, fileName)
            if (response.exists()) {
                return response
            }
        }

        def resourcesDir = new File(directory, "src${slash}main${slash}resources")
        if (resourcesDir.exists()) {
            response = new File(resourcesDir, fileName)
            if (response.exists()) {
                return response
            }
        }

        return null
    }

    protected static String getShortName(String longJobName) {
        if (longJobName.startsWith(LONG_JOB_PREFIX)) {
            def shortName = longJobName.substring(LONG_JOB_PREFIX.size())
            def index = shortName.lastIndexOf("-")
            if (index != -1) {
                return shortName.substring(0, index)
            }

            return shortName
        }
        return longJobName
    }

    boolean doInstall(OptionAccessor options) {
        new InstallJobCommand(main: this).run(options)
    }

    File getJobDir(String jobName) {
        def fullJobName = jobName
        if (!fullJobName.startsWith("metridoc-job-")) {
            fullJobName = "metridoc-job-$jobName"
        }
        File jobDir = null
        File jobPath = new File(jobPath)

        if (jobPath.exists()) {
            jobPath.eachFile(FileType.DIRECTORIES) {
                if (it.name.startsWith(fullJobName)) {
                    jobDir = it
                }
            }
        }

        if (!jobDir) {
            println ""
            println "ERROR: [$jobName] is not a recognized job"
            println ""
            System.exit(3)
        }

        return jobDir
    }

    protected boolean doHelp(CliBuilder cli, OptionAccessor options) {
        new HelpCommand(main: this, cliBuilder: cli).run(options)
    }

    protected static boolean doInstallDeps(OptionAccessor options) {
        new InstallMdocDependenciesCommand().run(options)
    }

    protected static void checkForAndInstallDependencies(OptionAccessor options) {
        if (!InstallMdocDependenciesCommand.dependenciesExist()) {
            InstallMdocDependenciesCommand.downloadDependencies()
            println ""
            println "Needed to download dependencies, please re-run the job"
            println ""
            System.exit(0)
        }
    }

    protected List parseArgs() {

        def cli = new CliBuilder(
                usage: "mdoc [global option] [<command> | <job> | help | help <job>] [job options]",
                header: "\nGlobal Options:",
                footer: "\nAvailable Commands:\n" +
                        " --> list-jobs                  lists all available jobs\n" +
                        " --> install <destination>      installs a job\n" +
                        " --> help [job name]            prints README of job, or this message\n" +
                        " --> install-deps               installs dependencies if they are not there"
        )

        cli.help("prints this message")
        cli.stacktrace("prints full stacktrace on error")
        cli.D(args: 2, valueSeparator: '=', argName: 'property=value', 'sets jvm system property')
        cli.logLevel(args: 1, argName: 'level', 'sets log level (info, error, etc.)')
        cli.logLineExt("make the log line more verbose")
        cli.lib(args: 1, argName: "directory", "add a directory of jars to classpath")
        cli.jobPath(args:1, argName: "jobPath", "specify where jobs are stored")
        cli.plainText("disables ansi logging")
        def options = cli.parse(args)
        if (options.lib) {
            libDirectories.add(options.lib)
            println "INFO: adding all jar files in [$options.lib] to classpath"
        }
        [options, cli]
    }
}
