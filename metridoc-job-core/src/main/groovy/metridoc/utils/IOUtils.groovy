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



package metridoc.utils;


import org.apache.camel.Exchange
import org.apache.camel.component.file.GenericFile
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.zip.GZIPInputStream
import org.apache.commons.lang.SystemUtils

/**
 *
 * @author tbarker
 */
public class IOUtils {

    private static final Logger LOG = LoggerFactory.getLogger(IOUtils.class);
    static final String METRIDOC = ".metridoc";
    static final String PROPERTIES_EXT = ".properties";


    private IOUtils() {}

    /**
     *
     * @param inputStream
     */
    public static void closeQuietly(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception e) {
                //ignore
                LOG.warn("exception occurred when closing a stream. Since we are closing the stream silently "
                    + "the error will be ignored", e);
            }
        }
    }

    /**
     *
     * @param reader
     */
    public static void closeQuietly(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception e) {
                //ignore
                LOG.warn("exception occurred when closing a reader. Since we are closing the stream silently "
                    + "the error will be ignored", e);
            }
        }
    }

    /**
     * searches for one instance of a resource.  By calling this method, the resource is expected to exist.
     * If the resource shows up multiple times, a
     * {@link metridoc.utils.MultipleResourcesException} is thrown.  If the resource is not found at all a
     * {@link FileNotFoundException} is thrown.
     *
     * @param resourceName
     * @return the stream of the resource
     */
    static InputStream getResource(String resourceName) {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        }
        catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = this.getClass().getClassLoader();
        }
        getResource(resourceName, cl)
    }

    /**
     * searches for one instance of a resource given a {@link .  By calling this method, the resource is expected to exist.
     * If the resource shows up multiple times, a
     *{@link MultipleResourcesException} is thrown.  If the resource is not found at all a
     *{@link FileNotFoundException} is thrown.
     *
     * @param resourceName
     * @return the stream of the resource
     */
    static InputStream getResource(String resourceName, ClassLoader classLoader) {
        def streams = getResources(resourceName, classLoader)
        if (streams.size() > 1) {
            throw new MultipleResourcesException(resources: streams, resourceName: resourceName)
        }

        if (streams.size() < 1) {
            throw new FileNotFoundException("Could not find resource ${resourceName}")
        }

        return streams[0]
    }

    /**
     * gets a list of resource urls.  Essentially grabs the enumeration of urls from {@link ClassLoader#getResources}
     * and converts them into a list of urls
     *
     * @param resourceName
     * @param classLoader
     * @return
     */
    public static List<URL> getResourceUrls(String resourceName, ClassLoader classLoader) {
        def result = []
        Enumeration<URL> resources = classLoader.getResources(resourceName);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            result.add(url)
        }

        return result
    }

    /**
     * gets a list of resource urls.  Essentially grabs the enumeration of urls from {@link ClassLoader#getResources}
     * and converts them into a list of urls
     *
     * @param resourceName
     * @param classLoader
     * @return
     */
    public static List<URL> getResourceUrls(String resourceName) {
        return getResourceUrls(resourceName, ClassUtils.defaultClassLoader)
    }

    /**
     * returns all streams of the specified resources.  Really useful for grabbing files that have the same name and 
     * location within multiple jars.
     *
     * @param resource
     * @param classLoader
     * @return a list of streams, empty list if no resources are found
     * @throws IOException
     */
    public static List<InputStream> getResources(String resource, ClassLoader classLoader) throws IOException {
        List<InputStream> result = []
        def urls = getResourceUrls(resource, classLoader)
        urls.each {URL url ->
            result.add(url.openStream())
        }

        return result;
    }

    /**
     * returns all streams of the specified resources.  Really useful for grabbing files that have the same name and
     * location within multiple jars.  The default {@link ClassLoader} is used, see
     * {@link ClassUtils#getDefaultClassLoader} on how the default {@link ClassLoader} is defined
     *
     * @param resource the resource on the classpath to grab
     * @return a list of streams, empty list if no resources are found
     * @throws IOException error if there are difficulties retrieving the resources
     */
    public static List<InputStream> getResources(String resource) throws IOException {
        return getResources(resource, ClassUtils.getDefaultClassLoader())
    }

    /**
     * loads properties from &lt;user.home&gt;/.metridoc/&lt;name&gt;.properties if they are there
     * @param name of the property file
     * @return properties
     */
    public static Properties loadProperties(String name) {
        String filePath = getPropertyFilePath(name);
        File file = new File(filePath);
        return loadPropertyFile(file);
    }

    private static Properties loadPropertyFile(File file) {
        Properties result = new Properties();
        String filePath = file.getPath();

        if (file.exists()) {
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);
                result.load(inputStream);
            } catch (IOException ex) {
                String message = "could not load properties from file " + filePath;
                throw new RuntimeException(message, ex);
            } finally {
                if (inputStream != null) {
                    IOUtils.closeQuietly(inputStream);
                }
            }
        } else {
            LOG.warn("could not find property file {}", filePath);
        }

        return result;
    }

    /**
     * loads properties from &lt;filePath&gt;/&lt;name&gt;.properties if they are there
     *
     * @param name of the property file
     * @param basePath
     * @return properties
     */
    public static Properties loadProperties(String name, String basePath) {
        String fullPath = getPropertyFilePath(basePath, name);
        File file = new File(fullPath);

        return loadPropertyFile(file);
    }

    static String getPropertyFilePath(String name) {

        String base = SystemUtils.USER_HOME + SystemUtils.FILE_SEPARATOR + METRIDOC + SystemUtils.FILE_SEPARATOR;

        return getPropertyFilePath(base, name);
    }

    static String getPropertyFilePath(String rootFilePath, String name) {
        String path = rootFilePath;

        if (!path.endsWith(SystemUtils.FILE_SEPARATOR)) {
            path += SystemUtils.FILE_SEPARATOR;
        }

        path += name;

        if (!name.endsWith(PROPERTIES_EXT)) {
            path += PROPERTIES_EXT;
        }

        return path;
    }

    public static InputStream convertGenericFileToInputStream(Exchange exchange) {

        File file = getFile(exchange);

        if (file != null) {
            return convertFileToInputStream(file);
        }
        return exchange.getIn().getBody(InputStream.class);
    }

    public static InputStream convertFileToInputStream(File file) {
        String fileName = file.getName();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            if (fileName != null && fileName.endsWith(".gz")) {
                return new GZIPInputStream(inputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException("could not find the file", e);
        }

        return inputStream;
    }

    public static InputStream convertGenericFileToInputStream(GenericFile genericFile) {
        File file = getFile(genericFile);
        return convertFileToInputStream(file);
    }

    public static File getFile(GenericFile file) {
        return (File) file.getBody();
    }

    public static File getFile(Exchange exchange) {
        GenericFile<File> genericFile = exchange.getIn().getBody(GenericFile.class);
        File file;

        if (genericFile == null) {
            file = exchange.getIn().getBody(File.class);
        } else {
            file = getFile(genericFile);
        }

        return file;
    }
}

class MultipleResourcesException extends RuntimeException {

    static final String MULTIPLE_RESOURCES = "Multiple resources found when expecting only one when " +
        "searching for resource with name "
    static final String NEXT_SENTENCE = ".  "
    static final String FOUND_RESOURCES = "The found resources are "

    List<URL> resources
    String resourceName

    @Override
    String getMessage() {
        return "${MULTIPLE_RESOURCES}${resourceName}${NEXT_SENTENCE}${FOUND_RESOURCES}${resources}"
    }
}
