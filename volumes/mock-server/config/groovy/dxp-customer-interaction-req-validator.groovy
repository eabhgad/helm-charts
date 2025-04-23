package groovy

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyValidationResponse;
import com.jsoniter.JsonIterator;

import org.apache.commons.io.IOUtils

class DXPCustomerInteractionRequestValidator {

	def GroovyValidationResponse validate(Object inputObject) {
		System.out.println("DXPCustomerInteractionRequestValidator.validate method started");
		int responseCode = 200;
		String responseFile = "";

		HttpRequestDto request = (HttpRequestDto)inputObject;

		if("GET".equalsIgnoreCase(request.getHttpMethod())) {
			responseFile = "/jetty_mock_responses/dxp/DXP_GetCustomerInteraction_Response.json";
		} else {
			String json = request.getContent();
			String journeyId = JsonIterator.deserialize(json).get("journeyId");
			System.out.println("journeyId >>>> " + journeyId);
			if(journeyId != null && journeyId.equals("deactivateContract")) {
				responseFile = "/jetty_mock_responses/dxp/DXP_CreateCustomerInteraction_Deactivation_Response.json";
			} else {
				responseFile = "/jetty_mock_responses/dxp/DXP_CreateCustomerInteraction_Response.json";
			}
		}
	
		return new GroovyValidationResponse(responseCode, responseFile);
	}
}