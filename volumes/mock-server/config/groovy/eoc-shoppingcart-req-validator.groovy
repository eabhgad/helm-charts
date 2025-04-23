package groovy

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyValidationResponse;
import com.jsoniter.JsonIterator;

import org.apache.commons.io.IOUtils

class EOCShoppingCartRequestValidator {

	def GroovyValidationResponse validate(Object inputObject) {
		System.out.println("EOCShoppingCartRequestValidator.validate method started");
		int responseCode = 200;
		String responseFile = "/jetty_mock_responses/eoc/EOCResponse.json";

		HttpRequestDto request = (HttpRequestDto)inputObject;

		if("POST".equalsIgnoreCase(request.getHttpMethod())) {
			responseCode = 201;
		} else if("PATCH".equalsIgnoreCase(request.getHttpMethod())) {
			responseCode = 200;
			responseFile = "/jetty_mock_responses/eoc/EOC_UpdateShoppingCart_Response.json";
		} else if("DELETE".equalsIgnoreCase(request.getHttpMethod())) {
			responseCode = 204;
			responseFile = "/jetty_mock_responses/eoc/EOC_Delete_Op_Response.txt";
		} 
	
		return new GroovyValidationResponse(responseCode, responseFile);
	}
}