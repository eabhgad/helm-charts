package groovy;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ericsson.datamigration.log.utils.LogUtil;
import java.text.MessageFormat;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;
import java.util.*;


/**
 * 
 * @author emubhka
 *
 */
public class CreateOrganizationPartyEntity {

	private static final Logger logger = LoggerFactory.getLogger(CreateOrganizationPartyEntity.class);

	JSONArray createOrganizationPartyEntity( JSONArray party,JSONArray contactMedium, JSONArray contactMediumChar,JSONObject mapping) {

		JSONArray partyArr = new JSONArray();

		//ArrayList<JsonObject> listJsonObj = new ArrayList<>();
		TreeMap<String, JSONObject> sorted = new TreeMap<>();

		for (int i = 0; i < party.length(); i++) {

			JSONObject partyObj = party.getJSONObject(i);
			//if(partyObj.optString("partyexternalid").equals("party_ext_0000300")){

			JSONObject partyRecord = new JSONObject();
			JSONArray languageArr = new JSONArray();

			JSONObject commonMapping = mapping.get("common");
			JSONObject lookupMapping = mapping.get("lookup");

			JSONObject genderMapping = mapping.get("lookup").get("gender");
			JSONObject nationalityMapping = mapping.get("lookup").get("nationality");
			JSONObject languageMapping = mapping.get("lookup").get("language");
			JSONObject maritalStatusMapping = mapping.get("lookup").get("marital_status");
			JSONObject titleMapping = mapping.get("lookup").get("title");

			JSONObject orgSpecExtId = mapping.get("lookup").get("orgSpecExtId");
			JSONObject statusOrgRootPartyMapping = mapping.get("lookup").get("statusOrgRootParty");

			JSONObject orgIdentificationSpecExtIdMapping = mapping.get("lookup").get("orgIdentificationSpecExtId");

			partyRecord.put("externalId",partyObj.optString("partyexternalid"));
			partyRecord.put("partitionId", commonMapping.get("partition_id"));
			partyRecord.put("isLegalEntity",true);
			partyRecord.put("name",partyObj.optString("organizationname"));
			partyRecord.put("tradingName",partyObj.optString("organizationtradename"));


			//get status getFormattedDate
			JSONObject validFor = getValidFor(getFormattedDate(partyObj.get("statusstartdatetime")));

			JSONObject status = new JSONObject();
			status.put("status",getFromMapping(statusOrgRootPartyMapping,partyObj,"status"));
			status.put("validFor",validFor);

			//get status arr
			JSONArray statusArr = new JSONArray();
			statusArr.put(status);

			//put into party

			partyRecord.put("contactMedium",getContactMediumArray(contactMedium,contactMediumChar,commonMapping,lookupMapping,partyObj));
			partyRecord.put("status",statusArr);
			
			if(partyObj.optString("organizationparentpartyid") != null && !partyObj.optString("organizationparentpartyid").isEmpty()){
				JSONObject organizationParentRelationship = new JSONObject();
				JSONObject organization = new JSONObject();
				organization.put("externalId",partyObj.optString("organizationparentpartyid"));

				organizationParentRelationship.put("organization",organization);
				partyRecord.put("organizationParentRelationship",organizationParentRelationship);
			}
			JSONObject organizationSpecification = new JSONObject();
			organizationSpecification.put("externalId", getFromMapping(orgSpecExtId,partyObj,"organizationspecification"));
			//organizationSpecification.put("externalId",partyObj.optString("organizationspecification"));
			partyRecord.put("organizationSpecification",organizationSpecification);


			JSONArray organizationIdentification = new JSONArray();
			JSONObject organizationIdentificationObj = new JSONObject();

			organizationIdentificationObj.put("externalId",partyObj.optString("partyexternalid"));
			organizationIdentificationObj.put("identificationId",partyObj.optString("identificationid"));
			organizationIdentificationObj.put("identificationType",partyObj.optString("identificationtype"));
			organizationIdentificationObj.put("issuingDate",getFormattedDate(partyObj.optString("issuingdate")));

			JSONObject organizationIdentificationSpecification = new JSONObject();
			organizationIdentificationSpecification.put("externalId",getFromMapping(orgIdentificationSpecExtIdMapping,partyObj,"orgIdentificationSpecExtId"));
			organizationIdentificationObj.put("organizationIdentificationSpecification",organizationIdentificationSpecification);
			organizationIdentification.put(organizationIdentificationObj);
			partyRecord.put("organizationIdentification",organizationIdentification);

			partyRecord.put("action", "create");

			sorted.put(partyObj.optString("partyexternalid"),partyRecord)
			//LogUtil.logDebug(logger, MessageFormat.format("Party Array = {0}", partyArr.toString()));

		}

		for (Map.Entry<String, Integer> entry : sorted.entrySet()){
			partyArr.put(entry.getValue())
		}
		return partyArr;

	}


	private String getFromMapping(JSONObject mapping, JSONObject sourceJson, String fieldName)
	{
		String object = null;

		for(String objKey : mapping.get("mapping").keySet())
		{
			object = mapping.get("mapping").get(objKey);
			if(objKey.equals(sourceJson.optString(fieldName)))
			{
				break;
			}
		}

		return object;
	}

	private JSONObject getValidFor(Object val) {
		JSONObject startDateTime = new JSONObject();
		startDateTime.put("startDateTime", val);
		return startDateTime;
	}

	private JSONArray getContactMediumArray(JSONArray contactMediumArr, JSONArray contactMediumChar, JSONObject commonMapping, JSONObject lookupMapping,JSONObject partyObj) {

		JSONArray cMediumArr = new JSONArray();
		JSONObject cMedium = null
        JSONObject characteristic = null
        JSONObject value = null
        JSONArray valueArr = null
        JSONArray characteristicArr = null
        JSONObject validFor = null

		for (JSONObject contactMedium : contactMediumArr) {
			if(contactMedium.optString("partyexternalid").equals(partyObj.optString("partyexternalid"))){
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
		return cMediumArr;

	}

	private JSONArray getPaymentMethodArray(JSONObject pMethod,JSONObject commonMapping) {
		JSONObject paymentMethod = new JSONObject();
		paymentMethod.put("externalId",pMethod.get("partyexternalid"));

		JSONObject paymentMethodSpecification = new JSONObject();
		//paymentMethodSpecification.put("externalId",commonMapping.get("payment_method"));
		//paymentMethodSpecification.put("id","2FCCA6676BFE495B8E47DF9183D0F34B");
		paymentMethodSpecification.put("id",commonMapping.get("payment_method_id"));
		paymentMethod.put("paymentMethodSpecification",paymentMethodSpecification);

		JSONObject validFor = getValidFor(getFormattedDate(pMethod.get("startdatetime")));
		paymentMethod.put("validFor", validFor);
		JSONArray paymentMethodArr = new JSONArray();
		paymentMethodArr.put(paymentMethod);

		return paymentMethodArr;

	}


	private JSONArray getMaritalStatusArray(JSONObject party, JSONObject maritalStatusMappingArr) {
		JSONObject maritalStatus = new JSONObject();

		//maritalStatus.put("status",party.optString("maritalstatus"));
		maritalStatus.put("status",getFromMapping(maritalStatusMappingArr,party,"maritalstatus"));

		//JSONObject validFor = getValidFor(getFormattedDate(status.get("MaritalStatusStartDatetime")));
		JSONObject validFor = getValidFor(getFormattedDate(party.get("statusstartdatetime")));
		maritalStatus.put("validFor", validFor);
		JSONArray maritalStatusArr = new JSONArray();
		maritalStatusArr.put(maritalStatus);

		return maritalStatusArr;

	}

	private String getFormattedDate(String dateStr)
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date dateObj = formatter.parse(dateStr);

		dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(dateObj);

		return dateStr;

	}

}
