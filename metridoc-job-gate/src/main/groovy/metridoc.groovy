configure()

import metridoc.gate.GateCSVFileService
import metridoc.gate.GateSQLService
import metridoc.gate.GateConvertNameToIdService

if(argsMap.params){
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
}else{
	println "Please provide a csv file"	
}



