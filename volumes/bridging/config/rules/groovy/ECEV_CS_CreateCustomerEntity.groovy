package groovy;

import org.json.JSONArray
import org.json.JSONObject

import com.ericsson.datamigration.bss.transformation.utils.SequenceGenerator
/**
 * 
 * @author erganaa
 *
 */
class ECEV_CS_CreateCustomerEntity {
	private ECEV_CS_AtRulesConstant atrules = new ECEV_CS_AtRulesConstant();

	def JSONArray createCustomerEntity(String externalId, JSONObject commonMapping,JSONObject validFor) {

		JSONObject customerRecord = new JSONObject();
		JSONArray customerEntity = new JSONArray();

		String seqGenerator = SequenceGenerator.nextCustomSequence("U", "customer1");

		//customerRecord.put("rmCustomerKey", ""+seqGenerator);

		customerRecord.put("externalId", atrules.getExternalId("cust",externalId));
		customerRecord.put("action", "create");

		JSONObject customerSpecification = new JSONObject();

		customerSpecification.put("externalId", commonMapping.get("cust_spec"));

		customerRecord.put("customerSpecification", customerSpecification);

		JSONArray statuses = getStatusArray("CustomerActive",validFor);

		customerRecord.put("status", statuses);

		JSONArray accountArray = new JSONArray();

		JSONObject account = new JSONObject();

		account.put("externalId", atrules.getExternalId("ba",externalId));

		account.put("billingAccountSpecExternalId",commonMapping.get("ba_spec") )

		account.put("status", getStatusArray("BillingAccountActive",validFor));

		accountArray.put(account);

		customerRecord.put("account", accountArray);

		JSONArray contactMediumArray = new JSONArray();

		JSONObject contactMediumObj = new JSONObject();

		contactMediumObj.put("contactRole", "Notification");

		contactMediumObj.put("validFor",validFor);

		contactMediumObj.put("contactMediumExternalId", atrules.getExternalId("cm_sms",externalId));

		contactMediumObj.put("language",commonMapping.get("language"));

		contactMediumObj.put("enabled",true);

		contactMediumArray.put(contactMediumObj);

		customerRecord.put("contactMediumAssociation", contactMediumArray);

		customerRecord.put("engagedPartyExternalId", externalId);

		JSONArray hostTimeZoneArray = new JSONArray();

		JSONObject hostTimeZoneObject = new JSONObject();

		hostTimeZoneObject.put("validFor",validFor);

		hostTimeZoneObject.put("timeZone",commonMapping.get("timezone"));

		hostTimeZoneArray.put(hostTimeZoneObject);

		customerRecord.put("homeTimeZone", hostTimeZoneArray);

		customerEntity.put(customerRecord);

		return customerEntity;
	}

	private JSONArray getStatusArray(String statusMsg, JSONObject validFor){
		JSONArray statuses = new JSONArray();
		JSONObject status = new JSONObject();
		status.put("status",statusMsg);
		status.put("validFor", validFor);
		statuses.put(status);
		return statuses;
	}
}
