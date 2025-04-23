package groovy

import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.ericsson.datamigration.log.utils.LogUtil
import java.text.MessageFormat

import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Date

/**
 *
 * @author arup
 *
 */
public class CreatePartyEntity {

    private static final Logger logger = LoggerFactory.getLogger(CreatePartyEntity.class)

    private AtRulesConstant atrules = new AtRulesConstant();

    JSONArray createPartyEntity(JSONObject party, JSONArray contactMedium, JSONArray contactMediumChar, JSONObject paymentMethod, JSONObject mapping) {
        JSONObject partyRecord = new JSONObject()
        JSONArray partyArr = new JSONArray()
        JSONArray languageArr = new JSONArray()
        
        JSONObject response = new JSONObject()
        response.put("errorGenerationPoint",atrules.ERROR_GENERATION_POINT)
        JSONArray messages = new JSONArray()
        //response.put("messages",messages)
        


        JSONObject commonMapping = mapping.get('common')
        JSONObject lookupMapping = mapping.get('lookup')

        JSONObject genderMapping = mapping.get('lookup').get('gender')
        JSONObject nationalityMapping = mapping.get('lookup').get('nationality')
        JSONObject languageMapping = mapping.get('lookup').get('language')
        JSONObject maritalStatusMapping = mapping.get('lookup').get('marital_status')
        JSONObject titleMapping = mapping.get('lookup').get('title')

        //partyRecord.put("externalId", atrules.getExternalId("party",externalId));
        partyRecord.put('externalId', party.optString('partyexternalid'))
        partyRecord.put('partitionId', commonMapping.get('partition_id'))
        partyRecord.put('birthDate', getFormattedDate(party.get('birthdate')))

        JSONObject TE_VALIDATION_PARTY = mapping.getJSONObject("te_error_validation").getJSONObject("Party").getJSONObject("error_id");

        String gender = getFromMappingWithValidation(genderMapping, party, 'gender')
        String nationalityValue =getFromMappingWithValidation(nationalityMapping, party, 'nationality')
        
        if(gender == null){
           messages = populateErrorResponse(messages,atrules.PARTY_GENDER_ERROR_CODE,TE_VALIDATION_PARTY,"stg_beam_party.gender",party.optString("gender"))
        }
        else{
          partyRecord.put('gender', gender)
        }
        if(nationalityValue == null){
            messages = populateErrorResponse(messages,atrules.PARTY_NATIONALITY_ERROR_CODE,TE_VALIDATION_PARTY,"stg_beam_party.nationality",party.optString("nationality"))
        }
        else{
          partyRecord.put('nationality', nationalityValue)
        }
        //partyRecord.put('nationality', getFromMapping(nationalityMapping, party, 'nationality'))
        String lang = getFromMappingWithValidation(languageMapping, party, 'language')
         if(lang == null){
         messages = populateErrorResponse(messages,atrules.PARTY_LANGUAGE_ERROR_CODE,TE_VALIDATION_PARTY,"stg_beam_party.language",party.optString("language"))
        }
        else{
         partyRecord.put('language', new JSONArray('[' + lang + ']'))
        }
        //partyRecord.put('language', new JSONArray('[' + lang + ']'))
        String title = getFromMappingWithValidation(titleMapping, party, 'title')
        if(title == null){
         messages = populateErrorResponse(messages,atrules.PARTY_TITLE_ERROR_CODE,TE_VALIDATION_PARTY,"stg_beam_party.title",party.optString("title"))
        }
        else{
         partyRecord.put('title', title)
        }
       // partyRecord.put('title', getFromMapping(titleMapping, party, 'title'))
        partyRecord.put('givenName', party.optString('givenname'))
        partyRecord.put('familyName', party.optString('familyname'))
        partyRecord.put('middleName', party.optString('middlename'))
        partyRecord.put('formattedName', party.optString('formattedname'))

        //get status getFormattedDate
        JSONObject validFor = getValidFor(getFormattedDate(party.get('statusstartdatetime')))

        JSONObject status = new JSONObject()
        status.put('status', 'PartyActive')
        status.put('validFor', validFor)

        //get status arr
        JSONArray statusArr = new JSONArray()
        statusArr.put(status)

        //put into party
        partyRecord.put('status', statusArr)

        //contact medium and contact medium characteristics
        partyRecord.put('contactMedium', getContactMediumArray(contactMedium, contactMediumChar, commonMapping, lookupMapping, party))

        if(paymentMethod != null) 
            partyRecord.put('paymentMethod', getPaymentMethodArray(paymentMethod, commonMapping))   

        partyRecord.put('maritalStatus', getMaritalStatusArray(party, maritalStatusMapping))
        if (party.optString('individualspecification') != null && !party.optString('individualspecification').isEmpty()) {
            JSONObject individualSpecification = new JSONObject()
            individualSpecification.put('externalId', (lookupMapping.get('partySpecExtId')).get('mapping').get(party.optString('individualspecification')))

            //individualSpecification.put("externalId","party_spec01");
            partyRecord.put('individualSpecification', individualSpecification)
        }

        partyRecord.put('action', 'create')
        response.put("messages",messages)
        if(messages.length()> 0){
         partyRecord.put("response",response)
         partyRecord.put("success",false)

        }
        
        partyArr.put(partyRecord)
        LogUtil.logDebug(logger, MessageFormat.format("Party Array = {0}", partyArr.toString()));
        return partyArr
    }

    private JSONArray populateErrorResponse(JSONArray messages,String errorCode,JSONObject TE_VALIDATION_PARTY,String type,String value){
          JSONObject erroCodeObj = TE_VALIDATION_PARTY.getJSONObject(errorCode)
          JSONObject errorDetails = new JSONObject()
          errorDetails.put("action",atrules.VALIDATION_ACTION)
          errorDetails.put("errorId",errorCode)
          String detailsMsg = atrules.DETAILS_MSG.replace("TABLE_INFO",type).replace("SOURCE_VALUE",value)
          errorDetails.put("details",detailsMsg)
          errorDetails.put("errorSeverity",erroCodeObj.get("ERROR_SEVERITY"))
          errorDetails.put("errorFieldType",type)
          errorDetails.put("errorFieldValue",value)
          messages.put(errorDetails);

        return messages

    }

    private String getFromMapping(JSONObject mapping, JSONObject sourceJson, String fieldName) {
        String object = null

        for (String objKey : mapping.get('mapping').keySet()) {
            object = mapping.get('mapping').get(objKey)
            if (objKey.equals(sourceJson.optString(fieldName))) {
                break
            }
        }

        return object
    }

    private String getFromMappingWithValidation(JSONObject mapping, JSONObject sourceJson, String fieldName) {
        String object = null

        for (String objKey : mapping.get('mapping').keySet()) {
            
            if (objKey.equals(sourceJson.optString(fieldName))) {
                object = mapping.get('mapping').get(objKey)
                break
            }
        }

        return object
    }

    private JSONObject getValidFor(Object val) {
        JSONObject startDateTime = new JSONObject()
        startDateTime.put('startDateTime', val)
        return startDateTime
    }

    private JSONArray getContactMediumArray(JSONArray contactMediumArr, JSONArray contactMediumChar, JSONObject commonMapping, JSONObject lookupMapping, JSONObject party) {
        JSONObject cMedium = null
        JSONObject characteristic = null
        JSONObject value = null
        JSONArray valueArr = null
        JSONArray characteristicArr = null
        JSONObject validFor = null
        JSONArray cMediumArr = new JSONArray()

        for(JSONObject contactMedium : contactMediumArr) {
            if(contactMedium.optString("partyexternalid").equals(party.optString("partyexternalid"))) {
                cMedium = new JSONObject();
            
                cMedium.put("externalId", contactMedium.get("externalid"));
                validFor = getValidFor(getFormattedDate(contactMedium.get('validfrom')));
                cMedium.put("validFor", validFor);

                cMedium.put("contactMediumSpecExternalId", getFromMapping(lookupMapping.get("contactMediumSpecExternalId"),contactMedium,"contactmediumspecexternalid"));

                characteristicArr = new JSONArray();
                for(JSONObject cmChar : contactMediumChar) {
                    if(cmChar.get("contactmediumexternalid").equals(contactMedium.get("externalid"))) {
                        characteristic = new JSONObject();
                        characteristic.put("charSpecExternalId", getFromMapping(lookupMapping.get("charSpecExternalId"), cmChar, "charspecexternalid"));
                        validFor = getValidFor(getFormattedDate(cmChar.get('validfrom')));
                        characteristic.put("validFor", validFor);
                        valueArr = new JSONArray();
                        value = new JSONObject();
                        value.put("value",cmChar.get("value"));
                        valueArr.put(value);
                        characteristic.put("value", valueArr);
                        characteristicArr.put(characteristic);
                    }
                }
                cMedium.put("characteristic",characteristicArr);
                cMediumArr.put(cMedium);
            }
        }
        return cMediumArr
    }

    private JSONArray getPaymentMethodArray(JSONObject pMethod, JSONObject commonMapping) {
        JSONObject paymentMethod = new JSONObject()
        paymentMethod.put('externalId', pMethod.get('partyexternalid'))

        JSONObject paymentMethodSpecification = new JSONObject()
        //paymentMethodSpecification.put("externalId",commonMapping.get("payment_method"));
        //paymentMethodSpecification.put("id","2FCCA6676BFE495B8E47DF9183D0F34B");
        paymentMethodSpecification.put('id', commonMapping.get('payment_method_id'))
        paymentMethod.put('paymentMethodSpecification', paymentMethodSpecification)

        JSONObject validFor = getValidFor(getFormattedDate(pMethod.get('startdatetime')))
        paymentMethod.put('validFor', validFor)
        JSONArray paymentMethodArr = new JSONArray()
        paymentMethodArr.put(paymentMethod)

        return paymentMethodArr
    }

    private JSONArray getMaritalStatusArray(JSONObject party, JSONObject maritalStatusMappingArr) {
        JSONObject maritalStatus = new JSONObject()

        //maritalStatus.put("status",party.optString("maritalstatus"));
        maritalStatus.put('status', getFromMapping(maritalStatusMappingArr, party, 'maritalstatus'))

        //JSONObject validFor = getValidFor(getFormattedDate(status.get("MaritalStatusStartDatetime")));
        JSONObject validFor = getValidFor(getFormattedDate(party.get('statusstartdatetime')))
        maritalStatus.put('validFor', validFor)
        JSONArray maritalStatusArr = new JSONArray()
        maritalStatusArr.put(maritalStatus)

        return maritalStatusArr
    }

    private String getFormattedDate(String dateStr) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        formatter.setTimeZone(TimeZone.getTimeZone('UTC'))
        Date dateObj = formatter.parse(dateStr)

        dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(dateObj)

        return dateStr
    }

}
