package groovy

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.utils.AuthenticationUtils
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyValidationResponse;
import org.apache.commons.io.IOUtils

class CSRequestValidator {

	def GroovyValidationResponse validate(Object inputObject) {
		System.out.println("CSRequestValidator started");
		int responseCode = 200;
		String responseFile = "";

		HttpRequestDto request = (HttpRequestDto)inputObject;

		byte[] inputDataAsBytes = request.getContentAsBytes();

		String serviceNumber = com.ericsson.datamigration.bridging.mock.servers.utils.IOUtils
		.getElementValue(inputDataAsBytes, "//methodCall/params/param/value/struct/member[name='subscriberNumber']/value/text()", null);
		
		String methodName = com.ericsson.datamigration.bridging.mock.servers.utils.IOUtils
		.getElementValue(inputDataAsBytes, "//methodCall/methodName/text()", null);
		
		System.out.println("serviceNumber >>>> " + serviceNumber);
		long serviceNumberVal = Long.parseLong(serviceNumber);

		if(methodName.equals("GeneralUpdate")) {
			if(serviceNumberVal == 61444444440) {
				responseFile = "/jetty_mock_responses/cs/GeneralUpdate_Error_Response.xml";
			} else {
				responseFile = "/jetty_mock_responses/cs/GeneralUpdate.xml";
			}
		} else if(methodName.equals("GeneralGet")) {
			responseFile = "/jetty_mock_responses/cs/GeneralGet.xml";
		}
		
		return new GroovyValidationResponse(responseCode, responseFile);
	}
}
