package groovy

import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyTemplateResponse;
import com.jsoniter.JsonIterator;
import java.util.Map;
import groovy.json.JsonSlurper;

class BAEGetUserData {

	def GroovyTemplateResponse validate(Object inputObject) {
		System.out.println("BAEGetUserData :: BAEGetUserData Intercetor started");
		int responseCode = 200;
		Map<String, Object> dataMap = new java.util.HashMap();
		HttpRequestDto request = (HttpRequestDto)inputObject;

		String method = request.getHttpMethod();
		String uri = request.getRequestUri();
		String json = request.getContent();

		def jsonSlurper = new JsonSlurper()
		//def accountOne = jsonSlurper.parseText('{"firstName":"Raam", "lastName":"Bhagat", "age":25 }')
		//def accountTwo = jsonSlurper.parseText('{"firstName":"Gopal", "lastName":"Bhagat", "age":30 }')
		//def accountThree = jsonSlurper.parseText('{"firstName":"Vaayu", "lastName":"Putra", "age":20 }')

		java.util.List users = new java.util.ArrayList();
		users.add(jsonSlurper.parseText('{"firstName":"Jon", "lastName":"Doe", "age":25 }'));
		users.add(jsonSlurper.parseText('{"firstName":"Richard", "lastName":"Gear", "age":30 }'));
		users.add(jsonSlurper.parseText('{"firstName":"Tom", "lastName":"Cruise", "age":20 }'));
		users.add(jsonSlurper.parseText('{"firstName":"Albert", "lastName":"Einstein", "age":21 }'));
		users.add(jsonSlurper.parseText('{"firstName":"Alba", "lastName":"Tross", "age":28 }'));
		users.add(jsonSlurper.parseText('{"firstName":"Shyam", "lastName":"Mishra", "age":26 }'));

		System.out.println("BAEGetUserData :: users >>>> " + users);

        dataMap.put("api", uri);
        dataMap.put("method", method);
		dataMap.put("uuid", java.util.UUID.randomUUID());
		dataMap.put("users", users);

		return new GroovyTemplateResponse(responseCode, dataMap);
	}
}