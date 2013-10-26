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

import groovy.io.FileType
import metridoc.utils.JansiPrintWriter
import org.fusesource.jansi.AnsiConsole

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class MetridocMain {

    public static final String LONG_JOB_PREFIX = "metridoc-job-"
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
        catch (Throwable ignored) {
            if (args.contains("-stacktrace") || args.contains("--stacktrace")) {
                throw ignored //just rethrow it
            }
            println ""
            System.err.println("ERROR: $ignored.message")
            println ""

            if (exitOnFailure) {
                System.exit(1)
            } else {
                throw ignored
            }
        }
    }

    static void setPropertyValues(OptionAccessor options) {
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
        if (!options.plainText) {
            AnsiConsole.systemInstall()
            System.out = new JansiPrintWriter(System.out)
            System.err = new JansiPrintWriter(System.err)
            Thread.addShutdownHook {
                AnsiConsole.systemUninstall()
            }
        }

        def simpleLoggerClass
        try {
            simpleLoggerClass = Thread.currentThread().contextClassLoader.loadClass("org.slf4j.impl.SimpleLogger")
        }
        catch (ClassNotFoundException ignored) {
            System.err.println("Could not find SimpleLogger on the classpath, [SimpleLogger] will not be initialized")
            return
        }

        String SHOW_THREAD_NAME_KEY = simpleLoggerClass.SHOW_THREAD_NAME_KEY
        String SHOW_LOG_NAME_KEY = simpleLoggerClass.SHOW_LOG_NAME_KEY
        String SHOW_DATE_TIME_KEY = simpleLoggerClass.SHOW_DATE_TIME_KEY
        String LOG_FILE_KEY = simpleLoggerClass.LOG_FILE_KEY
        System.setProperty(LOG_FILE_KEY, "System.out")
        if (options.logLevel) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", options.logLevel)
        }
        else {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error")
            System.setProperty("org.slf4j.simpleLogger.log.metridoc", "info")
        }

        System.setProperty(SHOW_DATE_TIME_KEY, "true")

        if (!options.logLineExt) {
            System.setProperty(SHOW_THREAD_NAME_KEY, "false")
            System.setProperty(SHOW_LOG_NAME_KEY, "false")
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
            println "[$jobName] is not a recognized job"
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
        cli.plainText("disables ansi logging")
        def options = cli.parse(args)
        if (options.lib) {
            libDirectories.add(options.lib)
            println "INFO: adding all jar files in [$options.lib] to classpath"
        }
        [options, cli]
    }
}
