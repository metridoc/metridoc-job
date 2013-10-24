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



package metridoc.core.services

import groovy.sql.Sql
import metridoc.utils.DataSourceConfigUtil
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
class ConfigServiceSpec extends Specification {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    def "test setting fields based on flags"() {
        given: "mergeMetridocConfig and metridocConfigLocation flags are set"
        def configService = new ConfigService()
        def binding = new Binding()
        binding.args = [
                "-mergeMetridocConfig=false",
                "-metridocConfigLocation=foo/bar"
        ]

        when: "when the binding is set"
        configService.setBinding(binding)

        then: "appropriate fields should be set"
        !configService.mergeMetridocConfig
        "foo/bar" == configService.metridocConfigLocation
    }

    def "test in script adhoc configuration"() {
        when: "an adhoc configuration is created"
        def variable = new ConfigService().addConfig {
            foo.bar = "foobar"
        }.getVariable("foo.bar")

        then: "the variable can be extracted"
        "foobar" == variable
    }

    def "test from file adhoc configuration"() {
        given: "a configuration file"
        def file = folder.newFile("foobar")
        file.withPrintWriter {
            it.println("foo.bar = \"foobar\"")
        }

        when: "add file to configuration"
        def variable = new ConfigService().addConfig(file).getVariable("foo.bar")

        then: "the variable can be extracted"
        "foobar" == variable
    }

    def "test that the correct binding is used"() {
        given:
        def binding = new Binding()

        when:
        def configTool = binding.includeService(ConfigService)
        configTool.binding.foo = "bar"

        then:
        binding.hasVariable("foo")
    }

    void "test config cli args"() {
        given:
        Binding binding = new Binding()
        binding.args = ["-config.foo.bar=5"] as String[]

        when:
        def configTool
        configTool = binding.includeService(ConfigService)
        def config = binding.config

        then:
        5 == config.foo.bar
    }

    void "test instantiating dataSource"() {
        given:
        def config = new ConfigObject()
        DataSourceConfigUtil.addEmbeddedDataSource(config)
        DataSourceConfigUtil.addEmbeddedDataSource(config, "foo", "foo")
        config.dataSource_blah = "bad dataSource"

        when:
        def tool = new ConfigService()
        tool.initiateDataSources(config)

        then:
        tool.binding.dataSource
        tool.binding.sql
        tool.binding.dataSource_foo
        tool.binding.sql_foo
    }

    void "config from flag overrides home config"() {
        given:
        def metridocConfig = folder.newFile("MetridocConfig.groovy")
        def flagConfig = folder.newFile("FlagConfig.groovy")

        metridocConfig.withPrintWriter {
            it.println("foo = \"bar\"")
        }

        flagConfig.withPrintWriter {
            it.println("foo = \"foobar\"")
        }

        def configTool = new ConfigService(metridocConfigLocation: metridocConfig.path)
        def binding = new Binding()
        binding.args = ["-config=${flagConfig.path}"] as String[]

        when:
        configTool.binding = binding

        then:
        "foobar" == configTool.getVariable("foo")
    }

    void "test embedded dataSourceInjection"() {
        given:
        Binding binding = new Binding()

        when:
        def configService
        def args = ["-embeddedDataSource", "-mergeMetridocConfig=false"]
        binding.args = args as String[]
        binding.includeService(ConfigService)

        then:
        noExceptionThrown()
        binding.dataSource
        binding.sql
    }

    void "test dealing with flag overrides"() {

    }
}

class Foo {
    Sql sql
}
