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

import metridoc.core.services.HibernateService
import metridoc.core.services.ConfigService

/*
    This test assumes that mdoc is run with -localMysql and -mergeMetridocConfig=false command
    see HibernateDataSourceSpec
 */
includeService(ConfigService)
def tool = includeService(HibernateService)
assert tool.localMysql: "localMysql should be true"
assert dataSource.url == "jdbc:mysql://localhost:3306/test"



