package groovy

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyValidationResponse;
import com.jsoniter.JsonIterator;

import org.apache.commons.io.IOUtils

class CMPValidator {

	def GroovyValidationResponse validate(Object inputObject) {
		System.out.println("cmp request Validator started");
		int responseCode = 200;
		String responseFile = "";

		HttpRequestDto request = (HttpRequestDto)inputObject;

		String json = request.getContent();
	
		System.out.println("event routing data received >>>> " + json);
		
		Long msisdnVal = Long.parseLong(JsonIterator.deserialize(json).get(0).get("serviceId").toString());

		System.out.println("msisdn >>>> " + msisdnVal);
		
		if(msisdnVal >= 61555555510 && msisdnVal <= 61555555520) {
			responseCode=311;
			responseFile = "/jetty_mock_responses/cmp/EventRoutingFailureResponse.json";			
		}
		else {
			responseFile = "/jetty_mock_responses/cmp/EventRoutingMessageResponse.json";
		}
		
		return new GroovyValidationResponse(responseCode, responseFile);
	}
}
