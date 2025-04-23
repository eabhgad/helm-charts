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
		String[] tokenParams =  json.split("&");
		if(tokenParams != null) {
			for(String tokenParam : tokenParams) {
				if(tokenParam.startsWith("scope=")) {
					String scopeTokenValue = tokenParam.replace("scope=", "");
					
					if(scopeTokenValue.equals("MESSAGEBROKER")) {
						responseFile = "/jetty_mock_responses/oauth_token/TelstraOAuthResponse_ED.json";
					} else if(scopeTokenValue.equals("AMDOCSOAUTH")) {
						responseFile = "/jetty_mock_responses/oauth_token/TelstraOAuthResponse_EOMSYS.json";
					} else if(scopeTokenValue.equals("CMPRoutingService")) {
						responseFile = "/jetty_mock_responses/oauth_token/TelstraOAuthResponse_CMP.json";
					} else {
						responseFile = "/jetty_mock_responses/oauth_token/TelstraOAuthResponse_DXP.json";
					}
				}
			}
		}
		
		return new GroovyValidationResponse(responseCode, responseFile);
	}
}
