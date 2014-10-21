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

package metridoc.ezproxy.services

import groovy.stream.Stream
import groovy.util.logging.Slf4j
import metridoc.core.InjectArgBase
import metridoc.core.services.CamelService
import metridoc.core.services.DataSourceService
import metridoc.core.services.DefaultService
import metridoc.ezproxy.entities.EzproxyBase
import metridoc.service.gorm.GormService
import org.apache.camel.util.URISupport
import org.hibernate.Session

import java.util.zip.GZIPInputStream

/**
 * Created with IntelliJ IDEA on 9/24/13
 * @author Tommy Barker
 */
@InjectArgBase("ezproxy")
@Slf4j
class EzproxyIngestService extends DefaultService {

    EzproxyStepsService ezproxyService
    CamelService camelService
    long waitForFile = 1000 * 60 * 3 //3 minutes
    boolean preview
    public static final String FILE_FILTER_IS_NULL = "ezproxy file filter cannot be null"
    public static final String EZ_DIRECTORY_IS_NULL = 'ezproxy directory or camelUrl must not be null'
    public static final String DEFAULT_FILE_FILTER = "ezproxy*"
    public static final Closure<String> EZ_DIRECTORY_DOES_NOT_EXISTS = { "ezproxy directory ${it} does not exist" as String }
    public static final Closure<String> EZ_FILE_DOES_NOT_EXIST = { "ezproxy file $it does not exist" as String }

    String fileFilter = DEFAULT_FILE_FILTER
    File directory
    File file
    String camelUrl
    Class<? extends EzproxyBase> entityClass

    void ingestData() {
        validateInputs()
        checkConnection()

        String fileUrl = createFileUrl()

        def usedUrl = camelUrl ?: fileUrl
        //this creates a file transaction
        def sanitizedUrl = URISupport.sanitizeUri(usedUrl)
        log.info "consuming from [${sanitizedUrl}]"
        boolean atLeastOneFileProcessed = false
        camelService.consumeWait(usedUrl, waitForFile) { File file ->

            if (file) {
                atLeastOneFileProcessed = true
                log.info "processing file $file"
                def ezproxyIterator = getEzproxyIterator(file)
                ezproxyIterator.validateInputs()
                if (preview) {
                    ezproxyIterator.preview()
                    return
                }
                else {
                    doIngest(ezproxyIterator, entityClass)
                }
            }
        }

        if (!atLeastOneFileProcessed) {
            log.info "no files were processed, if this is unexpected, consider extending the wait time to retrieve the file\n" +
                    "  command line: use --waitForFile=<milliseconds>"
            "  config file: use ezproxy.waitForFile=<milliseconds>"
        }

        camelService.close()
    }

    protected void checkConnection() {
        if (!preview) {
            DataSourceService gormService = includeService(GormService)

            try {
                gormService.enableFor(entityClass)
            }
            catch (IllegalStateException ignored) {
                //do nothing, already enabled
            }

            entityClass.withNewSession { Session session ->
                def url = session.connection().metaData.getURL()
                log.info "connecting to ${url}"
            }
        }
    }

    protected static List doIngest(EzproxyIteratorService ezproxyIteratorService,
                                   Class<? extends EzproxyBase> entity) {
        Map stats = [
                ignored: 0,
                written: 0
        ]

        def helperInstance = entity.newInstance()
        def entitiesSaved = []
        Set<String> naturalKeyCache = [] as Set
        int counter = 1
        entity.withTransaction {
            Stream.from(ezproxyIteratorService).map {
                if (counter % 10000 == 0) {
                    owner.log.info "procesed [$counter] records with stats [$stats]"
                }
                counter++
                return it
            }.filter {
                boolean result
                if (it.exception) {
                    log.error "Exception: ${it.exception}"
                    result = false
                }
                else {
                    result = helperInstance.acceptRecord(it)
                    //log.info "Accepted! ${result?:'WAT'}"
                }
                if (!result) {
                    stats.ignored = stats.ignored + 1
                }

                return result
            }.map {
                def instance = entity.newInstance()
                instance.populate(it)
                stats.written = stats.written + 1
                instance.naturalKeyCache = naturalKeyCache
                return instance
            }.filter { EzproxyBase base ->
                def response = base.shouldSave()
                if (!response) {
                    stats.written = stats.written - 1
                    stats.ignored = stats.ignored + 1
                }
                return response
            }.each { EzproxyBase base ->
                base.save(failOnError: true)
                entitiesSaved << base
            }

            counter--
            log.info "finished procesing [$counter] records with stats [$stats]"
            assert counter == stats.written + stats.ignored: "stats and total lines processed don't match"
        }

        return [stats, entitiesSaved]
    }

    protected EzproxyIteratorService getEzproxyIterator(File file) {
        def inputStream = file.newInputStream()
        def fileName = file.name
        if (fileName.endsWith(".gz")) {
            inputStream = new GZIPInputStream(inputStream)
        }
        def service = includeService(EzproxyIteratorService, inputStream: inputStream, file: file)
        return service
    }

    protected String createFileUrl() {
        String fileUrl
        if (file) {
            assert file.exists(): "$file does not exist"
            directory = new File(file.parent)
        }

        long readLockTimeout = 1000 * 60 * 60 * 24 //one day
        if (directory) {
            fileUrl = "${directory.toURI().toURL()}?noop=true&readLockTimeout=${readLockTimeout}&antInclude=${fileFilter}&sendEmptyMessageWhenIdle=true&filter=#ezproxyFileFilter"
        }
        fileUrl
    }

    void validateInputs() {
        if (!file) {
            assert fileFilter: FILE_FILTER_IS_NULL
            assert directory || camelUrl: EZ_DIRECTORY_IS_NULL
            if (directory) {
                log.info "Validate directory: ${directory}"
                assert directory.exists(): EZ_DIRECTORY_DOES_NOT_EXISTS(directory)
            }
            else{
                log.info "Validate camelUrl: ${camelUrl}"
            }
        }
        else {
            assert file.exists(): EZ_FILE_DOES_NOT_EXIST(file)
            log.info "Validate file: ${file}"
        }
    }
}
