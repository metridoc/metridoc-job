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

package metridoc.camel;

import groovy.lang.Closure;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * copied from https://svn.codehaus.org/grails-plugins/grails-camel/trunk/src/java/org/ix/grails/plugins/camel/ClosureProcessor.java
 * for project http://www.grails.org/plugin/camel.
 * <p/>
 * executes a processor closure
 *
 * @author Tommy Barker
 * @deprecated
 */
public class ClosureProcessor implements Processor {

    private Closure target;

    public ClosureProcessor(Closure target) {
        this.target = target;
    }

    public void process(Exchange exchange) throws Exception {
        this.target.call(exchange);
    }
}
