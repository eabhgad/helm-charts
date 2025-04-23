package groovy

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyValidationResponse;
import com.jsoniter.JsonIterator;

import org.apache.commons.io.IOUtils

class DxpSearchCustomerValidator {

	def GroovyValidationResponse validate(Object inputObject) {
		System.out.println("DXP request Validator started");
		int responseCode = 200;
		String responseFile = "";

		HttpRequestDto request = (HttpRequestDto)inputObject;

		String json = request.getContent();
		
		String data = JsonIterator.deserialize(json).get("target");

		System.out.println("data >>>> " + data);
		
		String accountUUID = JsonIterator.deserialize(data).get(0).get("value");
		

		System.out.println("accountUUID >>>> " + accountUUID);

		//long msisdnVal = JsonIterator.deserialize(data).get(0).get("value").toLong();

		//System.out.println("accountUUID >>>> " + msisdnVal);		
		
		if(accountUUID.equals("invalidsearch")) {
			responseCode=311;
			responseFile = "/jetty_mock_responses/dxp/DXP_SearchCustomerErrorResp.json";
		}
		else if(accountUUID.equals("searchfound")){
			responseFile = "/jetty_mock_responses/dxp/DXP_SearchCustomer_Found.json";
		}		
		else { 			
			//responseCode = 400;
			responseFile = "/jetty_mock_responses/dxp/DXP_SearchCustomer_NotFound.json";
		}				
		return new GroovyValidationResponse(responseCode, responseFile);
	}
}
