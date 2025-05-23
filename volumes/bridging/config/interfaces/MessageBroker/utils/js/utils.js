const ERROR_FAULT_CODE_KEY = "FaultCode"; //Needed for SOAPDbLookup workflow output
const ERROR_CODE_KEY = "code";
const ERROR_MESSAGE_KEY = "message";
const ERROR_LEGACY_HTTP_CODE_KEY = "HTTPCode";
const ERROR_FAILURE_FLAG_KEY = "fealureFlag";
const CACHE_LEGACY_ERROR_CODE = "LegacyErrorCode";
const CACHE_LEGACY_ERROR_MESSAGE = "LegacyErrorMessage";
const HTTP_MESSAGE_JSON_PATH = "http_message";
const HTTP_BODY_JSON_PATH = "http_body"

/**
 * It creates a response for callback calls
 * @param resultCallback It contains the output of a callback call
 * @returns
 */
function callback_response_management (resultCallback) {
       var jsonData = JSON.parse(resultCallback);
       logWriter.invoke("DEFAULT", "DEBUG", jsonData.callback_result + "--callback_result--");
       logWriter.invoke("DEFAULT", "DEBUG", JSON.stringify(jsonData.callback_value) + "--callback_value--");

       if ('OK' == jsonData.callback_result){
             return {'result' : jsonData.callback_value};
       } else {
             return {'error' : jsonData.callback_value};
       }
}


/**
 * It creates a response accordingly to the validation code from the dispatcher module
 * @param value Validation code received from dispatcher
 * @returns
 */
function assign_validation_code_value (value) {
	var result;
	logWriter.invoke("DEFAULT", "DEBUG", "Value for switch: " + value[0]);
	switch(value[0]) {
	case "400":
 		result = {'error' : {'error_code': 'Internal_Error'}};
 		break;
 	case "400-1":
 		result = {'error' : {'error_code': 'InvalidPayload'}};
 		break;
 	case "400-2":
 		result = {'error' : {'error_code': 'ParameterMissing'}};
 		break;
 	case "400-3":
 		result = {'error' : {'error_code': 'InvalidParameterValue'}};
 		break;
 	case "400-4":
 		result = {'error' : {'error_code': 'UnknownAccountIdentifier'}};
 		break;
 	case "401":
 		result = {'error' : {'error_code': 'Unauthorized'}};
 		break;
 	case "500":
 		result = {'error' : {'error_code': 'InternalServerError'}};
 		break;
 	case "500-2":
 		result = {'error' : {'error_code': 'SerializationError'}};
 		break;
 	case "500-3":
 		result = {'error' : {'error_code': 'DBLookupError'}};
 		break
 	case "504":
 		result = {'error' : {'error_code': 'No_Connection_To_Legacy'}};
 		break;
 	case "600":
 		result = {'error' : {'error_code': 'In_Migration'}};
 		break;
 	default:
 		result = {'result': '200'};
	} 
	logWriter.invoke("DEFAULT", "DEBUG", "Value returned:" + JSON.stringify(result));
	return result;
}

/**
 * It generates an error understandable to be used later in the workflow,
 * It can be called by 3 different ways:
 * 	-6 parameters->
 * 		-Input[0]->Json root key to be created (ErrorInfo)
 * 		-Input[1]->$.lastError that has been created by the WFM
 * 		-Input[2]->$lastError.http_code that contains the http error received as http response
 * 		-Input[3]->sourceId to access to cache
 * 		-Input[4]->defaultResourceId to use in case no resourceId can be calculated
 * 		-Input[5]->errorCodeJsonPath that contains the json path to get some information from the json received as response
 * 		-Input[6]->insert FaultCode tag in output (boolean)
 * 	-4 parameters:
 * 		-Input[0]->Json root key to be created (ErrorInfo)
 * 		-Input[1]->$.lastError.IncidenceCode that is the json path to the error generated by the WFM
 * 		-Input[2]->sourceId to access to cache
 * 		-Input[3]->defaultHttpCode to be used in case no http code is received from response
 * 		-Input[4]->defaultResourceId to use in case no resourceId can be calculated
 * 		-Input[5]->insert FaultCode tag in output (boolean)
 * 	-3 parameters:
 * 		-Input[0]->Json root key to be created (ErrorInfo)
 * 		-Input[1]->sourceId to access to cache
 * 		-Input[2]->defaultResourceId to access to cache
 * 		-Input[3]->insert FaultCode tag in output (boolean)
 */
function get_error_info (input) {
	var name = input[0];
	var defaultResourceId;
	var defaultHttpCode;
	var sourceId;
	var sourceResourceArray;
	var aux;
	var insertFaultCode = false;
	
	if (null == input || ((input.length != 7 || null == input[4]) && input.length != 6 && input.length != 4)) {
		var message = (null == input ? null : input.length.toString());
		logWriter.invoke("DEFAULT", "DEBUG", "Wrong input value. Expected list with 4, 6 or 7 values; Found: " + message);
		return {'error' : {'error_code': 'Wrong input value. Expected list with 4, 6 or 7 values; Found: ' + input.length.toString()}};
	}
	
	// Preconditions check
	if (input.length == 4 && null == input[1] && null == input[2]) {
		logWriter.invoke("DEFAULT", "DEBUG", "Wrong input value. Expected list with 4 values; Found: " + input.length.toString());
		return {'error' : {'error_code': 'Wrong input value. Expected list with 1 value; Found: ' + input.length.toString()}};
	}
	
	sourceResourceArray = new Array(4);
	if(!set_source_and_resource(input, sourceResourceArray)) {
		logWriter.invoke("DEFAULT", "DEBUG", "sourceId/resourceId attribute is null or empty for GetRuleErrorCode.");
		return {'error' : {'error_code': 'sourceId/resourceId attribute is null or empty for GetRuleErrorCode.'}};
	}

	sourceId = sourceResourceArray[0];
	defaultResourceId = sourceResourceArray[1];
	if (sourceResourceArray[2]) {
		defaultHttpCode = sourceResourceArray[2];
	}
	
	insertFaultCode = sourceResourceArray[3];
	aux = generate_error_message(input, defaultResourceId, sourceId, defaultHttpCode, insertFaultCode);
	var finalValue = {'result': aux};
	logWriter.invoke("DEFAULT", "DEBUG", "get_error_info finalValue is " + JSON.stringify(finalValue));
	return finalValue;
}

/**
 * It returns a json error based on the inputs
 * @param input it contains the parameters received from the rules skeleton
 * @param defaultResourceId it contains the defaultResourceId in case no resourceId can be calculated to take an error from cache
 * @param sourceId it contains the sourceId information to get an error from cache
 * @param defaultHttpCode it contains the default http code to be returned in case no http code can be obtained from cache
 * @param insertFaultCode it contains true or false depending it is necessary to insert FaultCode element in output
 * @returns
 */
function generate_error_message(input, defaultResourceId, sourceId, defaultHttpCode, insertFaultCode) {
	const HTTP_CODE_INPUT_INDEX = 2;
	const DBSS_ERROR_MESSAGE_INPUT_INDEX = 1;
	
	var dbssErrorBody = null;
	var dbssErrorCode;
	var httpCode = null;
	var errorInfo;
	
	if (input.length > 4){
		dbssErrorBody = get_error_json_string(input[DBSS_ERROR_MESSAGE_INPUT_INDEX], HTTP_BODY_JSON_PATH);
		logWriter.invoke("DEFAULT", "DEBUG", "generate_error_message dbssErrorBody inside length higher than 3 " + dbssErrorBody);
	}
	
	if (input.length == 7 && null != input[5]) {
		//If jsonpath is defined, we must try to apply it to get the dbss error code.
		//This must always happen when the error comes from a HTTP issue
		dbssErrorCode = input[5].toString();
		logWriter.invoke("DEFAULT", "DEBUG", "generate_error_message errorCodeJsonPath inside length equals than 7 and input[5] not null " + dbssErrorCode);
		
		if(null == dbssErrorCode || dbssErrorCode.length == 0){
			dbssErrorCode = get_error_json_string(input[DBSS_ERROR_MESSAGE_INPUT_INDEX], HTTP_MESSAGE_JSON_PATH);
			logWriter.invoke("DEFAULT", "DEBUG", "generate_error_message dbssErrorCode inside length equals than 7 and input[5] not null when dbssErrorCode is null" + dbssErrorCode);
		}
		
	} else {
		dbssErrorCode = dbssErrorBody;
		logWriter.invoke("DEFAULT", "DEBUG", "generate_error_message dbssErrorCode inside length different than 7 " + dbssErrorCode);
	}
	
	var httpCode = null;
	
	if(input.length == 7) {
		httpCode = input[HTTP_CODE_INPUT_INDEX];
	}
	
	var errorInfo;
	
	if(null == dbssErrorCode || dbssErrorCode.length == 0){
		logWriter.invoke("DEFAULT", "DEBUG", "generate_error_message dbssErrorCode is null " + dbssErrorCode);
		errorInfo = get_default_legacy_error(defaultResourceId, sourceId, insertFaultCode);
	}else{
		logWriter.invoke("DEFAULT", "DEBUG", "generate_error_message dbssErrorCode is not null " + dbssErrorCode);
		var responseErrorMessage = input[1];
		//If responseErrorMessage is a json string, then we take the value contained in the json path dbssErrorCode
		
		if (responseErrorMessage.trim().indexOf("{") != -1) {
			dbssErrorCode = get_json_field(responseErrorMessage, dbssErrorCode);
		}
		
		if (!dbssErrorCode) {
			dbssErrorCode = get_json_field(responseErrorMessage, HTTP_MESSAGE_JSON_PATH);
		}
		//Getting response based on the DBSS error code as resource
		errorInfo = get_legacy_error(sourceId, dbssErrorCode.replace(/\s/g, ""), dbssErrorBody, httpCode, defaultHttpCode, defaultResourceId, insertFaultCode);
	}
	logWriter.invoke("DEFAULT", "DEBUG", "ERROR_FAILURE_FLAG_KEY is " + ERROR_FAILURE_FLAG_KEY);
	if (errorInfo[ERROR_LEGACY_HTTP_CODE_KEY] != null && errorInfo[ERROR_LEGACY_HTTP_CODE_KEY].toString().startsWith("5")){
		errorInfo[ERROR_FAILURE_FLAG_KEY] = true;
	} else {
		errorInfo[ERROR_FAILURE_FLAG_KEY] = false;
	}
	logWriter.invoke("DEFAULT", "DEBUG", "generata_error_message errorInfo: " + JSON.stringify(errorInfo));
	return errorInfo;
}

function get_json_field(jsonObject, jsonPath) {
	var strSplit = jsonPath.split(".");
	var i;
	var output = null;
	logWriter.invoke("DEFAULT", "DEBUG", "jsonObject in get_json_field is " + jsonObject + " and the type is " + typeof jsonObject);
	var aux = JSON.parse(jsonObject);
	logWriter.invoke("DEFAULT", "DEBUG", "AUX IS " + JSON.stringify(aux));
	for (i = 0; i < strSplit.length; i++) {
		logWriter.invoke("DEFAULT", "DEBUG", "strSplit[" + i + "] is " + strSplit[i]);
		logWriter.invoke("DEFAULT", "DEBUG", "The object retrieved is " + aux[strSplit[i]]);
		if(aux[strSplit[i]]) {
			if (typeof aux[strSplit[i]] === 'object' || aux[strSplit[i]] instanceof Object) {
				if (i == strSplit.length -1) {
					output = aux[strSplit[i]];
				} else {
					aux = aux[strSplit[i]];
					logWriter.invoke("DEFAULT", "DEBUG", "The output for i " + i + " is " + JSON.stringify(aux));
				}
			} else {
				if (i == strSplit.length -1) {
					output = aux[strSplit[i]];
					logWriter.invoke("DEFAULT", "DEBUG", "The output for i " + i + " is " + output);
				} else {
					aux = JSON.parse(aux[strSplit[i]]);
					logWriter.invoke("DEFAULT", "DEBUG", "The output for i not object " + i + " is " + JSON.stringify(aux));
				}
			}
		} else {
			break;
		}
	}
	
	if(output) {
		logWriter.invoke("DEFAULT", "DEBUG", "The type of output is " + typeof output);
		if (typeof output === 'object' || output instanceof Object) {
			logWriter.invoke("DEFAULT", "DEBUG", "The final output for jsonPath " + jsonPath + " is " + JSON.stringify(output));
		} else if (typeof output === 'string'){
			logWriter.invoke("DEFAULT", "DEBUG", "The final output for jsonPath " + jsonPath + " is " + output);
		}
	}
	
	return output;
}

/**
 * It creates a json default legacy error based on the parameters
 * @param defaultResourceId it contains the resourceId to be used when taking an error from cache
 * @param sourceId it contains the sourceId to be used when taking an error from cache
 * @param insertFaultCode it contains true or false depending it is necessary to insert FaultCode element in output
 * @returns
 */
function get_default_legacy_error(defaultResourceId, sourceId, insertFaultCode) {
	var cacheInput = [sourceId, defaultResourceId];
	var mapping = get_mapping_cache_input(cacheInput);
	logWriter.invoke("DEFAULT", "DEBUG", "get_default_legacy_error mapping value: " + JSON.stringify(mapping));
	var resultValue = mapping.result;
	logWriter.invoke("DEFAULT", "DEBUG", "get_default_legacy_error resultValue value: " + JSON.stringify(resultValue));
	var errorInfo;
	if (null == resultValue || Object.keys(resultValue).length == 0) {
		logWriter.invoke("DEFAULT", "DEBUG", "Default error not mapped in cache");
		errorInfo = get_unknown_internal_error();
		logWriter.invoke("DEFAULT", "DEBUG", "the value of insertFaultCode in get_default_legacy_error  is " + insertFaultCode);
		if (true === insertFaultCode) {
			errorInfo[ERROR_FAULT_CODE_KEY] = defaultResourceId;
		}
		return errorInfo;
	}
	
	var mappedError = resultValue[0];
	logWriter.invoke("DEFAULT", "DEBUG", "get_default_legacy_error mappedError is " + JSON.stringify(mappedError));
	
	errorInfo = generate_error_json(mappedError[CACHE_LEGACY_ERROR_CODE], mappedError[CACHE_LEGACY_ERROR_MESSAGE], mappedError[ERROR_LEGACY_HTTP_CODE_KEY]);
	logWriter.invoke("DEFAULT", "DEBUG", "the value of insertFaultCode in get_default_legacy_error is " + insertFaultCode);
	if (true === insertFaultCode) {
		errorInfo[ERROR_FAULT_CODE_KEY] = defaultResourceId;
	}
	logWriter.invoke("DEFAULT", "DEBUG", "get_default_legacy_error errorInfo 4 is: " + JSON.stringify(errorInfo));
	return errorInfo;
}

/**
 * It generates an unknown internal error
 */
function get_unknown_internal_error() {
	return generate_error_json("500", "", "500");
}

/**
 * It generates the error json for the code, message and httpCode parameters received
 * @param code information to be included as code error response
 * @param message information to be included as message error response
 * @param httpCode information to be included as http code error response
 * @returns
 */
function generate_error_json(code, message, httpCode) {
	var errorJson = {};
	errorJson[ERROR_CODE_KEY] = code;
	errorJson[ERROR_MESSAGE_KEY] = message;
	errorJson[ERROR_LEGACY_HTTP_CODE_KEY] = httpCode;
	
	return errorJson;
}

/**
 * It returns the legacy error based on the input parameters
 * 
 * @param sourceId sourceId to be used to take error from cache
 * @param resourceId resourceId to be used to take error from cache
 * @param dbssErrorMessage error message returned in json object in case no error message is in cache
 * @param dbssConnectionHttpCode dbss connection http code received in response
 * @param defaultHttpCode default http code to be used
 * @param defaultResourceId defaultResourceId to be used in case no error is for the sourceId and resourceId parameters
 * @param insertFaultCode it contains true or false depending it is necessary to insert FaultCode element in output
 * 
 * @returns
 */
function get_legacy_error(sourceId, resourceId, dbssErrorMessage, dbssConnectionHttpCode, defaultHttpCode, defaultResourceId, insertFaultCode) {
	var cacheInput = [sourceId, resourceId];
	var mapping = get_mapping_cache_input(cacheInput);
	logWriter.invoke("DEFAULT", "DEBUG", "get_legacy_error mapping value: " + JSON.stringify(mapping));
	var resultValue = mapping.result;
	logWriter.invoke("DEFAULT", "DEBUG", "get_legacy_error resultValue value: " + JSON.stringify(resultValue));
	var jsonObject;
	var errorInfo;
	if (null == resultValue) {
		logWriter.invoke("DEFAULT", "DEBUG", "get_legacy_error resultValue is null");
		return get_default_legacy_error(defaultResourceId, sourceId, insertFaultCode);
	}
	
	jsonObject = resultValue[0];
	logWriter.invoke("DEFAULT", "DEBUG", "get_legacy_error jsonObject value: " + JSON.stringify(jsonObject));
	if (null != jsonObject) {
		var errorMessage = jsonObject[CACHE_LEGACY_ERROR_MESSAGE];
		var httpCode = jsonObject[ERROR_LEGACY_HTTP_CODE_KEY];
		logWriter.invoke("DEFAULT", "DEBUG", "get_legacy_error errorMessage value: " + errorMessage);
		logWriter.invoke("DEFAULT", "DEBUG", "get_legacy_error httpCode value: " + httpCode);
		
		// In case there is no httpCode retrieved from the cache mapped error, 
		// the http code returned from dbss is assigned
		if(null == httpCode){
			httpCode = dbssConnectionHttpCode;
			logWriter.invoke("DEFAULT", "DEBUG", "get_legacy_error httpCode was null, new value for dbssConnectionHttpCode: " + httpCode);
		}
		
		// If there is no dbss http code, the default http code specified
		// is assigned from the yaml definition
		if(null == httpCode){
			httpCode = defaultHttpCode;
			logWriter.invoke("DEFAULT", "DEBUG", "get_legacy_error httpCode was null, new value for defaultHttpCode: " + httpCode);
		}
		logWriter.invoke("DEFAULT", "DEBUG", "get_legacy_error httpCode type is : " + typeof httpCode);
		//Check if httpCode is null or empty
		if(!(typeof httpCode === 'string' || httpCode.length == 0)) {
			logWriter.invoke("DEFAULT", "DEBUG", "get_legacy_error httpCode netiher string nor length higher than 0");
			return get_default_legacy_error(defaultResourceId, sourceId, insertFaultCode);
		}
		
		//If cache error message is empty or null set the DBSS error message received
		if (!errorMessage) {//if ((typeof errorMessage === 'string' || errorMessage instanceof String) && (!errorMessage || 0 === errorMessage.length)) {
			errorInfo = generate_error_json(jsonObject[CACHE_LEGACY_ERROR_CODE], dbssErrorMessage, httpCode);
		} else {
			errorInfo = generate_error_json(jsonObject[CACHE_LEGACY_ERROR_CODE], errorMessage, httpCode);
		}
		logWriter.invoke("DEFAULT", "DEBUG", "the value of insertFaultCode in get_legacy_error is " + insertFaultCode);
		if (true === insertFaultCode) {
			logWriter.invoke("DEFAULT", "DEBUG", "It enters in the if");
			errorInfo[ERROR_FAULT_CODE_KEY] = resourceId;
			logWriter.invoke("DEFAULT", "DEBUG", "The result of the if is " + JSON.stringify(errorInfo));
		}
		
		return errorInfo;
	}
	logWriter.invoke("DEFAULT", "DEBUG", "get_legacy_error mapping obtained " + JSON.stringify(errorInfo));
	
	return errorInfo;
}

/**
 * It returns the error code received in the dbss response based on the
 * errorCodeJsonPath parameter
 * @param dbssErrorBody it contains a json with the error information
 * @param errorCodeJsonPath json element to take the information from
 * @returns
 */
function get_error_code(dbssErrorBody, errorCodeJsonPath) {
	var json = JSON.parse(dbssErrorBody);
	logWriter.invoke("DEFAULT", "DEBUG", "get_error_code dbssErrorBody: " + dbssErrorBody);
	logWriter.invoke("DEFAULT", "DEBUG", "get_error_code errorCodeJsonPath: " + errorCodeJsonPath);
	return json[errorCodeJsonPath];
}

/**
 * It returns the error received from DBSS based on the jsonElement parameter in case input
 * parameter is a json
 * @param input contains the information related to the error to be returned
 * @param jsonElement it contains the json element to be returned the value from
 * @returns
 */
function get_error_json_string(input, jsonElement) {
	if (null == input) {// || !(input instanceof JSONObj || input instanceof String)) {
		var typeErrorMessage = input == null ? "null": typeof input;
		logWriter.invoke("DEFAULT", "DEBUG", "Wrong DBSS Http body input found. Expected String or JSONObject; Found: " + typeErrorMessage);
	}
	
	if((typeof input === 'string' || input instanceof String) && input.trim().indexOf("{") == -1){
		return input;
	}

	return (JSON.parse(input))[jsonElement];
}

/**
 * It returns an array with sourceId and resource based on the input length.
 * It will also return a defaultResourceId in case the input length is 5.
 * In case any of the conditions is fulfilled false will be returned.
 * In case one of the conditions is fulfilled true will be returned.
 * @param input input parameters received from rules skeleton
 * @param sourceResourceArray array to be fulfilled
 * @returns {Boolean}
 */
function set_source_and_resource(input, sourceResourceArray) {
	if (input.length == 7 && null != input[3] && null != input[4]) {
		sourceResourceArray[0] = input[3].toString();
		sourceResourceArray[1] = input[4].toString();
		sourceResourceArray[2] = input[2].toString();
		sourceResourceArray[3] = (input[6] === 'true');
		logWriter.invoke("DEFAULT", "DEBUG", "The position 3 is " + sourceResourceArray[3]);
		return true;
	} else if (input.length == 6 && null != input[2] && null != input[4]) {
		logWriter.invoke("DEFAULT", "DEBUG", "set_source_and_resource length 5 sourceId is " + input[2]);
		logWriter.invoke("DEFAULT", "DEBUG", "set_source_and_resource length 5 resourceId is " + input[4]);
		logWriter.invoke("DEFAULT", "DEBUG", "set_source_and_resource length 5 defaultHttpCpde is " + input[3]);
		sourceResourceArray[0] = input[2].toString();
		sourceResourceArray[1] = input[4].toString();
		sourceResourceArray[2] = input[3].toString();
		sourceResourceArray[3] = (input[5] === 'true');
		logWriter.invoke("DEFAULT", "DEBUG", "The position 3 is " + sourceResourceArray[3]);
		return true;
	} else if (input.length == 4 && null != input[1] && null != input[2]) {
		sourceResourceArray[0] = input[1].toString();
		sourceResourceArray[1] = input[2].toString();
		sourceResourceArray[3] = (input[3] === 'true');
		logWriter.invoke("DEFAULT", "DEBUG", "The position 3 is " + sourceResourceArray[3]);
		return true;
	}
	
	return false;
}

/**
 * It will return the information from cache related to that sourceId and resourceId by calling
 * a java callback
 * @param input it contains the parameters received from rules skeleton file
 * @returns
 */
function get_mapping_cache_input(input) {
	logWriter.invoke("DEFAULT", "DEBUG", "The source in get_mapping_cache_input is " + input[0]);
	logWriter.invoke("DEFAULT", "DEBUG", "The resource in get_mapping_cache_input is " + input[1]);
	var value = getMappingCacheInput.invoke(input[0], input[1]);
	var valueOutput = callback_response_management(value);
	logWriter.invoke("DEFAULT", "DEBUG", "get_mapping_cache_input output: " + JSON.stringify(valueOutput));
	return valueOutput;
	
}

//module.exports = {
//		  'callback_response_management': callback_response_management,
//		  'assign_validation_code_value': assign_validation_code_value,
//		  'get_error_info': get_error_info,
//		  'get_mapping_cache_input': get_mapping_cache_input
//		}

const sequence = {
  counter: {},      // Storage for sequence counters
  ids: {},          // Storage for generated tmpIDs
  digits: '000000000',   // Counter format
  init_count: 0,    // initial counter value
  next: function(str, id) {
    // Step conter or set to initial value if there is no counter
	  var count;
    if (this.counter.hasOwnProperty(str)) {
      count = this.counter[str] + 1;
    } else {
      count = this.init_count;
    }
    this.counter[str] = count
    // return string_00ABC (counter padded with zero's)
    const tmpID = str + '_' + (this.digits + count).slice(-this.digits.length);
    if (id !== '') {
      this.ids[id] = tmpID;
    }
    return tmpID
  },

  current: function (str) {
    // Get last counter
	  var tmpID;
    if (this.counter.hasOwnProperty(str)) {
      // return string_00ABC (counter padded with zero's)
      tmpID = str + '_' + (this.digits + this.counter[str]).slice(-this.digits.length)
    } else {
      tmpID = "undefined"
    }
    return tmpID
  },
  
  get: function(str) {
    // get save counter
    return this.ids[str]
  }
};

Array.prototype.extend = function (other_array) {
	  /* you should include a test to check whether other_array really is an array */
	  other_array.forEach(function(v) {this.push(v)}, this);    
	}

// set format of the data string function 
function format(x) {
	let dformat
	if (x != undefined) {
		//console.log("Date format function")
		const format1 = x.split('/')
		const format2 = x.split('-')
		//console.log("format type:: "+ format1[0]+": "+format1.length + " ,"+format2[1]+": "+format2.length)
		if (format1.length > 1) {
			if(format1[0].length > 3) {
			    dformat = '%Y/%m/%d %H:%M:%S'
			}
			else {
				dformat = '%d/%m/%Y %H:%M:%S'
			}
		}
		if (format2.length > 1) {
			if(format2[0].length > 3) {
			    dformat = '%Y-%m-%d %H:%M:%S'
			}
			else {
				dformat = '%d-%m-%Y %H:%M:%S'
			}
		}
	}
	//console.log("dformat value:: " + dformat)
	 return dformat
  }
// Output Date as an RM ISO string
Date.prototype.toIsoString = function() {
    var tzo = -this.getTimezoneOffset(),
        dif = tzo >= 0 ? '+' : '-',
        pad1 = function(num) {
            var norm = Math.abs(Math.floor(num));
            return (norm < 10 ? '0' : '') + norm;
        };
    return this.getFullYear() +
        '-' + pad1(this.getMonth() + 1) +
        '-' + pad1(this.getDate()) +
        'T' + pad1(this.getHours()) +
        ':' + pad1(this.getMinutes()) +
        ':' + pad1(this.getSeconds()) +
        '.' + pad1(this.getMilliseconds()) +
        dif + pad1(tzo / 60) +
        ':' + pad1(tzo % 60);
}

function pad(x, p) {
  return (x<10?p:'') + x
}

var locales = {
  'en': {
    'A': ['Sunday', 'Monday', 'Tuesday', 'Wednesday',
          'Thursday', 'Friday', 'Saturday'],
    'a': ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
    'B': ['January', 'February', 'March', 'April', 'May', 'June', 'July',
          'August', 'September', 'October', 'November', 'December'],
    'b': ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 
          'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  },
};

var strf = {
  'a': function (d, locale) { return locales[locale].a[d.getDay()] },
  'A': function (d, locale) { return locales[locale].A[d.getDay()] },
  'b': function (d, locale) { return locales[locale].b[d.getMonth()] },
  'B': function (d, locale) { return locales[locale].B[d.getMonth()] },
  'c': function (d) { return d.toString() },
  'C': function (d) { return Math.floor(d.getFullYear()/100) },
  'd': function (d) { return pad(d.getDate(),'0') },
  'D': function (d) { return strf.m(d) + '/' + strf.d(d) + '/' + strf.y(d) },
  'e': function (d) { return pad(d.getDate(),' ') },
  'F': function (d) { return strf.Y(d) + '-' + strf.m(d) + '-' + strf.d(d) },
  // %g like %G, but without the century
  // %G The 4-digit year corresponding to the ISO week number
  'h': function (d) { return strf.b(d) },
  'H': function (d) { return pad(d.getHours(),'0') },
  'I': function (d) { return pad((d.getHours() % 12 || 12), '0') },
  //  %j  day of the year as a decimal number (range 001 to 366)
  /*
  'j': function (d) {
      var t = d.getDate();
      var m = d.getMonth() - 1;
      if (m > 1) {
        var y = d.getYear();
        if (((y % 100) == 0) && ((y % 400) == 0)) ++t;
        else if ((y % 4) == 0) ++t;
      }
      while (m > -1) t += d.dpm[m--];
      return pad(3,'0');
    },
  */
  'k': function (d) { return pad(d.getHours(),' ') },
  'l': function (d) { return pad((d.getHours() % 12 || 12),' ') },
  'M': function (d) { return pad(d.getMinutes(),'0') },
  'm': function (d) { return pad((d.getMonth()+1),'0') },
  'n': function (d) { return "\n"; },
  'p': function (d) { return (d.getHours() > 11) ? 'PM' : 'AM' },
  'P': function (d) { return strf.p(d).toLowerCase() },
  'r': function (d) { return strf.I(d) + ':' + strf.M(d) + ':' +
                          strf.S(d) + ' ' + strf.p(d) },
  'R': function (d) { return strf.H(d) + ':' + strf.M(d) },
  's': function (d) { return d.getMilliseconds() },
  'S': function (d) { return pad(d.getSeconds(),'0') },
  't': function (d) { return "\t" },
  'T': function (d) { return strf.H(d) + ':' + strf.M(d) + ':' + strf.S(d) },
  //%U  number of the current year as a decimal number, starting with the first Sunday as the first day of the first week
  //U: function (d) { return ??? }, 
  'u': function (d) { return(d.getDay() || 7) },
  // %V  The ISO 8601:1988 week number of the current year as a decimal number, range 01 to 53, where week 1 is the first week that has at least 4 days in the current year, and with Monday as the first day of the week.
  //V: function (d) { return ??? }, 
  'v': function (d) { return strf.e(d) + '-' + strf.b(d) + '-' + strf.Y(d) },
  // %W  week number of the current year as a decimal number, starting with the first Monday as the first day of the first week
  //W: function (d) { return false; },
  'w': function (d) { return d.getDay() },
  'x': function (d) { return d.toDateString() }, // wrong?
  'X': function (d) { return d.toTimeString() }, // wrong?
  'Y': function (d) { return d.getFullYear() },
  'y': function (d) { return pad((d.getYear() % 100), '0') },
  // %Z  time zone name or abbreviation
  //Z: function (d) { return ???; },
  'z': function (d) { var z = d.getTimezoneOffset(); 
                    return (z<=0?('+' + pad(Math.floor(-z/60), '0') + 
                                  ':' + pad(-z%60, '0')):
                                 ('-' + pad(Math.floor(a/60), '0')) +
                                  ':' + pad(z%60, '0')) }, 
  //'+': function (d) { return strf.c(d) },
  '%': function (d) { return '%' }
  };

var defaultLocale = 'en';

function strftime(date, fmt, locale) {
    var r = '';
    var n = 0;
    if (!locale) { locale = defaultLocale; }
    while(n < fmt.length) {
        var c = fmt.substring(n, n+1);
        if (c === '%') {
            c = fmt.substring(++n, n+1);
            r += (strf[c]) ? strf[c](date, locale) : c;
        } else r += c;
        ++n;
    }
    return r;
}

Date.prototype.strftime = function (fmt, locale) {
  return strftime(this, fmt, locale);
};

Date.prototype.strftime.setDefaultLocale = function (locale) {
  defaultLocale = locale;
};
Date.prototype.strftime.locales = locales;

// Parse date strings using POSIX date format
const strp = {
  'a' : [ '[a-z]+', function(matched) { this.setUTDay(locales['en'].a.indexOf(matched)+1) } ],
  'A' : [ '[a-z]+', function(matched) { this.setUTDay(locales['en'].A.indexOf(matched)+1) } ],
  'b' : [ '[a-z]+', function(matched) { this.setUTCMonth(locales['en'].b.indexOf(matched)) } ],
  'B' : [ '[a-z]+', function(matched) { this.setUTCMonth(locales['en'].B.indexOf(matched)) } ],
  'd' : [ '[0-9]{0,2}', function(matched) { this.setUTCDate(+matched) } ],
  'H' : [ '[0-9]{0,2}', function(matched) { this.setUTCHours(+matched) } ],
  'I' : [ '[0-9]{0,2}', function(matched) { this.setUTCHours(+matched) } ],
  'm' : [ '[0-9]{0,2}', function(matched) { this.setUTCMonth(+matched - 1) } ],
  'M' : [ '[0-9]{0,2}', function(matched) { this.setUTCMinutes(+matched) } ],
  'p' : [ 'AM|PM', function(matched) { this.AMPM = matched } ],
  's' : [ '[0-9]+', function(matched) { this.setUTCMilliseconds(+matched) } ],
  'S' : [ '[0-9]{0,2}', function(matched) { this.setUTCSeconds(+matched) } ],
  'Y' : [ '[0-9]{4}', function(matched) { this.setUTCFullYear(+matched) } ],
  'z' : [ '[+-][0-9]{4}', function(matched) {
    this.timezone = (+matched.slice(0, 3) * (60 * 60)) + (+matched.slice(3, 5) * 60);
  } ],
  'Z' : [ 'UTC|Z|[+-][0-9][0-9]:?[0-9][0-9]', function (matched) {
    if (matched === 'Z') return;
    if (matched === 'UTC') return;
    // '+09:00' or '+0900'
    matched = matched.replace(/:/, '');
    this.timezone = (+matched.slice(0, 3) * (60 * 60)) + (+matched.slice(3, 5) * 60);
  } ],
  '%' : [ '%', function () {} ]
};

function strptime (date, str, format1, locale) {
 // if (!locale) { locale = defaultLocale; }
  if (!format1) throw Error("Missing format");
  var ff = [];
  var re = new RegExp(format1.replace(/%(?:([a-zA-Z%])|('[^']+')|("[^"]+"))/g, function (_, a, b, c) {
    var fd = a || b || c;
    var d  = strp[fd];
    if (!d) throw Error("Unknown format descripter: " + fd);
    ff.push(d[1]);
    return '(' + d[0] + ')';
  }), 'i');
  var matched = str.match(re);
  if (!matched) throw Error('Failed to parse');

  for (var i = 0, len = ff.length; i < len; i++) {
    var fun = ff[i];
    if (!fun) continue;
    fun.call(date, matched[i + 1]);
  }
  if (date.timezone) {
    date.setTime(date.getTime() - date.timezone * 1000); 
  }
  
  if (date.AMPM) {
    if (date.getUTCHours() === 12) date.setUTCHours(date.getUTCHours() - 12);
    if (date.AMPM === 'PM') date.setUTCHours(date.getUTCHours() + 12);
  }
  
  return date;
}

Date.prototype.strptime = function (str, fmt, locale) {
  return strptime(this, str, fmt, locale);
};

Date.prototype.fromEpochDays = function(days) {
  this.setTime(days * 86400000)
  return this
}

Date.prototype.fromEpochSeconds = function(days, seconds) {
  this.setTime((days<<65536 + seconds>0?seconds:-seconds)*1000)
  return this
}

//module.exports = {
//  'sequence': sequence
//}

function test_it() {
  console.log('Sequence testing'+ 
              '\n================');
  console.log('Default init_count');
  console.log('  Next("party")          ' + sequence.next('party'));          // party_0000
  console.log('Change init_count = 2');
  sequence.init_count = 2;
  console.log('  Next("contract"):      ' + sequence.next('contract'));       // contract_0002
  console.log('  Current("contract"):   ' + sequence.current('contract'));    // contract_0002
  console.log('Change digit format to "00"')
  sequence.digits = '00';
  console.log('  Current("party"):      ' + sequence.current('party'));       // party_00
  console.log('  Current("contract"):   ' + sequence.current('contract'));    // contract_02
  console.log('Change digit format back to default "0000"')
  sequence.digits = '0000';
  console.log('Save generated sequence');
  console.log('  Next("party", "test"): ' + sequence.next('party', 'test'));  // party_0001
  console.log('  Next("party"):         ' + sequence.next('party'));          // party_0002
  console.log('  Get("test"):           ' + sequence.get('test'));            // party_0001
  console.log('Error cases');
  console.log('  Current("test"):       ' + sequence.current('test'));        // undefined
  console.log('  Get("test1"):          ' + sequence.get('test1'));           // undefined

  console.log('\nDate formatting testing' + 
              '\n=======================');
  var d = new Date();
  console.log('Current date: ' + d);
  console.log('  d.toIsoString() = ' + d.toIsoString())


  console.log("  d.strftime('%Y-%m-%dT%H:%M:%S.000%z') = " + 
              d.strftime('%Y-%m-%dT%H:%M:%S.%s%z'));

  console.log("  d.strptime('Aug 3 2016', '%b %d %Y') = " +
              d.strptime('Aug 3 2016', '%b %d %Y'))
  console.log("  d.strptime('2017-08-05 12:31:00', '%Y-%m-%d %H:%M:%S') = " + 
              d.strptime('2017-08-05 12:31:00', '%Y-%m-%d %H:%M:%S'));
              
  
  console.log('\nTiming based on 10000 iterations:');
  console.time('  toIsoString()');
  for (let i=0;i<10000;i++) 
    d.toIsoString();
  console.timeEnd('  toIsoString()');
  
  console.time("  strftime('%Y-%m-%dT%H:%M:%S.000%z')");
  for (let i=0;i<10000;i++) 
    d.strftime('%Y-%m-%dT%H:%M:%S.%s%z');
  console.timeEnd("  strftime('%Y-%m-%dT%H:%M:%S.000%z')");
  
  console.time("  strptime('2017-08-05 12:31:00', '%Y-%m-%d %H:%M:%S')");
  for (let i=0;i<10000;i++) 
    d.strptime('2017-08-05 12:31:00', '%Y-%m-%d %H:%M:%S');
  console.timeEnd("  strptime('2017-08-05 12:31:00', '%Y-%m-%d %H:%M:%S')");

  console.log('d.fromEpochDays("17409"): '+d.fromEpochDays('17409'));
  console.log('d.fromEpochSeconds("17409", "-100"): ' + d.fromEpochSeconds("17409", "-100"));
}

//if (require.main === module) 
//  test_it() // run testcases 
