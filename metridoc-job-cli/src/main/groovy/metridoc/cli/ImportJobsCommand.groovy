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

    public static final String IMPORT_FILE = "import.groovy"
    MetridocMain main


    public static final Closure NOT_DIRECTORY = { file ->
        "cannot import for [$file] since it is not a directory"
    }

    public static final Closure DOES_NOT_EXIST = { file ->
        "cannot import for [$file] since it does not exist"
    }

    @Override
    boolean run(OptionAccessor options) {
        def args = options.arguments()
        if (args && args[0] == "import-jobs") {
            if (args.size() == 1) {
                addImports(new File(SystemUtils.USER_DIR))
            } else {
                addImports(new File(args[1]))
            }
            return true
        }

        return false
    }

    protected static addImports(File file) {
        checkProject(file)
        def importsFile = new File(file, "import.groovy")
        File mdoc = prepareMdocDirectory(file)
        if (importFileExists(file)) {
            Map<String, ImportUrl> imports = getImportHash(importsFile)
            imports.each { key, value ->
                String path = value.path
                def url = value.url
                File destination = getDestination(mdoc, url)
                url.withInputStream { inputStream ->
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
            }
        } else {
            log.info "[$IMPORT_FILE] does not exist, no jobs to import"
        }
    }

    protected static File getDestination(File mdoc, URL url) {
        def urlText = url.toString()
        int lastIndex = urlText.lastIndexOf("/")
        def fileName = urlText.substring(lastIndex + 1)

        return new File(mdoc, fileName)
    }

    protected static Map<String, ImportUrl> getImportHash(importsFile) {
        Script script = new GroovyShell().parse(importsFile)
        script.run()
        def result = [:]
        script.binding.variables.each { key, value ->
            if (value instanceof Map) {
                def importUrl = ImportUrl.create(value)
                if (importUrl) {
                    result[key] = importUrl
                }
            }
        }

        return result
    }

    static protected File getImportFile(File directory) {
        new File(directory, IMPORT_FILE)
    }

    static protected boolean importFileExists(File directory) {
        getImportFile(directory).exists()
    }

    protected static void checkProject(File file) {
        assert file.exists(): DOES_NOT_EXIST(file)
        assert file.isDirectory(): NOT_DIRECTORY(file)
    }

    protected static File getMdoc(File file) {
        new File(file, ".mdoc")
    }

    protected static File prepareMdocDirectory(File file) {
        def mdoc = getMdoc(file)
        if (!mdoc.exists()) {
            assert mdoc.mkdir(): "could not create directory .mdoc"
        }
        mdoc.eachDir { File dir ->
            dir.deleteDir()
        }
        return mdoc
    }

}



class ImportUrl {
    URL url
    String path

    static ImportUrl create(Map data) {
        try {
            if (data.containsKey("url")) {
                return new ImportUrl(url: new URI(data.url).toURL(), path: data.path)
            }
        } catch (URISyntaxException ignored) {
            return null
        }
    }
}