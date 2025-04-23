package groovy;
/**
 * @author edesanu
 */
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC

import com.ericsson.datamigration.bss.transformation.utils.DateTimeUtil;
import com.ericsson.datamigration.bss.transformation.utils.ResourceHandler;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;

class UpdateUnbilledUsages {

	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateUnbilledUsages.class);

	def String execute(String input){
		LOGGER.debug("executing groovy scripts: input {} ",input);
		JSONObject inputJson = new JSONObject(input);
		JSONObject output = createEntities(inputJson);
		LOGGER.debug("output {}", output);
		return output.toString();
	}

	private JSONObject createEntities(JSONObject json) {
	
		JSONObject output = null;
		JSONObject inputJson = json.getJSONObject("BODY");
		
		String chunkId = json.optString("chunk_id");
		String luwId = json.optString("luw_id");
		String action = json.optString("action");
		
		output = createResponse(inputJson, chunkId, luwId, action);
		
		output.put("chunk_id", json.get("chunk_id"));
		output.put("luw_id", json.get("luw_id"));
		output.put("action",json.get("action"));
		output.put("isValid", "true");
		return output;
	}	
	
	private JSONObject createResponse(JSONObject inputJson, String chunkId, String luwId,String action)
	{
		JSONObject output = new JSONObject();
		JSONArray outputArr = new JSONArray();		
		JSONObject mapping = ResourceHandler.getResourceAsJSON("mapping.json");	

		JSONArray inputArray = (JSONArray) inputJson.getJSONArray("stg_beam_cha_offline_cdr");
		JSONArray inputArrayUDD = (JSONArray) inputJson.getJSONArray("stg_beam_cha_offline_cdr_udd");

		LOGGER.debug("input array: {}", inputArray);

		for(JSONObject input: inputArray) {
			JSONObject unbilled = new JSONObject();
			LOGGER.debug("input json: {}", input);
			
			String recordUniqueId = input.get("recorduniqueid");
			
			//CBEV 1.7 supports subscriptionid as object, in future updates, this might be changed to array
			JSONObject subscriptionId = new JSONObject();
			subscriptionId.put("value",input.get("subscriptionid"));
			subscriptionId.put("type",input.get("subscriptionid_type"));

			String srcTimeStamp = input.get("triggertime");
			String triggerTimestamp = getFormattedDate(srcTimeStamp);
			LOGGER.debug("triggerTimestamp: {}", triggerTimestamp);

			unbilled.put("recordUniqueID",recordUniqueId);
			unbilled.put("subscriptionId",subscriptionId);
			unbilled.put("triggerTimestamp",triggerTimestamp);

			//consumedUnits
			JSONObject consumedUnits = new JSONObject();
			int srcValue = Integer.parseInt(input.get("consumedunits_value"));
			String serviceId = null;
			String targetServiceId = null;
			long multfactor;

			for(JSONObject inputUDD: inputArrayUDD) {
				if(inputUDD.get("recorduniqueid").equals(recordUniqueId)) {
					serviceId = inputUDD.get("serviceid");
					targetServiceId = mapping.get("unbilled_usage").get(inputUDD.get("serviceid")).get("ServiceId");
				}
			}

			String targetUnbilledUnit = mapping.get("unbilled_usage").get(serviceId).get("TargetUnit");
			String targetUnit = mapping.get("unit").get(input.get("consumedunits_uom")).get("TargetUnit");

			if(targetUnbilledUnit.equals(targetUnit)) {
				multfactor = Long.parseLong(mapping.get("unit").get(input.get("consumedunits_uom")).get("MultiplicationFactor"));
			} else {
				LOGGER.error("No corresponding mapping found between unbilled usages and unit in catalog mapping. Please verify Catalog mapping.");
			}

			long srcValueMult = srcValue * multfactor;
			
			if(serviceId != null) {
				int decimalPlaces = Integer.parseInt(mapping.get("unbilled_usage").get(serviceId).get("TargetDecimal"));
				double power = Math.pow(10,decimalPlaces);
				long trgtvalue = Math.ceil(srcValueMult * power);
				LOGGER.debug("trgtvalue: {}", trgtvalue);
				consumedUnits.put("number",trgtvalue);
				consumedUnits.put("decimalPlaces",decimalPlaces);
			} else {
				//serviceId does not exist in stg_beam_cha_offline_cdr_udd for the recordUniqueId in stg_beam_cha_offline_cdr
				LOGGER.error("serviceId does not exist in stg_beam_cha_offline_cdr_udd for the recordUniqueId in stg_beam_cha_offline_cdr");
			}
			unbilled.put("consumedUnits", consumedUnits);

			//chargedAmount
			JSONObject chargedAmount = new JSONObject();
			double chargeSrcAmount = Double.parseDouble(input.get("chargedamount_value"));

			String targetChUnit = mapping.get("unit").get(input.get("chargedamount_uom")).get("TargetUnit");
			double multfactorChUnit = Double.parseDouble(mapping.get("unit").get(input.get("chargedamount_uom")).get("MultiplicationFactor"));

			double srcChValueMult = chargeSrcAmount * multfactorChUnit;
			int decimalPlacesCh = Integer.parseInt(mapping.get("unit").get(input.get("chargedamount_uom")).get("TargetDecimal"));

			double powerCh = Math.pow(10,decimalPlacesCh);
			long trgtChValue = Math.ceil(srcChValueMult*powerCh);
			LOGGER.debug("trgtChValue: {}", trgtChValue);

			chargedAmount.put("number", trgtChValue);
			chargedAmount.put("decimalPlaces", decimalPlacesCh);

			unbilled.put("chargedAmount",chargedAmount);

			//userDefinedData
			//Using targetserviceId populated during consumedUnits
			JSONObject userDefinedData = new JSONObject();
			LOGGER.debug("targetServiceId: {}", targetServiceId.toString());
			userDefinedData.put("serviceId", targetServiceId);

			unbilled.put("userDefinedData",userDefinedData);
			LOGGER.debug("unbilled: {}", unbilled.toString());
			//write each record to a file.
			//writeToFile(unbilled);
				
			outputArr.put(unbilled);

		}

		output.put("UsageHistory", outputArr);

		return output;
		
	}

	private String getFormattedDate(String srcTimeStamp)
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date dateObj = formatter.parse(srcTimeStamp);
		String dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(dateObj);

		return dateStr;

	}
}
