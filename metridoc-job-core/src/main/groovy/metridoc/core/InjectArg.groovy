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



package metridoc.core

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Used to suggest to any service / tool on how items are injected when included.  You can either target
 * based on the command line, config or both.  As a last resort, if neither work a service / arg will
 * be injected based on name.  This annotation is just a suggestion and injection should not be
 * considered required.
 *
 * By default when a service / tool is included, fields are injected by name no matter what.  This
 * behaviour can be turned off with <code>ignore</code> to bypass injection entirely, or set
 * <code>injectByName</inject> to false and only inject by config or cli
 *
 * @author Tommy Barker
 *
 *
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectArg {
    String config() default ""
    String cli() default ""
    boolean ignore() default false
    boolean injectByName() default true
}