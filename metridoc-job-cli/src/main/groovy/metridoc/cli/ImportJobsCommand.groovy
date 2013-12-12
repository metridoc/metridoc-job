package metridoc.cli

import groovy.util.logging.Slf4j
import metridoc.utils.ArchiveMethods
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.SystemUtils

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 12/12/13
 * Time: 4:07 PM
 * To change this template use File | Settings | File Templates.
 */
@Slf4j
class ImportJobsCommand implements Command {

    MetridocMain main

    @Override
    boolean run(OptionAccessor options) {
        def args = options.arguments()
        if(args && args[0] == "import-jobs") {
            if(args.size() == 1) {
                addImports(new File(SystemUtils.USER_DIR))
            }
            else {
                addImports(new File(args[1]))
            }
            return true
        }

        return false
    }

    protected static addImports(File file) {
        log.info file as String
        assert file.isDirectory() : "import-jobs needs a directory with [import.groovy] in the root"
        def importsFile = new File(file, "import.groovy")
        def mdoc = new File(file, ".mdoc")
        if(!mdoc.exists()) {
            assert mdoc.mkdir() : "could not create directory .mdoc"
        }
        mdoc.eachDir {File dir ->
            dir.deleteDir()
        }
        if (importsFile.exists()) {
            Script script = new GroovyShell().parse(importsFile)
            script.run()
            def imports = script.binding.variables
            def url
            imports.each {key, value ->
                try {
                    String urlText = value.url
                    String path = value.path
                    url = new URI(urlText).toURL()
                    int lastIndex = urlText.lastIndexOf("/")
                    def fileName = urlText.substring(lastIndex + 1)
                    def destination = new File(mdoc, fileName)
                    url.withInputStream {inputStream ->
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

                    ArchiveMethods.unzip(destination, mdoc, path)
                    destination.delete()
                } catch (URISyntaxException ignored) {
                    log.error "Could not download [$url] for [${key}]"
                }
            }
        }
        else {
            log.info "[imports.groovy] does not exist, no jobs to import"
        }

    }

}
