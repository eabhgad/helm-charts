package groovy

import com.ericsson.datamigration.bridging.converter.core.wfm.input.ConverterRequest
import com.ericsson.datamigration.bridging.commons.core.enums.ProtocolType;
import com.ericsson.datamigration.bridging.commons.core.util.JsoniterJsonHelper;
import com.ericsson.datamigration.bridging.converter.core.enums.RequestValidationResultCode;
import org.apache.commons.text.CaseUtils

import java.util.Map;
import java.util.List;

import com.jsoniter.JsonIterator
import com.jsoniter.ValueType
import com.jsoniter.any.Any
import com.jsoniter.spi.JsonException

class MigrationWorkflowRequestBuilder {

	private static final String INTERFACE_ID = "MigrationService"
	private static final String OPERATION_ID = "MigrateCustomer"
	private static final String SERVICE_NUMBER_ATTRIBUTE = "serviceNumber"
	private static final String TRANSACTION_TYPE_ATTRIBUTE = "transitionType"
	private static final String CORRELATION_ID_ATTRIBUTE = "correlationId"
	private static final String SIM_SN_NUMBER = "simSN"
	private static final String BRAND = "brand"
	
	def ConverterRequest buildWorkflowRequest(Object inputObject) {
		List<Object> args = (List<Object>)inputObject;
		String input = (String)args.get(0);
		Map<String, String> headers = (Map<String, String>)args.get(1); 

		Any inputObj = JsonIterator.deserialize(input);
		
		ConverterRequest converterRequest = new ConverterRequest();
		converterRequest.setInterfaceId(INTERFACE_ID);
		converterRequest.setOperationId(OPERATION_ID);
		converterRequest.setBodyType(ProtocolType.valueOf("HTTP"));
		String subscriberId = inputObj.toString(SERVICE_NUMBER_ATTRIBUTE);
		String simSN = inputObj.toString(SIM_SN_NUMBER);
		simSN = transformSimSN(simSN);
		String brand = inputObj.toString(BRAND);
		brand =  transformBrand(brand);
		converterRequest.setSubscriberId(subscriberId);
		converterRequest.setRequestType(inputObj.toString(TRANSACTION_TYPE_ATTRIBUTE));
		converterRequest.setCorrelationId(inputObj.toString(CORRELATION_ID_ATTRIBUTE));
		try {
			Map<String, Object> inputData = (Map<String, Object>)inputObj.as(Map.class);
			inputData.put(SIM_SN_NUMBER,simSN);
			inputData.put(BRAND,brand);
			inputData.put("msisdn", subscriberId);
			inputData.put("requestType", inputObj.toString(TRANSACTION_TYPE_ATTRIBUTE));
			inputData.putAll(headers);
			converterRequest.setParameters(inputData);
		} catch(JsonException je) {
			System.err.println("Unable to deserialize input json.", je);
		}
		converterRequest.setRequestValidationResultCode(RequestValidationResultCode.OK);
		converterRequest.setBody(input);
		
		return converterRequest;
	}
	
	def String transformSimSN(String simSn){
	String transformedSimSN = simSn;
	if(simSn.length() == 20){
	 transformedSimSN = simSn.substring(6,18)+simSn.substring(19,20);
	}
	return transformedSimSN;
	}

	def String transformBrand(String brand){
		return CaseUtils.toCamelCase(brand,true);
	}
}