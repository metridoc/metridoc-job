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
		//intialize the maps to the parameters passed into this constructor
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
				//convert names to IDs for each row
				newRowArr.add(processedRow);
			}
		}
		//Insert new category options in case there is any
		GateSQLService.insertAllCategories();
		return newRowArr;
	}

	private static List processRow(row) {
		//convert each category name to its corresponding ID
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
		//if name is empty string and there is a "N/A" option in map, return "N/A"'s ID
		if(name == '' && map.containsKey('N/A')){return map['N/A']};
		//ID found, return the ID corresponding to name
		if(map[name] >= 0){return map[name]};
		//name is of null value, return -1
		if(name == null){return -1};

		//It is a new option in the category, create a new ID for this option, which is
		//the 1 + max ID of the cateogry.
		int id = map.size();
		if(name == ''){
			//if name is empty sting, then create 'N/A' in map
			name = 'N/A';
		}
		map.put(name, id);

		//Add the new category option into the map in GateSQLService waiting for insertion
		//into the database
		GateSQLService.addToNewMaps(name, id, category);

		return id;
	}
}