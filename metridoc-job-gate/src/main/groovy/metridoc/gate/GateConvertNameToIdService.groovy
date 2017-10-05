package metridoc.gate

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import metridoc.core.Step

import java.text.SimpleDateFormat

@Slf4j
class GateConvertNameToIdService {
	private static HashMap<String, Integer> doors = new HashMap<String, Integer>();
	private static HashMap<String, Integer> affiliations = new HashMap<String, Integer>();
	private static HashMap<String, Integer> centers = new HashMap<String, Integer>();
	private static HashMap<String, Integer> departments = new HashMap<String, Integer>(); 
	private static HashMap<String, Integer> uscs = new HashMap<String, Integer>();

	GateConvertNameToIdService(doorMap, affiliationMap, centerMap, departmentMap, uscMap){
		doors = doorMap;
		affiliations = affiliationMap;
		centers = centerMap;
		departments = departmentMap;
		uscs = uscMap;
	}

	public static List processRowArr(rowArr) {
		def newRowArr = [];
		for(row in rowArr) {
			def processedRow = processRow(row);
			if(processedRow.size() == 6){
				newRowArr.add(processedRow);
			}
		}
		GateSQLService.insertAllCategories();
		return newRowArr;
	}

	private static List processRow(row) {
		if(row.size() == 6){
			String datetime = row[0]; 
			int doorId = convertNameToInt(row[1], doors, 'door');
			int affiliationId = convertNameToInt(row[2], affiliations, 'affiliation');
			int centerId = convertNameToInt(row[3], centers, 'center');
			int departmentId = convertNameToInt(row[4], departments, 'department');
			int uscId = convertNameToInt(row[5], uscs, 'USC');
			if(datetime != null) {
				return [datetime, doorId, affiliationId, centerId, departmentId, uscId];
			}
		}else{
			return [];
		}
	}

	private static int convertNameToInt(name, map, category) {
		if(name == '' && map.containsKey('N/A')){return map['N/A']};
		if(map[name] >= 0){return map[name]};
		if(name == null){return -1};

		int id = map.size();
		if(name == ''){
			name = 'N/A';
		}
		map.put(name, id);

		GateSQLService.addToNewMaps(name, id, category);

		return id;
	}
}