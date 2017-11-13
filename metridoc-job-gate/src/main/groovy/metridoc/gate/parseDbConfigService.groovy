package metridoc.gate

import groovy.util.logging.Slf4j
import metridoc.core.Step
import grails.util.Environment

class parseDbConfigService {
	public static HashMap<String, String> parseDbAuthenticationInfo(){
		String configFileDir = System.getProperty("user.home")+'/.metridoc/MetridocConfig.groovy';
		String dbConfigInfoString = new File(configFileDir).text
		def dbConfigInfoArr = dbConfigInfoString.split("\n");

		def enviroment = Environment.current.toString().toLowerCase();

		def correctEnv = false;
		def envLen = enviroment.length();

		HashMap<String, String> resultMap = new HashMap<String, String>();
		def noEnv = false;

		if(dbConfigInfoArr[0].substring(0,10) == "dataSource"){
			noEnv = true;
		}

		for(int i=0; i<dbConfigInfoArr.length;i++){
			def currentString = dbConfigInfoArr[i].stripIndent();
			def currentStringLength = currentString.length();
			if(currentStringLength > envLen && currentString.substring(0,envLen) == enviroment){
				correctEnv = true;
			}
			if(correctEnv || noEnv){
				if(currentStringLength > 8 && currentString.substring(0,8) == "username"){
					String value = extractValueFromConfigOption(currentString)
					resultMap.put("username",value);
				}else if(currentStringLength > 8 && currentString.substring(0,8) == "password"){
					String value = extractValueFromConfigOption(currentString)
					resultMap.put("password",value);
				}else if(currentStringLength > 3 && currentString.substring(0,3) == "url"){
					String value = extractValueFromConfigOption(currentString)
					resultMap.put("url",value);
				}else if(currentStringLength > 15 && currentString.substring(0,15) == "driverClassName"){
					String value = extractValueFromConfigOption(currentString)
					resultMap.put("driver",value);
				}
			}

		}
		return resultMap;
	}

	private static String extractValueFromConfigOption(option){
		def untrimmedValue = option.split('=')[1];
		def value = untrimmedValue.stripIndent();
		def length = value.length();
		return value.substring(1, length-1);
	}
}