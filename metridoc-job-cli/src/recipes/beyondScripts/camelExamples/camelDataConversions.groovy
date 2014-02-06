import metridoc.core.services.CamelService
import org.apache.camel.Exchange

/**
 * Created by tbarker on 2/5/14.
 */
def service = includeService(CamelService)

service.send("seda:foo", "bar1")
service.send("seda:foo", "bar2")
//grabbing exchange
service.consume("seda:foo") {Exchange exchange ->
    println exchange.in.body
}
//grabbing exchange
service.consume("seda:foo") {String message ->
    println message
}