package groovy;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author erganaa
 *
 */
public class ECEV_CS_CreatePartyEntity {

	//private static final Logger LOGGER = LoggerFactory.getLogger(CreatePartyEntity.class);

	JSONArray createPartyEntity(String externalId, String rmPartyKey,
			JSONObject commonMapping,String externalId_before,JSONObject validFor,String fname, String sname) {

		JSONObject partyRecord = new JSONObject();
		JSONArray partyEntity = new JSONArray();

		//partyRecord.put("rmPartyKey", rmPartyKey);

		partyRecord.put("externalId", externalId);

		partyRecord.put("partitionId", "1");
		
		//----
		if (!fname.equals("")) {
			partyRecord.put("givenName", fname);
						
		}
		if (!sname.equals("")) {
			partyRecord.put("familyName", sname);
						
		}
		//----

		JSONArray langauage = new JSONArray();

		langauage.put(commonMapping.get("language"));
		partyRecord.put("language", langauage);
		partyRecord.put("action", "create");
		// partyRecord.put("dateOfBirth","1980-08-24T11:09:57.340-03:00");
		
		// JSONArray maritalStatusArr = new JSONArray();
		// JSONObject maritalStatus = new JSONObject();
		// maritalStatus.put("maritalStatusId","Married");
		// maritalStatus.put("validFor",validFor);
		// maritalStatusArr.put(maritalStatus);
		
		// partyRecord.put("title","Mr");
		// partyRecord.put("maritalStatus",maritalStatusArr);
		// partyRecord.put("gender","Male");
		// partyRecord.put("middleName","SubscriberMiddleName_2024");

		JSONArray contactMediumArray = new JSONArray();

		JSONObject contactMediumObject = new JSONObject();

		contactMediumObject.put("externalId", "cm_sms_" + externalId);

		contactMediumObject.put("validFor", validFor);

		JSONArray characteristicArray = new JSONArray();

		JSONObject charObj1 = new JSONObject();

		charObj1.put("charSpecExternalId", commonMapping.get("cm_char_sms_channel_type"));

		charObj1.put("validFor", validFor);

		JSONArray valArrayCharObj1 = new JSONArray();

		JSONObject valObject1 = new JSONObject();

		valObject1.put("value", commonMapping.get("cm_char_sms_channel_type_value"));

		valArrayCharObj1.put(valObject1);

		charObj1.put("value", valArrayCharObj1);

		characteristicArray.put(charObj1);

		JSONObject charObj2 = new JSONObject();

		charObj2.put("charSpecExternalId", commonMapping.get("cm_char_sms_comm_id"));

		charObj2.put("validFor", validFor);

		JSONArray valArrayCharObj2 = new JSONArray();

		JSONObject valObject2 = new JSONObject();

		//valObject2.put("value", externalId);
		
		String countryCode = commonMapping.get("country_code");
		StringBuilder sb = new StringBuilder(countryCode);
		sb.append(externalId_before);

		valObject2.put("value", sb.toString());

		valArrayCharObj2.put(valObject2);

		charObj2.put("value", valArrayCharObj2);

		characteristicArray.put(charObj2);

		contactMediumObject.put("characteristic", characteristicArray);

		contactMediumObject.put("contactMediumSpecExternalId", commonMapping.get("cm_sms"));

		contactMediumArray.put(contactMediumObject);

		partyRecord.put("contactMedium", contactMediumArray);
                
                //added by abhay
                JSONObject individualSpecification = new JSONObject();
                individualSpecification.put("externalId","Party Specification Prepaid")
                //partyRecord.put("individualSpecification",individualSpecification);
               
		JSONArray statuses = getStatusArray("PartyActive", validFor);

		partyRecord.put("status", statuses);

		partyEntity.put(partyRecord);

		return partyEntity;
	}

	private JSONArray getStatusArray(String statusMsg, JSONObject validFor) {
		JSONArray statuses = new JSONArray();
		JSONObject status = new JSONObject();
		status.put("status", statusMsg);
		status.put("validFor", validFor);
		statuses.put(status);
		return statuses;
	}

}
