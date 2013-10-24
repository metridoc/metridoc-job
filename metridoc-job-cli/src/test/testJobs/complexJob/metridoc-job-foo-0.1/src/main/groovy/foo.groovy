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
  *	permissions and limitations under the License.  */

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

