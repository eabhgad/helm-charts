package groovy

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyValidationResponse;
import com.jsoniter.JsonIterator;

import org.apache.commons.io.IOUtils

class DxpCreatePartyValidator {

	def GroovyValidationResponse validate(Object inputObject) {
		System.out.println("DXP request Validator started");
		int responseCode = 200;
		String responseFile = "";

		HttpRequestDto request = (HttpRequestDto)inputObject;

		String json = request.getContent();
		
		String data = JsonIterator.deserialize(json).get("externalId");

		System.out.println("data >>>> " + data);
		
		if(data.equals("invalidparty")) {
			responseCode = 311;
			responseFile = "/jetty_mock_responses/dxp/DXP_CreatePartyErrorResp.json";
		}
		
		else if(data.equals("invaliddeleteparty")){ 
			responseFile = "/jetty_mock_responses/dxp/DXP_CreatePartyResp_DeletePartyFailure.json";
		}		
		else {
			responseFile = "/jetty_mock_responses/dxp/DXP_CreatePartyResp.json";
		}
			
		return new GroovyValidationResponse(responseCode, responseFile);
	}
}