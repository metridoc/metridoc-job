configure()

import metridoc.gate.GateCSVFileService
import metridoc.gate.GateSQLService
import metridoc.gate.GateConvertNameToIdService
import java.util.Scanner; 

if(argsMap.params){
	if(argsMap.params.size() == 1){
		String fileName = argsMap.params[0];
		if(GateCSVFileService.checkFileName(fileName)){
			def rowArr = [];
			GateCSVFileService.readFile(fileName, rowArr);
			def doorMap = GateSQLService.getAllDoors();
			def affiliationMap = GateSQLService.getAllAffiliations();
			def centerMap = GateSQLService.getAllCenters();
			def departmentMap = GateSQLService.getAllDepartments();
			def uscMap = GateSQLService.getAllUSCs();
			GateConvertNameToIdService conversion = new GateConvertNameToIdService(doorMap, affiliationMap, centerMap, departmentMap, uscMap);
			def idRowArr = conversion.processRowArr(rowArr);
			GateSQLService.insertRecord(idRowArr);
		}else{
			println "Please provide a csv file"
		}
	}else if(argsMap.params.size() == 2){
		String month = argsMap.params[0];
		String year = argsMap.params[1];
		int monthInt = Integer.parseInt(month);
		int yearInt = Integer.parseInt(year);
		if (monthInt > 0  && monthInt < 13){
			println "Are you sure you want to delete all entry records in ${monthInt}/${yearInt}? (y/n)";
			Scanner scan = new Scanner(System.in);
			String s = scan.next();
			if(s == 'y'){
				println "Okay, deleting all entry records in ${monthInt}/${yearInt}...";
				GateSQLService.deleteEntryRecords(monthInt, yearInt);
				println("Deletion complete");
			}
		}else{
			println "Please provide a valid month"
		}
	}
}else{
	println "Please provide some arguments"	
}



