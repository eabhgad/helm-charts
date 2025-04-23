package groovy

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import com.ericsson.datamigration.bridging.mock.servers.dto.HttpRequestDto;
import com.ericsson.datamigration.bridging.mock.servers.dto.GroovyValidationResponse;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any

import org.apache.commons.io.IOUtils
import org.json.JSONArray
import org.json.JSONObject

class ModifyResponse {

	def namespaces = [soapenv: 'http://schemas.xmlsoap.org/soap/envelope/',
                      v3: 'http://www.telstra.com/ServiceDetails/v3']

	def String modifyResponseJson(Object inputObject) {
		//System.out.println("inside modify Response json: " + inputObject);
		List list = (List)inputObject;
		String serviceName = (String)list.get(0);
		String responseData = (String)list.get(1);
		String requestData = list.get(2);

		String response  = responseData;
		if(serviceName.equals("DXP_SEARCH_API") && responseData!=null && responseData.trim().length()>2 && responseData.trim().startsWith("[")){
			// customer found
			JSONObject jsonObj = new JSONObject((String)JsonIterator.deserialize(responseData).get(0));
			if(jsonObj.optString("id")!=null){
				String reqData = JsonIterator.deserialize(requestData).get("target");
				Any accountUUID = JsonIterator.deserialize(reqData).get(0).get("value");
				jsonObj.put("id", accountUUID);
				JSONArray array = new JSONArray();
				array.put(jsonObj);
				response = array.toString();
			}
		}
		else if(serviceName.equals("DXP_CREATE_CUSTOMER_API") && responseData!=null && responseData.trim().length()>2){
			String customerId = JsonIterator.deserialize(requestData).get("customerId");
			JSONObject responseJson = new JSONObject(response);
			if(responseJson.optString("customerId")!=null){
				responseJson.put("customerId", customerId);
				response = responseJson.toString();

			}
		}
		else if(serviceName.equals("ORPHEUS") && responseData!=null && responseData.trim().length()>2 && responseData.contains("@MSISDN@")){
			def xmlRequestContent = new XmlSlurper().parseText(requestData)
			xmlRequestContent.declareNamespace(namespaces)
			String msisdn = xmlRequestContent.'soapenv:Body'.'v3:GetServiceDetailsRequest'.msisdn.text()
			String resposneMsisdn =  "61"+msisdn.substring(1)
			response = responseData.replaceAll("@MSISDN@",resposneMsisdn);
			//System.out.println("================> ModifyResponse : inputMsisdn ["+msisdn+"] outputMsisdn["+resposneMsisdn+"]");
		}

		return response;
	}
}