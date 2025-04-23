package groovy

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils

import com.ericsson.datamigration.bridging.commons.core.constants.BridgingConstants
import com.ericsson.datamigration.bridging.commons.core.dto.ValidationResponse;
import com.ericsson.datamigration.bridging.commons.core.model.database.Account;
import com.ericsson.datamigration.bridging.commons.core.model.database.LocationInfo

import static com.ericsson.datamigration.bridging.commons.core.constants.BridgingConstants.OPERATION_TYPE;
import static com.ericsson.datamigration.bridging.commons.core.constants.BridgingConstants.CREATE_OPERATION;
import static com.ericsson.datamigration.bridging.commons.core.constants.BridgingConstants.UPDATE_OPERATION;
import static com.ericsson.datamigration.bridging.commons.core.constants.BridgingConstants.DELETE_OPERATION;
import static com.ericsson.datamigration.bridging.commons.core.constants.BridgingConstants.ACCOUNT_UPDATE_OPERATION;
import static com.ericsson.datamigration.bridging.commons.core.constants.BridgingConstants.REQUEST_CONTENT;

class SubscriberManageRequestValidator {

	private static final String TYPE_OF_IDENTIFIER = "type";
	private static final String IDENTIFIER = "identifier";
	private static final String STATUS = "status";
	
	private static final String GENERIC_ERROR_CODE = "Error while processing request.";
	private static final String INVALID_PARAM_CODE = "InvalidParameterValue";
	private static final String PARAMETER_MISSING_CODE = "ParameterMissing";
	
	private static final def ALLOWED_TYPES = ["MSISDN", "CustomerId", "VirtualAccountId"];

	private static final def ALLOWED_STATUS = ["MIGRATED", "NOT_MIGRATED"];
	
	private final def actionValues = ["ADD", "UPDATE", "DELETE"];
	
	private static final Logger logger = LoggerFactory.getLogger(SubscriberManageRequestValidator.class);
	
	def ValidationResponse validate(Object inputObject) {

		logger.debug("SubscriberManageRequestValidator.validate started ...");
		Map<String, Object> input = (Map<String, Object>)inputObject;

		logger.debug("SubscriberManageRequestValidator:: requestOperation: {}, account : {}", (String)input.get(OPERATION_TYPE), (Account)input.get(REQUEST_CONTENT));
		
		String requestOperation = (String)input.get(OPERATION_TYPE);
		
		switch(requestOperation) {
			case CREATE_OPERATION:
				return this.validateCreateSubscriberRequest((Account)input.get(REQUEST_CONTENT));
			case UPDATE_OPERATION:
				return this.validateCreateSubscriberRequest((Account)input.get(REQUEST_CONTENT));
			case DELETE_OPERATION:
				break;
			case ACCOUNT_UPDATE_OPERATION:
				break;
		}

		return new ValidationResponse("200", "Success", "The request is successfully validated.", true);
	}
	
	def validateCreateSubscriberRequest(Account accountValidate) {
		def response = validateBasicParams(accountValidate);
		
		if(response != true)
			return response;
		
		response = validateAccountInfo(accountValidate, false);
		if(response != true)
			return response;

		return new ValidationResponse("200", "Success", "The request is successfully validated.", true);
	}

	def validateUpdateSubscriberRequest(Account accountValidate) {

		def response = validateBasicParams(accountValidate);
		if(response != true)
			return response;
		
		response = validateAccountInfo(accountValidate, true);
		if(response != true)
			return response;

		return new ValidationResponse("200", "Success", "The request is successfully validated.", true);
	}
		
	def validateBasicParams(Account accountValidate) {
		def result = validateValue(TYPE_OF_IDENTIFIER, accountValidate.getTypeofidentifier(), ALLOWED_TYPES);
		if( result != true )
			return result;
			
		result = validateValue(IDENTIFIER, accountValidate.getIdentifier(), null);
		if( result != true )
			return result;

		result = validateValue(STATUS, accountValidate.getStatus(), ALLOWED_STATUS);
		if( result != true )
			return result;
	
		return true;
	}
	
	
	/**
	 * Checks locations from the account received to be created
	 * @param accountValidate
	 * @param checkUpdate It checks more information in case the checking is for an update account
	 */
	def validateAccountInfo(Account accountValidate, boolean checkUpdate) {
		if(accountValidate.getLocations() != null && !accountValidate.getLocations().isEmpty()) {
			for(LocationInfo loc: accountValidate.getLocations()) {
				if(StringUtils.isEmpty(loc.name))
					return missingRequiredPatamater("accountInfo.name"); 
				if(StringUtils.isEmpty(loc.value))
					return missingRequiredPatamater("accountInfo.value");
	
				//In case the checking is for an update we enhance the checks
				if(checkUpdate) {
					if(StringUtils.isEmpty(loc.action))
						return missingRequiredPatamater("action");
					if(actionValues != null && !actionValues.contains(loc.action)) 
						return invalidValue ("accountInfo.action", loc.action, actionValues.join(", "));
				
				}
			}
		}
		return true;
	}
	
	def validateValue(String attrName, String attrValue, List allowedValues)
	{
		if(attrValue == null)
			return missingParameter(attrName, attrValue);
		
		if(attrValue.isEmpty()) 
			return invalidValue(attrName, attrValue, allowedValues);
		
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

	def missingParameter (String paramName, String paramValue) {
		String errorCode = PARAMETER_MISSING_CODE;
		String errorMsg = "Required parameter '"+paramName+"' is not present.";
		String errorDetails = "";
		logger.error("{} : {} : {}", errorCode, errorMsg, errorDetails);
		return new ValidationResponse(errorCode, errorMsg, errorDetails, false);
	}
	
}