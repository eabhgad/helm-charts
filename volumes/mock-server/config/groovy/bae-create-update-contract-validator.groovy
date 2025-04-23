package groovy

import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyTemplateResponse;
import com.jsoniter.JsonIterator;
import java.util.Map;

class BAECreateCustomerData {

	def GroovyTemplateResponse validate(Object inputObject) {
		System.out.println("BAECreateCustomerData :: BAECreateCustomerData Intercetor started");
		int responseCode = 200;
		Map<String, Object> dataMap = new java.util.HashMap();
		HttpRequestDto request = (HttpRequestDto)inputObject;
		String httpMethod = request.getHttpMethod();

		// Extract value of custExtId from request URI
		// Expected request URI = bae/bssfSubscriptionManagement/v1/customerExternalId/custExtId/contractExternalId
		String reqUri = request.getRequestUri();
		String[] arr = reqUri.split("[/]");
		String customerExternalId = (arr != null && arr.length >= 7) ? arr[5] : "Null";
		System.out.println("BAECreateCustomerData :: URI arr >>>> " + arr);

		String uri = request.getRequestUri();
		String json = request.getContent();
		String correlationId = JsonIterator.deserialize(json).get("correlationId");

		dataMap.put("api", uri);
		dataMap.put("method", httpMethod);
		dataMap.put("correlationId", correlationId);
		dataMap.put("custExtId", customerExternalId);
		if(httpMethod != null && httpMethod.equalsIgnoreCase("PATCH")) {
			String contExtId = (arr != null && arr.length >= 8) ? arr[7] : "Null";
			dataMap.put("contExtId", contExtId);
		}

		System.out.println("BAECreateCustomerData :: dataMap >>>> " + dataMap);

		return new GroovyTemplateResponse(responseCode, dataMap);
	}
}