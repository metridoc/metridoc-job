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
		//check if the last 4 characters of the filename is ".csv"
		return filename[-4..-1] == '.csv';
	}
	
	public static void readFile(fileName, rowArr) {
		new File(fileName).eachLine {line ->
			//split each line into an array
		 	def row = line.split(',');
		 	//add processed row into rowArr
		 	rowArr.add(processRow(row));
		}
		//remove the first row, which is column headers
		rowArr.remove(0);
		//remove the last row, which is empty
		rowArr.remove(rowArr.size() - 1);
	}

	private static List processRow(row) {
		def datetime;
		def door;
		def affiliation;
		def center;
		def department;
		def usc;
		//if row size is 10 or 6, we assume that date and time are in 2 different columns
		//if row size is 9 or 5, we assume that date and time are in the same column
		//if row size is 5 or 6, we assume that no value was provided in the affiliation, center, department, USC cells
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
		//return an array without sensitive identification information
		return [datetime, door, affiliation, center, department, usc];
	}

	private static String reformatTime(row) {
		//combine column of date and column of time to be datetime
		//acceptable date format: MM/DD/YYYY
		//acceptable time format: HH:MM AM/PM; HH:MM:SS AM/PM; HH:MM:SS; HH:MM
		//convert into datetime format that can be accepted by SQL: YYYY-MM-DD HH:MM:SS
		//Ignore seconds in reformatting, all seconds are changed to 00
		def date = row[0].split('/');
		if(date.size() < 3){
			return "";
		}
		def datetime = date[2] + '-' + addZero(date[0]) + '-' + addZero(date[1]) + ' ';
		if(row[1][-2..-1] == 'AM' || row[1][-2..-1] == 'am') {
			row[1] = row[1].split(' ')[0];
			def time = row[1].split(':');
			if(time[0] == '12'){
				//if it is 12 AM, change to 00 o'clock (military time)
				time[0] = '00';
			}
			datetime += addZero(time[0]) + ':' + addZero(time[1]) + ':00';
		}else if(row[1][-2..-1] == 'PM' || row[1][-2..-1] == 'pm') {
			row[1] = row[1].split(' ')[0];
			def time = row[1].split(':');
			if(time[0] != '12'){
				//if it is 12 PM, keep it 12 o'clock 
				//if it is not 12 but it's PM, then add 12 to the original hour(military time)
				time[0] = String.valueOf(time[0].toInteger() + 12);
			}
			datetime += addZero(time[0]) + ':' + addZero(time[1]) + ':00';
		}else{
			//no AM/PM, then directly append time to date
			def time = row[1].split(':');
			datetime += addZero(time[0]) + ':' + addZero(time[1]) + ':00';
		}
		return datetime;
	}

	private static String changeDatetimeFormat(datetime){
		//change datetime format in only one column to match the format with SQL database
		//acceptable datetime format: MM/DD/YYYY HH:MM:SS or MM/DD/YYYY HH:MM
		//change into datetime format that can be accepted by SQL: YYYY-MM-DD HH:MM:SS
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
		//This function adds a 0 to single digits to keep the format consistent
		if(val.length() == 1){
			return '0'+val;
		}else{
			return val;
		}
	}
}