package groovy

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyValidationResponse;
import com.jsoniter.JsonIterator;

import org.apache.commons.io.IOUtils

class EDNotifyValidator {

	def GroovyValidationResponse validate(Object inputObject) {
		System.out.println("ED NotifyValidator started");
		int responseCode = 200;
		String responseFile = "";

		HttpRequestDto request = (HttpRequestDto)inputObject;

		String json = request.getContent();
		
		String data = JsonIterator.deserialize(json).get("data");

		System.out.println("data >>>> " + data);
		
		String subscriberId = JsonIterator.deserialize(data).get("subscriberId");

		System.out.println("subscriberId >>>> " + subscriberId);

		long msisdnVal = JsonIterator.deserialize(data).get("serviceNumber").toLong();

		System.out.println("msisdn >>>> " + msisdnVal);
		
		
		if(msisdnVal >= 61111111110 && msisdnVal <= 61111111120) {
			responseCode = 311;
			responseFile = "/jetty_mock_responses/event_dispatcher/TelstraNotificationErrorResponse.json";
		} else {
			responseFile = "/jetty_mock_responses/event_dispatcher/TelstraNotificationResponse.json";
		}
		
		return new GroovyValidationResponse(responseCode, responseFile);
	}
}
