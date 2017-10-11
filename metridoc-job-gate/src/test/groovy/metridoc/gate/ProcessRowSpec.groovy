package metridoc.gate

import spock.lang.Specification

class ProcessRowSpec extends Specification{
    void "test process row with length 10"() {
    	given:
    	def row = ['10/1/2017', '1:00:00 PM','VAN PELT LIBRARY EMPLOYEE REAR DOOR_ *VPL','0000000000','TEST1','TEST 2','PENN STAFF','FACILITIES','HOUSEKEEPING','Staff']

    	when:
    	def testResult = GateCSVFileService.processRow(row);

    	then:
    	testResult == ["2017-10-01 13:00:00", 'VAN PELT LIBRARY EMPLOYEE REAR DOOR_ *VPL', 'PENN STAFF','FACILITIES','HOUSEKEEPING','Staff']
    }

    void "test process row with length 6"() {
    	given:
    	def row = ['10/1/2017', '1:00:00 PM','VAN PELT LIBRARY EMPLOYEE REAR DOOR_ *VPL','0000000000','TEST1','TEST 2']

    	when:
    	def testResult = GateCSVFileService.processRow(row);

    	then:
    	testResult == ["2017-10-01 13:00:00", 'VAN PELT LIBRARY EMPLOYEE REAR DOOR_ *VPL', '','','','']
    }

    void "test procee row with length 9"() {
    	given:
    	def row = ['2017-02-01 00:28:15.000','VAN PELT LIBRARY EMPLOYEE REAR DOOR_ *VPL','0000000000','TEST1','TEST 2','PENN STAFF','FACILITIES','HOUSEKEEPING','Staff']

    	when:
    	def testResult = GateCSVFileService.processRow(row);

    	then:
    	testResult == ["2017-02-01 00:28:00", 'VAN PELT LIBRARY EMPLOYEE REAR DOOR_ *VPL', 'PENN STAFF','FACILITIES','HOUSEKEEPING','Staff']
    }

    void "test procee row with length 5"() {
    	given:
    	def row = ['2017-02-01 00:28:15.000','VAN PELT LIBRARY EMPLOYEE REAR DOOR_ *VPL','0000000000','TEST1','TEST 2']

    	when:
    	def testResult = GateCSVFileService.processRow(row);

    	then:
    	testResult == ["2017-02-01 00:28:00", 'VAN PELT LIBRARY EMPLOYEE REAR DOOR_ *VPL', '','','','']
    }

}
