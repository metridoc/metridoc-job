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
import metridoc.utils.ArchiveMethods
import metridoc.utils.JansiPrintWriter
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.SystemUtils
import org.fusesource.jansi.AnsiConsole
import org.slf4j.LoggerFactory

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

            checkForAndInstallDependencies(options)

            if (doInstallDeps(options)) return

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
        def cliArgs = options.arguments()
        if ("list-jobs" == cliArgs[0]) {
            def jobDir = new File(jobPath)
            println ""

            if (jobDir.listFiles()) {
                println "Available Jobs:"
            }
            else {
                println "No jobs have been installed"
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
        if (!options.plainText) {
            AnsiConsole.systemInstall()
            System.out = new JansiPrintWriter(System.out)
            System.err = new JansiPrintWriter(System.err)
        }

        def arguments = options.arguments()
        def shortJobName = arguments[0]

        def file
        if (isUrl(shortJobName)) {
            def slashIndex = shortJobName.lastIndexOf("/")
            def questionIndex = shortJobName.lastIndexOf(".groovy")
            def fileName
            if (questionIndex > 0) {
                fileName = shortJobName.substring(slashIndex + 1, questionIndex)
            }
            else {
                fileName = shortJobName.substring(slashIndex + 1)
            }

            try {
                file = File.createTempFile(fileName, ".groovy")
            }
            catch (IOException e) {
                def tmpDir = new File("$home${slash}.metridoc${slash}tmp")
                if(!tmpDir.exists()) {
                    assert tmpDir.mkdirs() : "Could not create $tmpDir"
                }
                file = new File(tmpDir, "${fileName}.groovy")
                file.createNewFile()
            }

            file.setText(new URL(shortJobName).text, "utf-8")
            file.deleteOnExit()
        }

        file = file ?: new File(shortJobName)
        File metridocScript
        def loader = findHighestLevelClassLoader()
        addLibDirectories(loader)

        if (file.isFile()) {
            metridocScript = file
        }
        else if (file.isDirectory()) {
            addDirectoryResourcesToClassPath(this.class.classLoader as URLClassLoader, file)
            metridocScript = getRootScriptFromDirectory(file)
        }
        else {
            def jobDir = getJobDir(shortJobName)
            addDirectoryResourcesToClassPath(this.class.classLoader as URLClassLoader, jobDir)
            metridocScript = getRootScriptFromDirectory(jobDir, shortJobName)
        }

        def binding = new Binding()

        binding.args = [] as String[]
        def log = LoggerFactory.getLogger(this.getClass())
        log.debug "parsing arguments $arguments to be used with job $shortJobName"
        def argsList = args as List
        def index = argsList.indexOf(arguments[0])
        def jobArgsList = argsList[(index + 1)..<argsList.size()]
        log.debug "arguments used in job $shortJobName after removing job name are $jobArgsList"
        binding.args = jobArgsList as String[]

        if (options.stacktrace) {
            binding.stacktrace = true
        }

        assert metridocScript && metridocScript.exists(): "root script does not exist"
        def thread = Thread.currentThread()

        def shell = new GroovyShell(thread.contextClassLoader, binding)
        thread.contextClassLoader = shell.classLoader
        log.info "Running $metridocScript at ${new Date()}"
        def response = null
        def throwable
        try {
            response = shell.evaluate(metridocScript)
        }
        catch (Throwable badExecution) {
            throwable = badExecution
        }
        log.info "Finished running $metridocScript at ${new Date()}"
        if (throwable) throw throwable
        return response
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected void setupLogging(OptionAccessor options) {
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

    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyAccessibility"])
    protected void addDirectoryResourcesToClassPath(URLClassLoader loader, File file) {
        def resourceDir = new File(file, "src${slash}main${slash}resources")
        if (resourceDir.exists()) {
            loader.addURL(resourceDir.toURI().toURL())
        }
        def groovyDir = new File(file, "src${slash}main${slash}groovy")
        if (groovyDir.exists()) {
            loader.addURL(groovyDir.toURI().toURL())
        }

        loader.addURL(file.toURI().toURL())
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected File getRootScriptFromDirectory(File directory, String shortName = null) {
        if (shortName == null) {
            def path = directory.canonicalPath
            def index = path.lastIndexOf(SystemUtils.FILE_SEPARATOR)
            shortName = getShortName(path.substring(index + 1))
        }

        def response

        response = getFileFromDirectory(directory, "metridoc.groovy")
        if (response) return response

        response = getFileFromDirectory(directory, "${shortName}.groovy")
        if (response) return response

        return response
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
        def cliArgs = options.arguments()

        def command = cliArgs[0]
        if (command == "install") {
            assert cliArgs.size() == 2: "when installing a job, [install] requires a location"
            installJob(cliArgs[1])
            return true
        }

        return false
    }

    protected File getJobDir(String jobName) {
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
                jobDir = getJobDir(arguments[1])
            }

            readme = getFileFromDirectory(jobDir, "README")
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
            cli.usage()
            println ""
            def mdocVersion = this.class.classLoader.getResourceAsStream("MDOC_VERSION")
            println "Currently using mdoc $mdocVersion"
            println ""
            return true
        }

        return false
    }

    protected static boolean doInstallDeps(OptionAccessor options) {
        options.arguments().contains("install-deps")
    }

    protected static void checkForAndInstallDependencies(OptionAccessor options) {
        if (!dependenciesExist()) {
            InstallMdocDependencies.downloadDependencies()
        }
        else if (doInstallDeps(options)) {
            println "Dependencies have already been installed"
        }
    }

    protected static boolean askingForHelp(OptionAccessor options) {
        !options.arguments() || options.help || options.arguments().contains("help")
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

    URLClassLoader findHighestLevelClassLoader() {
        def loader = this.class.classLoader

        if (loader.rootLoader) {
            return this.class.classLoader.rootLoader as URLClassLoader
        }

        def loaders = []
        loaders << loader
        while (loader.parent) {
            loaders << loader.parent
            loader = loader.parent
        }
        loaders = loaders.reverse()

        for (it in loaders) {
            if (it instanceof URLClassLoader) {
                return it
            }
        }

        throw new RuntimeException("Could not find a suitable classloader")
    }

    void addLibDirectories(URLClassLoader classLoader) {
        libDirectories.each {
            addJarsFromDirectory(classLoader, new File(it))
        }
    }

    @SuppressWarnings("GroovyAccessibility")
    static void addJarsFromDirectory(URLClassLoader classloader, File directory) {
        if (directory.exists() && directory.isDirectory()) {
            directory.eachFile(FileType.FILES) {
                if (it.name.endsWith(".jar")) {
                    classloader.addURL(it.toURI().toURL())
                }
            }
        }
    }

    void installJob(String urlOrPath) {
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

        def jobPathDir = new File("$jobPath")
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

        ArchiveMethods.unzip(destination, jobPathDir)
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

    private static boolean dependenciesExist() {
        dependenciesExistHelper("org.springframework.context.ApplicationContext")
    }

    private static boolean dependenciesExistHelper(String className) {
        try {
            Class.forName(className)
            return true
        }
        catch (ClassNotFoundException ignored) {
            return false
        }
    }
}
