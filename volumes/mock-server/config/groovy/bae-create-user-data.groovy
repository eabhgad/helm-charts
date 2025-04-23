package groovy

import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyTemplateResponse;
import com.jsoniter.JsonIterator;
import java.util.Map;

class BAECreateUserData {

	def GroovyTemplateResponse validate(Object inputObject) {
		System.out.println("BAECreateUserData :: BAECreateUserData Intercetor started");
		int responseCode = 200;
		Map<String, Object> dataMap = new java.util.HashMap();
		HttpRequestDto request = (HttpRequestDto)inputObject;

		String method = request.getHttpMethod();
		String uri = request.getRequestUri();
		String json = request.getContent();
		
		String userId = JsonIterator.deserialize(json).get("userId");

		System.out.println("BAECreateUserData :: userId >>>> " + userId);

        dataMap.put("api", uri);
        dataMap.put("method", method);
        dataMap.put("user", userId);
		dataMap.put("uuid", java.util.UUID.randomUUID());
		
		return new GroovyTemplateResponse(responseCode, dataMap);
	}
}