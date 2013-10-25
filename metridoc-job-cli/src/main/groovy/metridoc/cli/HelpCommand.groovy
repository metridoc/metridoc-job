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
            def mdocVersion = this.class.classLoader.getResourceAsStream("MDOC_VERSION")
            println "Currently using mdoc $mdocVersion"
            println ""
            return true
        }

        return false
    }

    protected static boolean askingForHelp(OptionAccessor options) {
        !options.arguments() || options.help || options.arguments().contains("help")
    }
}
