import entity.Bar
@Grab(group = 'com.github.stefanbirkner', module = 'system-rules', version = '1.3.1')
import metridoc.core.tools.HibernateTool
@Grab(group='com.github.stefanbirkner', module='system-rules', version='1.3.1')
import org.junit.contrib.java.lang.system.StandardOutputStreamLog
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource

assert new ClassPathResource("fileInFoo.txt").exists()

includeService(embeddedDataSource:true, HibernateTool).enableFor(Bar)


println "complex foo ran"
LoggerFactory.getLogger("metridoc.foo").info "logging from foo"
return "complex foo project ran"

