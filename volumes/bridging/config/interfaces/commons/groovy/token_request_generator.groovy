package groovy

import org.json.JSONArray
import org.json.JSONObject

import com.ericsson.datamigration.bridging.converter.core.te.callbacks.GetMappingCacheInput
import com.ericsson.datamigration.bss.wfm.input.JSONInputData
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.ReadContext

class AuthRequestGenerator {

	//Generate querystring which will be sent as part of the token GET request
	def String prepareAuthRequest(Object input) {
	    String delimiter = '&';
		String clientId = "client_id=XYZ";
		String clientSecret = "client_secret=top_secret";
		String grantType = "grant_type=client_credentials";
		String scope = "scope=MESSAGEBROKER";
		String oauthParam = clientId + delimiter + clientSecret + delimiter + grantType + delimiter + scope;
		
		return oauthParam;
    }
}
