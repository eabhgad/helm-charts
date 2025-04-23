package groovy

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ericsson.datamigration.bridging.commons.core.dto.ValidationResponse;

class SubscriberLookupRequestValidator {

	private static final String TYPE_OF_IDENTIFIER = "type";
	private static final String IDENTIFIER = "identifier";
	
	private static final String GENERIC_ERROR_CODE = "Error while processing request.";
	private static final String INVALID_PARAM_CODE = "InvalidParameterValue";
	private static final String PARAMETER_MISSING_CODE = "ParameterMissing";
	
	private static final def ALLOWED_TYPES = ["MSISDN", "CustomerId", "VirtualAccountId"]; 
	
	private static final Logger logger = LoggerFactory.getLogger(SubscriberLookupRequestValidator.class);
	
	def ValidationResponse validate(Object inputObject) {

		logger.debug("SubscriberLookupRequestValidator.validate started ...");

		Map<String, String> input = (Map<String, String>)inputObject;
		String type = input.get(TYPE_OF_IDENTIFIER);
		String identifier = input.get(IDENTIFIER);
		
		logger.debug("SubscriberLookupRequestValidator.validate intput : type [{}] identifier [{}]", type, identifier);
		
		def result = validateValue(TYPE_OF_IDENTIFIER, type, ALLOWED_TYPES);
		if( result != true )
			return result;

		result = validateValue(IDENTIFIER, identifier, null);
		if( result != true )
			return result;

		return new ValidationResponse("200", "Success", "The request is successfully validated.", true);
		
	}


	def validateValue(String attrName, String attrValue, List allowedValues) 
	{
		if(attrValue == null || attrValue.trim().isEmpty()) {
			return invalidValue(attrName, attrValue, allowedValues);
		}
		if(allowedValues != null && !allowedValues.contains(attrValue)) {
			return invalidValue(attrName, attrValue, allowedValues);
		}
		return true;
	}
	
	def invalidValue (String paramName, String paramValue, List allowedValues) {
		String errorCode = INVALID_PARAM_CODE;
		String errorMsg = "Parameter '"+paramName+"' has invalid value '"+paramValue+"'.";
		String errorDetails = (allowedValues == null ? "" : "Allowed values : "+allowedValues.join(", "));
		logger.error("{} : {} : {}", errorCode, errorMsg, errorDetails);
		return new ValidationResponse(errorCode, errorMsg, errorDetails, false);
	}
}