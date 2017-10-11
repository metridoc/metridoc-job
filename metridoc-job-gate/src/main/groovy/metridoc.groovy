configure()

import metridoc.gate.GateCSVFileService
import metridoc.gate.GateSQLService
import metridoc.gate.GateConvertNameToIdService
import java.util.Scanner; 

if(argsMap.params){
	if(argsMap.params.size() == 1){
		//If there is only 1 parameter, we think the user wants to import a csv file
		String fileName = argsMap.params[0];
		if(GateCSVFileService.checkFileName(fileName)){
			//If the file name ends in ".csv"
			def rowArr = [];
			//Process each row in the csv file and add them to rowArr variable
			GateCSVFileService.readFile(fileName, rowArr);
			//For each category, get all existing options from the database and construct a map of [option name: option id]
			def doorMap = GateSQLService.getAllDoors();
			def affiliationMap = GateSQLService.getAllAffiliations();
			def centerMap = GateSQLService.getAllCenters();
			def departmentMap = GateSQLService.getAllDepartments();
			def uscMap = GateSQLService.getAllUSCs();
			//Put the maps into GateConvertNameToIdService 
			GateConvertNameToIdService conversion = new GateConvertNameToIdService(doorMap, affiliationMap, centerMap, departmentMap, uscMap);
			//Convert each row's names into their corresponding IDs
			def idRowArr = conversion.processRowArr(rowArr);
			//Insert the entry records
			GateSQLService.insertRecord(idRowArr);
		}else{
			//This is not a csv file
			println "Please provide a csv file"
		}
	}else if(argsMap.params.size() == 2){
		//If there are 2 arguments, we think the user wants to delete a month's worth of entry records in the databse
		String month = argsMap.params[0];
		String year = argsMap.params[1];
		int monthInt = Integer.parseInt(month);
		int yearInt = Integer.parseInt(year);
		if (monthInt > 0  && monthInt < 13){
			//if the month integer is valid
			println "Are you sure you want to delete all entry records in ${monthInt}/${yearInt}? (y/n)";
			//Ask for user's confirmation
			Scanner scan = new Scanner(System.in);
			String s = scan.next();
			if(s == 'y'){
				//User has confirmed that he wants to delete the data, we then proceed to delete the data
				println "Okay, deleting all entry records in ${monthInt}/${yearInt}...";
				GateSQLService.deleteEntryRecords(monthInt, yearInt);
				println("Deletion complete");
			}
		}else{
			//month integer invalid, print out error message and end the program
			println "Please provide a valid month"
		}
	}
}else{
	//no parameters, print out error message and end the program
	println "Please provide some parameters"	
}



