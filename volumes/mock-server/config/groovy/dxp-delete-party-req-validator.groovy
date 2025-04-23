package groovy

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyValidationResponse;
import com.jsoniter.JsonIterator;

import org.apache.commons.io.IOUtils

class DxpDeletePartyValidator {

	def GroovyValidationResponse validate(Object inputObject) {
		System.out.println("DXP delete Validator started");
		int responseCode = 204;
		String responseFile = "/jetty_mock_responses/dxp/DXP_DeletePartyResp.json";
    	return new GroovyValidationResponse(responseCode, responseFile);
	}
}