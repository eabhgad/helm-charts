package groovy

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ericsson.datamigration.bridging.commons.core.dto.ValidationResponse;
import com.ericsson.datamigration.bridging.commons.core.validation.ErrorMessage;
import com.ericsson.datamigration.bridging.commons.core.model.database.Account;

class SoapLookupResponseBuilder {

	private static final String ACCOUNT_KEY = "account";
	private static final String ERROR_KEY = "error";
	
	private static final String GENERIC_ERROR_CODE = "Error while processing request.";
	private static final String INVALID_PARAM_CODE = "InvalidParameterValue";
	private static final String PARAMETER_MISSING_CODE = "ParameterMissing";
	
	private static final def ALLOWED_TYPES = ["MSISDN", "CustomerId", "VirtualAccountId"]; 
	
	private static final Logger logger = LoggerFactory.getLogger(SoapLookupResponseBuilder.class);
	
	def execute(Object inputMap) {

		logger.debug("SoapLookupResponseBuilder.execute started ...");

		Map<String, Object> input = (Map<String, Object>)inputMap;
		Account account = (Account)input.get(ACCOUNT_KEY);
		ErrorMessage error = (ErrorMessage) input.get(ERROR_KEY);
		
		if(account != null) {
			return getSuccessResponse(account);
		}
		
		return getErrorResponse(error);
	}

	def getSuccessResponse(Account account) {
		StringBuilder sb = new StringBuilder("");
		sb.append("<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">");
		sb.append("<S:Body>");
		sb.append("<LookupServiceDBLookupReadResponse xmlns=\"http://bss.bridging.ericsson.com/soap/dbLookup\">");
		sb.append("<TYPE_OF_IDENTIFIER>");
		sb.append(account.getTypeofidentifier());
		sb.append("</TYPE_OF_IDENTIFIER>");
		sb.append("<IDENTIFIER>");
		sb.append(account.getIdentifier());
		sb.append("</IDENTIFIER>");
		sb.append("<STATUS>");
		sb.append(account.getStatus());
		sb.append("</STATUS>");
		sb.append("</LookupServiceDBLookupReadResponse>");
		sb.append("</S:Body>");
		sb.append("</S:Envelope>");
		return sb.toString();
	}

	def getErrorResponse(ErrorMessage errorMsg) {
		if(errorMsg == null) {
			return internalError();
		} else {
			StringBuilder sb = new StringBuilder("");
			sb.append("<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">");
			sb.append("<S:Body>");
			sb.append("<S:Fault xmlns:ns4=\"http://www.w3.org/2003/05/soap-envelope\">");
			sb.append("<faultCode>");
			sb.append(errorMsg.getErrorMessage().getCode());
			sb.append("</faultCode>");
			sb.append("<faultString>");
			sb.append(errorMsg.getErrorMessage().getMessage());
			sb.append("</faultString>");
			sb.append("<details>");
			sb.append(errorMsg.getErrorMessage().getDetails());
			sb.append("</details>");
			sb.append("</S:Fault>");
			sb.append("</S:Body>");
			sb.append("</S:Envelope>");
			return sb.toString();
		}
	}
	
	def internalError() {
		StringBuilder sb = new StringBuilder("");
		sb.append("<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">");
		sb.append("<S:Body>");
		sb.append("<S:Fault xmlns:ns4=\"http://www.w3.org/2003/05/soap-envelope\">");
		sb.append("<faultCode>InternalServerError</faultCode>");
		sb.append("<faultString>Internal error while processing request.</faultString>");
		sb.append("<details>An exception occurred while processing the request.</details>");
		sb.append("</S:Fault>");
		sb.append("</S:Body>");
		sb.append("</S:Envelope>");
		return sb.toString();
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