package metridoc.gate

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import metridoc.core.Step
import metridoc.core.services.CamelService
import metridoc.core.tools.CamelTool
import metridoc.core.tools.RunnableTool
import metridoc.service.gorm.GormService
import metridoc.utils.DataSourceConfigUtil

import java.sql.*;
import java.text.SimpleDateFormat

@Slf4j
class GateSQLService {
	private static String getAllDoors = '''SELECT door_id AS id, door_name AS name FROM gate_door;'''

	private static String getAllAffiliations = '''SELECT affiliation_id AS id, affiliation_name AS name FROM gate_affiliation;'''

	private static String getAllCenters = '''SELECT center_id AS id, center_name AS name FROM gate_center;'''

	private static String getAllDepartments = '''SELECT department_id AS id, department_name AS name FROM gate_department;'''

	private static String getAllUSCs = '''SELECT USC_id AS id, USC_name AS name FROM gate_USC;'''

	private static String insertDoorStmt = '''INSERT INTO gate_door (door_id, door_name) VALUES (?, ?)''';
	private static String insertAffiliationStmt = '''INSERT INTO gate_affiliation (affiliation_id, affiliation_name) VALUES (?, ?)'''
	private static String insertCenterStmt = '''INSERT INTO gate_center (center_id, center_name) VALUES (?, ?)''';
	private static String insertDepartmentStmt = '''INSERT INTO gate_department (department_id, department_name) VALUES (?, ?)''';
	private static String insertUSCStmt = '''INSERT INTO gate_USC (USC_id, USC_name) VALUES (?, ?)''';

	private static String insertRecordStmt = '''INSERT INTO gate_entry_record (entry_datetime, door, affiliation, center, department, usc) VALUES (?, ?, ?, ?, ?, ?)'''

	private static String getMaxIdStmt = '''SELECT max(entry_id) FROM gate_entry_record;''';

	private static String deleteEntryRecordsByMonth = '''DELETE FROM gate_entry_record WHERE entry_datetime BETWEEN ? AND ?;'''

	private static Connection conn = null;

	private static String url;
	private static String driver;
	private static String userName; 
	private static String password;
	private static int maxId;
	private static Statement stmt;

	private static HashMap<String, Integer> newDoors = [:];
	private static HashMap<String, Integer> newAffiliations = [:];
	private static HashMap<String, Integer> newCenters = [:];
	private static HashMap<String, Integer> newDepartments = [:];
	private static HashMap<String, Integer> newUSCs = [:];

	public static void setPasswordUsername(String urlAddr, String drv, String un, String pd){
		url = urlAddr;
		driver = drv;
		userName = un;
		password = pd;
	}

	public static HashMap<String, Integer> getQuery(query){
		def map = [:];
		try
     	{
			Class.forName(driver);
			conn = DriverManager.getConnection(url,userName,password);
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
	            int id = rs.getInt("id");
	            String name = rs.getString("name");
	            map.put(name, id);
	        }
			conn.close();
		} catch (Exception e) {
         	e.printStackTrace();
		}
    	return map;
	}

	public static void insertRecord(rowArr){
		try {
	        Connection conn = DriverManager.getConnection(url,userName,password);
	        conn.setAutoCommit(false);

	        PreparedStatement statement = conn.prepareStatement(insertRecordStmt);
	        int curId = maxId + 1;
	        int i = 0;
	        for (def row : rowArr) {
	        	if(row){
		            statement.setTimestamp(1, java.sql.Timestamp.valueOf(row[0]));
		            statement.setInt(2, row[1]);
		            statement.setInt(3, row[2]);
		            statement.setInt(4, row[3]);
		            statement.setInt(5, row[4]);
		            statement.setInt(6, row[5]);
		            statement.addBatch();
		            curId++;
		            i++;

		            if (i % 1000 == 0 || i == rowArr.size()) {
		                statement.executeBatch(); // Execute every 1000 items.
		            	println("Inserted "+ i + " rows");
		            }
	        	}
	        }
	        conn.commit();
            println("Finished");
            conn.close();
	    }catch (Exception e) {
         	e.printStackTrace();
         	println("Rolling back data here....");
         	try{
			 if(conn!=null)
			    conn.rollback();
			}catch(SQLException se2){
			  se2.printStackTrace();
			}
			System.exit(0);
		}
	}

	public static void insertAllCategories(){
		insertQuery(insertDoorStmt, newDoors, "gate_door");
		insertQuery(insertAffiliationStmt, newAffiliations, "gate_affiliation");
		insertQuery(insertCenterStmt, newCenters, "gate_center");
		insertQuery(insertDepartmentStmt, newDepartments, "gate_department");
		insertQuery(insertUSCStmt, newUSCs, "gate_USC");
	}

	private static Boolean insertQuery(query, map, category){
		try
     	{
			Class.forName(driver);
			conn = DriverManager.getConnection(url,userName,password);
			conn.setAutoCommit(false);
			PreparedStatement preparedStmt = conn.prepareStatement(query);
			int i = 0;
			for (String keyName : map.keySet()) {
				preparedStmt.setInt(1, map.get(keyName));
				preparedStmt.setString(2, keyName);
				preparedStmt.addBatch();
				i++;
			}
			preparedStmt.executeBatch();
			conn.commit();
			println("Inserted " + i + " rows into "+ category +" table");
			
			conn.close();
		} catch (Exception e) {
         	e.printStackTrace();
         	println("Rolling back data here....");
         	try{
			 if(conn!=null)
			    conn.rollback();
			}catch(SQLException se2){
			  se2.printStackTrace();
			}
			System.exit(0);
         	return false;;
		}
		return true;
	}

	private static String addZero(val){
		if(val.length() == 1){
			return '0'+val;
		}else{
			return val;
		}
	}

	public static void deleteEntryRecords(month, year) {
		String monthStr = addZero(Integer.toString(month))
		String startDatetime = "${year}-${monthStr}-01 00:00:00";
		String endDatetime;
		if(month == 12){
			year++;
			endDatetime = "${year}-01-01 00:00:00"
		}else{
			month++;
			monthStr = addZero(Integer.toString(month));
			endDatetime = "${year}-${monthStr}-01 00:00:00";
		}
		try
     	{
			Class.forName(driver);
			conn = DriverManager.getConnection(url,userName,password);
			PreparedStatement preparedStmt = conn.prepareStatement(deleteEntryRecordsByMonth);
			preparedStmt.setString(1, startDatetime);
			preparedStmt.setString(2, endDatetime);
			
			preparedStmt.executeUpdate();
			
			conn.close();
		} catch (Exception e) {
         	e.printStackTrace();
         	println("Deleteion failed");
			System.exit(0);
		}
	}

	public static HashMap<String, Integer> getAllDoors() {
		return getQuery(getAllDoors);
	}

	public static HashMap<String, Integer> getAllAffiliations() {
		return getQuery(getAllAffiliations);
	}

	public static HashMap<String, Integer> getAllCenters() {
		return getQuery(getAllCenters);
	}

	public static HashMap<String, Integer> getAllDepartments() {
		return getQuery(getAllDepartments);
	}

	public static HashMap<String, Integer> getAllUSCs() {
		return getQuery(getAllUSCs);
	}

	public static void addToNewMaps(name, id, category) {
		if(category == 'door'){
			newDoors.put(name, id);
		}else if(category == "affiliation"){
			newAffiliations.put(name, id);
		}else if(category == "center"){
			newCenters.put(name, id);
		}else if(category == "department"){
			newDepartments.put(name, id);
		}else if(category == "USC"){
			newUSCs.put(name, id);
		}
	}
}
