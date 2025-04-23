package groovy

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.utils.AuthenticationUtils
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyValidationResponse;
import org.apache.commons.io.IOUtils

class OrpheusResponseValidator {

	def namespaces = [soapenv: 'http://schemas.xmlsoap.org/soap/envelope/',
                      v3: 'http://www.telstra.com/ServiceDetails/v3']


	def GroovyValidationResponse validate(Object inputObject) {
		System.out.println("OrpheusResponseValidator started");
		int responseCode = 200;
		String responseFile = "";

		HttpRequestDto request = (HttpRequestDto)inputObject;

		String xmlRequest = new String(request.getContentAsBytes());

		String msisdn = getValueOfXmlElement(xmlRequest, 'msisdn');

		String migrationFlag = getValueOfXmlElement(xmlRequest, 'migrationFlag');
			
		//System.out.println("msisdn >>>> " + msisdn);
		long msisdnVal = Long.parseLong(msisdn);
		int migrationFlagVal = Integer.parseInt(migrationFlag);
		//System.out.println("msisdnVal >>>> " + msisdnVal);
		
		String filepath = "/jetty_mock_responses/orpheus/"+msisdnVal +".xml";
		
		boolean isExists = findFileByName(filepath);
		
		System.out.println("response file Exists >>>> " + isExists);
		
		if(isExists){
			responseFile = filepath;
			System.out.println("Sending response from file name: >>>> " + responseFile);
		}
		else if(msisdnVal >= 222222220 && msisdnVal <= 222222240) {
			responseCode = 404;
			responseFile = "/jetty_mock_responses/orpheus/Orpheus_Error_Response.xml";
		} else {
			if(msisdnVal == 444444430) {
				responseFile = "/jetty_mock_responses/orpheus/OrpheausResponse_For_4444444430.xml";
			} else if(msisdnVal == 444444435) {
				responseFile = "/jetty_mock_responses/orpheus/OrpheausResponse_For_4444444435.xml";
			} else if(msisdnVal == 444444440) {
				responseFile = "/jetty_mock_responses/orpheus/OrpheausResponse_For_4444444440.xml";
			} 
			else if(msisdnVal == 444444444 && migrationFlagVal == 0) {
				responseCode = 404;
				responseFile = "/jetty_mock_responses/orpheus/Orpheus_Error_Response.xml";
			}
			else if(msisdnVal == 444444425){
				 responseFile = "/jetty_mock_responses/orpheus/OrpheausResponse_For_4444444425_ImsiNull.xml";							
			}else {
				if(msisdnVal == 444444445 && migrationFlagVal == 2) {
					responseFile = "/jetty_mock_responses/orpheus/OrpheausResponse_ConsistencyCheck.xml";
				}
				else if(msisdnVal == 555555519 && migrationFlagVal == 2) {
					responseFile = "/jetty_mock_responses/orpheus/OrpheausResponse_ConsistencyCheck.xml";
				}
				 else {
					//responseFile = "/jetty_mock_responses/orpheus/OrpheausResponse.xml";
					responseFile = "/jetty_mock_responses/orpheus/OrpheausResponseTemplate.xml";
				}
			}
		}
		
		return new GroovyValidationResponse(responseCode, responseFile);
	}
	
	def boolean findFileByName(String filepath) {
		try{
			String baseDir = AuthenticationUtils.getResourcesPath();
			File file = new File(baseDir+filepath);
			//System.out.println("file path: " + baseDir+filepath);
			if(file!=null && file.exists()){
				return true;
			}
		}
		catch(Exception e){
			System.out.println("no files found: "+filepath);
		}
		return false;
	}
	
	def getValueOfXmlElement(String xmlContent, String xmlElement) {
		def xmlRequestContent = new XmlSlurper().parseText(xmlContent)
		xmlRequestContent.declareNamespace(namespaces)
		return xmlRequestContent.'soapenv:Body'.'v3:GetServiceDetailsRequest'."${xmlElement}".text()
	}		
}
