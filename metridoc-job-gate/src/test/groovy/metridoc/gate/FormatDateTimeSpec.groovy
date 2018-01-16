package metridoc.gate

import spock.lang.Specification

class FormatDateTimeSpec extends Specification{
    void "test normal combine date and time columns"() {
    	given:
    	def row = ["10/1/2017", "1:00:00 PM"];

    	when:
    	def testResult = GateCSVFileService.reformatTime(row);

    	then:
    	testResult == "2017-10-01 13:00:00"
    }

    void "test combine date and time columns 12PM"() {
    	given:
    	def row = ["10/10/2017", "12:13:00 PM"];

    	when:
    	def testResult = GateCSVFileService.reformatTime(row);

    	then:
    	testResult == "2017-10-10 12:13:00"
    }

    void "test combine date and time columns 12AM"() {
    	given:
    	def row = ["10/10/2017", "12:13:00 AM"];

    	when:
    	def testResult = GateCSVFileService.reformatTime(row);

    	then:
    	testResult == "2017-10-10 00:13:00"
    }

    void "test combine date and time columns AM time"() {
    	given:
    	def row = ["10/9/2017", "3:10:00 AM"];

    	when:
    	def testResult = GateCSVFileService.reformatTime(row);

    	then:
    	testResult == "2017-10-09 03:10:00"
    }

    void "test combine date and time columns no AM/PM"() {
    	given:
    	def row = ["10/9/2017", "15:10:00"];

    	when:
    	def testResult = GateCSVFileService.reformatTime(row);

    	then:
    	testResult == "2017-10-09 15:10:00"
    }

    void "test combine date and time columns time has no seconds"() {
    	given:
    	def row = ["10/9/2017", "3:10 PM"];

    	when:
    	def testResult = GateCSVFileService.reformatTime(row);

    	then:
    	testResult == "2017-10-09 15:10:00"
    }

    void "test combine date and time columns time has no seconds no AM/PM"() {
    	given:
    	def row = ["10/9/2017", "3:10"];

    	when:
    	def testResult = GateCSVFileService.reformatTime(row);

    	then:
    	testResult == "2017-10-09 03:10:00"
    }

    void "test change datetime column format no seconds"() {
    	given:
    	def datetime = "10/9/2017 3:10"

    	when:
    	def testResult = GateCSVFileService.changeDatetimeFormat(datetime);

    	then:
    	testResult == "2017-10-09 03:10:00"
    }

    void "test change datetime column format with seconds"() {
    	given:
    	def datetime = "10/9/2017 3:10:00"

    	when:
    	def testResult = GateCSVFileService.changeDatetimeFormat(datetime);

    	then:
    	testResult == "2017-10-09 03:10:00"
    }
}
