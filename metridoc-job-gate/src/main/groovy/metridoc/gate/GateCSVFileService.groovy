package metridoc.gate

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import metridoc.core.Step
import metridoc.core.services.CamelService
import metridoc.core.tools.CamelTool
import metridoc.core.tools.RunnableTool

import javax.sql.DataSource
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.io.File 

@Slf4j
class GateCSVFileService {

	public static boolean checkFileName(filename) {
		return filename[-4..-1] == '.csv';
	}
	
	public static void readFile(fileName, rowArr) {
		new File(fileName).eachLine {line ->
		 	def row = line.split(',');
		 	rowArr.add(processRow(row));
		}
		rowArr.remove(0);
		rowArr.remove(rowArr.size() - 1);
	}

	private static List processRow(row) {
		def datetime;
		def door;
		def affiliation;
		def center;
		def department;
		def usc;
		if(row.size() == 10 || row.size() == 6){
			datetime = reformatTime(row);
			door = row[2];
			if(row.size() == 6){
				affiliation = '';
				center = '';
				department = '';
				usc = '';
			}else{
				affiliation = row[6];
				center = row[7];
				department = row[8];
				usc = row[9];
			}
		}else if(row.size() == 9){
			datetime = changeDatetimeFormat(row[0]);
			door = row[1];
			affiliation = row[5]
			center = row[6]
			department = row[7]
			usc = row[8]
		}else if(row.size() == 5){
			datetime = changeDatetimeFormat(row[0]);
			door = row[1];
			affiliation = '';
			center = '';
			department = '';
			usc = '';
		}else{
			return [];
		}
		return [datetime, door, affiliation, center, department, usc];
	}

	private static String reformatTime(row) {
		//combine column of date and column of time to be datetime
		def date = row[0].split('/');
		if(date.size() < 3){
			return "";
		}
		def datetime = date[2] + '-' + addZero(date[0]) + '-' + addZero(date[1]) + ' ';
		if(row[1][-2..-1] == 'AM' || row[1][-2..-1] == 'am') {
			row[1] = row[1].split(' ')[0];
			def (hour, minute, second) = row[1].split(':');
			if(hour == '12'){
				hour = '00';
			}
			datetime += addZero(hour) + ':' + addZero(minute) + ':00';
		}else if(row[1][-2..-1] == 'PM' || row[1][-2..-1] == 'pm') {
			row[1] = row[1].split(' ')[0];
			def (hour, minute, second) = row[1].split(':');
			if(hour != '12'){
				hour = String.valueOf(hour.toInteger() + 12);
			}
			datetime += addZero(hour) + ':' + addZero(minute) + ':00';
		}
		return datetime;
	}

	private static String changeDatetimeFormat(datetime){
		//change datetime format in only one column to match the format with SQL database
		def dateAndTime = datetime.split(' ');
		if(dateAndTime.size()<2){
			return '';
		}
		def date = dateAndTime[0];
		def time = dateAndTime[1];
		def dateSplit = date.split('/');
		if(dateSplit.size() == 3){
			date = dateSplit[2] + '-' + addZero(dateSplit[0]) + '-' + addZero(dateSplit[1]);
		}
		def timeSplit = time.split(':');
		if(timeSplit.size() > 0){
			time = addZero(timeSplit[0]) + ':' + addZero(timeSplit[1]) + ':00';
		}
		return date + ' ' + time;
	}

	private static String addZero(val){
		if(val.length() == 1){
			return '0'+val;
		}else{
			return val;
		}
	}
}