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

public class ECEV_CS_BillingAccountEntity {
	
	private ECEV_CS_AtRulesConstant atrules = new ECEV_CS_AtRulesConstant();

	//private static final Logger LOGGER = LoggerFactory.getLogger(ECEV_CS_BillingAccountEntity.class);


	public JSONArray createBillingAccountEntity(String externalId, JSONObject commonMapping, String unit, String currentTime) {
		JSONArray bAEntity = new JSONArray();
		JSONObject billingAccountRecord = new JSONObject();
		billingAccountRecord.put("triggerTime", currentTime);
		billingAccountRecord.put("customerExternalId", atrules.getExternalId("cust",externalId));
		billingAccountRecord.put("billingAccountExternalId", atrules.getExternalId("ba",externalId));
		billingAccountRecord.put("billingAccountBucketSpecExternalId", commonMapping.get("bab_spec_external_id"));
		billingAccountRecord.put("reason", "Migration");
		billingAccountRecord.put("action", "Set");
		billingAccountRecord.put("unitOfMeasure", commonMapping.get("bab_unit_of_measure"));

		JSONObject amount = new JSONObject();
		amount.put("number", Long.parseLong(unit));
		amount.put("decimalPlaces", commonMapping.optInt("bab_decimal_places"));
		billingAccountRecord.put("amount", amount);

		bAEntity.put(billingAccountRecord);

		return bAEntity;
	}
}
