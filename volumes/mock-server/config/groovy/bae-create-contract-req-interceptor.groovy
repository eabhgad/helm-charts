package groovy

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyValidationResponse;
import com.jsoniter.JsonIterator;

import org.apache.commons.io.IOUtils

class BAECreateContractInterceptor {

	def GroovyValidationResponse validate(Object inputObject) {
		System.out.println("BAECreateContractInterceptor :: BAE Create Contract Request Intercetor started");
		int responseCode = 200;
		String responseFile = "";

		HttpRequestDto request = (HttpRequestDto)inputObject;

		// Expected request URI = bae/bssfSubscriptionManagement/v1/customerExternalId/custExtId/contractExternalId
		String reqUri = request.getRequestUri();
		// Extract value of custExtId from request field
		String[] arr = reqUri.split("[/]");
		String customerExternalId = (arr != null && arr.length == 7) ? arr[5] : "Null";
		
		String json = request.getContent();
		
		String data = JsonIterator.deserialize(json).get("externalId");

		System.out.println("BAECreateContractInterceptor :: customerExternalId >>>> " + customerExternalId);
		
		responseFile = "/jetty_mock_responses/bae/contract/create_contract_response.json";
		
		/**
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
		**/
			
		return new GroovyValidationResponse(responseCode, responseFile);
	}
}