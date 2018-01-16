package metridoc.gate

import spock.lang.Specification

class ConvertNameToIdSpec extends Specification{

    void "test convert existing name in maps"() {
    	given:
    	def emptyMap = [:];
    	def affiliationMap = [:];
		def uscMap = [:];
		affiliationMap.put('PENN STAFF', 0);
		affiliationMap.put('SERVICE PROVIDER', 1);
		affiliationMap.put('STUDENT', 2);
		affiliationMap.put('PENN ALUMNI', 3);
		affiliationMap.put('VISITING SCHOLAR', 4);
		affiliationMap.put('VISITING STUDENT', 5);
		uscMap.put('Staff', 0);
		uscMap.put('Temporary', 1);
		uscMap.put('Contractor', 2);
		uscMap.put('Student', 3);
		uscMap.put('Penn Alumni', 4);
		uscMap.put('Visiting Scholar', 5);
		uscMap.put('Visiting Student', 6);
    	GateConvertNameToIdService nameToIdServ = new GateConvertNameToIdService(emptyMap, affiliationMap, emptyMap, emptyMap, uscMap)

    	when:
    	def id1 = nameToIdServ.convertNameToInt("PENN STAFF", affiliationMap, 'affiliation')
    	def id2 = nameToIdServ.convertNameToInt("VISITING SCHOLAR", affiliationMap, 'affiliation')
    	def id3 = nameToIdServ.convertNameToInt("Temporary", uscMap, 'USC')
    	def id4 = nameToIdServ.convertNameToInt("Student", uscMap, 'USC')

    	then:
    	id1 == 0
    	id2 == 4
    	id3 == 1
    	id4 == 3
    }

    void "test convert non-existing name in maps"() {
    	given:
    	def emptyMap = [:];
    	def affiliationMap = [:];
		def uscMap = [:];
		affiliationMap.put('PENN STAFF', 0);
		affiliationMap.put('SERVICE PROVIDER', 1);
		affiliationMap.put('STUDENT', 2);
		affiliationMap.put('PENN ALUMNI', 3);
		affiliationMap.put('VISITING SCHOLAR', 4);
		affiliationMap.put('VISITING STUDENT', 5);
		uscMap.put('Staff', 0);
		uscMap.put('Temporary', 1);
		uscMap.put('Contractor', 2);
		uscMap.put('Student', 3);
		uscMap.put('Penn Alumni', 4);
		uscMap.put('Visiting Scholar', 5);
		uscMap.put('Visiting Student', 6);
    	GateConvertNameToIdService nameToIdServ = new GateConvertNameToIdService(emptyMap, affiliationMap, emptyMap, emptyMap, uscMap)

    	when:
    	def id1 = nameToIdServ.convertNameToInt("", affiliationMap, 'affiliation')
    	def id2 = nameToIdServ.convertNameToInt("Faculty", affiliationMap, 'affiliation')
    	def id3 = nameToIdServ.convertNameToInt(null, affiliationMap, 'affiliation')

    	then:
    	id1 == 6
    	id2 == 7
    	affiliationMap['N/A'] == 6
    	affiliationMap['Faculty'] == 7
    	id3 == -1
    }

}
