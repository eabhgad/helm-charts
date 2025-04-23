package groovy
import com.ericsson.datamigration.bridging.commons.core.dto.ValidationResponse
import com.ericsson.datamigration.bridging.dispatcher.core.service.async.services.MigrationService
import com.ericsson.datamigration.bridging.commons.core.util.LogUtils
import com.jsoniter.JsonIterator
import com.jsoniter.ValueType
import com.jsoniter.any.Any
import com.jsoniter.spi.JsonException
import java.util.List;
import java.util.Arrays;
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.format.DateTimeFormatter;
import com.ericsson.datamigration.bridging.converter.core.te.callbacks.GetMappingCacheInput;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;

class MigrationRequestValidator {

	private final String transitionType = "transitionType";
	private final String packageId = "packageId";
	private final String brand = "brand";
	private final String cdborCidn = "cdborCidn";
	private final String correlationId = "correlationId";
	private final String resourceId = "resourceId";
	private final String serviceNumber  = "serviceNumber";
	private final String accountUUID = "accountUUID";
	private final String oldPlan = "oldPlan";
	private final String newPlan = "newPlan";
	private final String customerId = "customerId";
	private final String contactUUID = "contactUUID";
	private final String simSN = "simSN";
	private final String pin = "pin";
	private final String puk = "puk";
	private final String simCategory = "simCategory";
	private final String firstName = "firstName";
	private final String middleName = "middleName";
	private final String lastName = "lastName";
	private final String title = "title";
	private final String email = "email";
	private final String contactNumber = "contactNumber";
	private final String dob = "doB";
	private final String addressLine1 = "addressLine1";
	private final String postalCode = "postalCode";
	private final String city = "city";
	private final String state = "state";
	private final String callRestrictionLevel = "callRestrictionLevel";
	private final String iddDiversion  = "iddDiversion";
	private final String cliSettings = "cliSettings";
	private final String messageBankType = "messageBankType";
	private final String messageBankTimezone = "messageBankTimezone";
	private final String internationalRoaming = "internationalRoaming";
	private final String customerType = "customerType";
	private final String serviceAddressID = "serviceAddressID";
	private final String listingType = "listingType";

	
	// Define regular expression pattern for MSISDN
	// The pattern below specifies
	//	- value should start & end with digit
	//	- length of value should be between 1 to 11 digits
	private final def MSISDN_PATTERN = ~/^\d{11}$/

	//private final def SIM_SN_PATTERN = ~/^\d{20}$/
	private final def SIM_SN_PATTERN = ~/^\d{13}|\d{20}$/
	
	def ValidationResponse validate(Object inputObject) {

		//log.debug("---------------> MigrationRequestValidator.validate started....")

		String input = (String)inputObject;

		//log.debug("---------------> validation of input : " + input)
		
		Any inputObj = JsonIterator.deserialize(input);

		def validationResult = validateRequestParams(inputObj);
		if(validationResult != true) {
			return new ValidationResponse("400", "Bad request", validationResult ,false);
		}
		//log.debug("MigrationRequestValidator.validate done.")

		ValidationResponse response = new ValidationResponse("200", "Success", "The migration request is successfully validated and the migration workflow is being initiated.", true)
		response.addAttribute("subscriberId", inputObj.toString(this.serviceNumber))
		response.addAttribute("subscriberType", "MSISDN")
		return response;
		
	}

	def validateRequestParams(Any input) {
		Any jsonObj = null;
		String attrValue=""
		def result
		List allowedValues = null;

		/**
		 * validate transitionType property
		 **/
		result = validateValue(input.get(this.transitionType), this.transitionType)
		if(result != true)
			return result;
			
		/**
		 * Validate 'correlationId' attribute
		 */
		result = validateValue(input.get(this.correlationId), this.correlationId)
		if(result != true)
			return result;

		/**
		 * Validate 'resourceId' attribute
		 */
		result = validateValue(input.get(this.resourceId), this.resourceId)
		if(result != true)
			return result;

		/**
		 * Validate 'serviceNumber' attribute
		 */
		result = validateValue(input.get(this.serviceNumber), this.serviceNumber)
		if(result != true)
			return result;

		//The serviceNumber should only contain digits
		jsonObj  = input.get(this.serviceNumber);
		attrValue = jsonObj.toString();
		def matcher = attrValue =~ MSISDN_PATTERN
		if(!matcher.matches()) {
			return "The parameter: '$serviceNumber' has an invalid value: '$attrValue'. The value of '$serviceNumber' should contain only digits and it should be 11 digits.";
		}
		
		if(!attrValue.startsWith("61")) {
			return "The parameter: '$serviceNumber' has an invalid value: '$attrValue'. The value of '$serviceNumber' should starts with 61";
		}

		/**
		 * Validate 'accountUUID' attribute
		 */
		result = validateValue(input.get(this.accountUUID), this.accountUUID)
		if(result != true)
			return result;

			
;


		return true;
	}

	def validateDob(String date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		try {
			LocalDate localDate=	 LocalDate.parse(date , formatter);
			if(!localDate.isAfter(LocalDate.now())){
				String[] params = date.split("-");
				int month = Integer.parseInt(params[1]);
				int day = Integer.parseInt(params[2]);
				int year =  Integer.parseInt(params[0]);
				boolean isSuccess = validateDate(day,month,year);
				if(!isSuccess){
					return "The parameter: 'doB' has an invalid value: '$date'"
			}
		}
		else{
		 return	"The parameter: 'doB' has an invalid value: '$date'. Future date is not allowed";
		}
		} catch (DateTimeParseException e) {
		 return	"The parameter: 'doB' has an invalid value: '$date'. Allowed format is YYYY-MM-DD";
		}
		return true;
	}

	private boolean validateDate(int day, int month , int year) {
		switch (month) {
		case 1:
		case 3:
		case 5:
		case 7:
		case 8:
		case 10:
		case 12: return day < 32;
		case 4:
		case 6:
		case 9:
		case 11: return day < 31;
		case 2:
			int modulus100 = year % 100;
			if ((modulus100 == 0 && year % 400 == 0) || (modulus100 != 0 && year % 4 == 0)) {
				//its a leap year
				return day < 30;
			} else {
				return day < 29;
			}
		default:
			break;
		}
		return false;
	}

	def validateValue(Any attrValueObj, String attrName, List allowedValues) {
		
		// Check if attribute value is not null / empty
		def result = validateValue(attrValueObj, attrName);
		
		// If attribute value if valid
		if(result == true) {
			return checkAllowedValues(attrValueObj, attrName, allowedValues)
		}
		
		return result;
	}
	
	/**
	 *  Check if attribute value is one of the allowed values
	 * @param attrValueObj
	 * @param attrName
	 * @param allowedValues
	 * @return
	 */
	def checkAllowedValues(Any attrValueObj, String attrName, List allowedValues) {
		if (allowedValues.contains(attrValueObj.toString().toUpperCase()))
			return true
		else
			return "The parameter: '$attrName' has an invalid value: '${attrValueObj.toString()}'. Allowed values are $allowedValues";
	}

	def validateValue(Any jsonObj, String attrName) {
		if(jsonObj.valueType().equals(ValueType.INVALID)) {
			return "The parameter: '$attrName' is missing";
		}
		if(jsonObj.toString().trim().isEmpty()) {
			return "The parameter: '$attrName' has an empty value";
		}
		return true;
	}

	def validateMessageBankTimezone(Any jsonObj, String attrName, Any jsonMessageBankTypeObj) {
		if((!(jsonMessageBankTypeObj.valueType().equals(ValueType.INVALID))) && (!(jsonMessageBankTypeObj.toString().trim().isEmpty()))){
			if(jsonObj.valueType().equals(ValueType.INVALID)) {
				return "The parameter: '$attrName' is missing";
			}
			if(jsonObj.toString().trim().isEmpty()) {
				return "The parameter: '$attrName' has an empty value";
			}
		}
		return true;
	}
	
	public String getProductMappingDetails(GetMappingCacheInput getMappingCacheInput,
		String sourceId, String resourceId) {
		
		String mappingObj = (String)getMappingCacheInput.invoke(sourceId, resourceId);

		
		if(mappingObj != null) {
			ReadContext readContext = JsonPath.parse(mappingObj);
			String callbackResult = readContext.read("callback_result");
			if("OK".equals(callbackResult)) {
				return mappingObj;
			}
		}
	
		return null;
	}

}