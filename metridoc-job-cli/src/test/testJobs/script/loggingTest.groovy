import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger("metridoc.foo")

log.info "some info"
log.warn "warning"
log.error "an error"
println "from println"
System.out.println "from out"
System.err.println "from err"
new RuntimeException("oops", new RuntimeException("the cause")).printStackTrace()
