package groovy

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyValidationResponse;
import com.jsoniter.JsonIterator;

import org.apache.commons.io.IOUtils

class EomsysCreateSubmitValidator {

	def GroovyValidationResponse validate(Object inputObject) {
		System.out.println("EOMSYS create submit request validator started");
		
		//contactUUID
		
		HttpRequestDto request = (HttpRequestDto)inputObject;

		String json = request.getContent();
		
		String data = JsonIterator.deserialize(json).get("relatedParty");

		System.out.println("data >>>> " + data);
		
		String contactUUID = JsonIterator.deserialize(data).get(0).get("id");

		System.out.println("subscriberId >>>> " + contactUUID);

		//long msisdnVal = JsonIterator.deserialize(data).get(0).get("value").toLong();

		//System.out.println("msisdn >>>> " + msisdnVal);		
		
		int responseCode = 201;
		String responseFile = "/jetty_mock_responses/eomsys/CreateOrderResponse.json";
		
		if(contactUUID.equals("5555555555") ){
		 responseCode = 404;
		 responseFile = "Error in submitting order";
		}
			
		return new GroovyValidationResponse(responseCode, responseFile);
	}
}