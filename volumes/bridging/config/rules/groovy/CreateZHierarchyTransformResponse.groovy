package groovy;
/**
 * @author emubhka
 */
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC

import java.util.Optional;
import com.ericsson.datamigration.bss.transformation.utils.DateTimeUtil;
import com.ericsson.datamigration.bss.transformation.utils.ResourceHandler;
import com.ericsson.datamigration.bss.transformation.utils.SequenceGenerator;

import com.ericsson.datamigration.bridging.rocksdb.ExtIdInfo;
import com.ericsson.datamigration.bridging.rocksdb.RocksDBRepository;

class CreateZHierarchyTransformResponse {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateZHierarchyTransformResponse.class);

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
                String migration_type = json.optString("migration_type");

		output = createResponse(inputJson, chunkId, luwId,migration_type);

		output.put("chunk_id", json.get("chunk_id"));
		output.put("luw_id", json.get("luw_id"));
		output.put("isValid", "true");
		return output;
	}

	private JSONObject createResponse(JSONObject inputJson, String chunkId, String luwId,String migration_type) {
		JSONObject output = new JSONObject();

		JSONObject mapping = ResourceHandler.getResourceAsJSON("mapping.json");
		JSONArray party = inputJson.getJSONArray("stg_beam_party");
		JSONArray partyRole = inputJson.getJSONArray("stg_beam_party_role");

		JSONArray contactMedium = inputJson.getJSONArray("stg_beam_contact_medium");
		JSONArray contactMediumChar = inputJson.getJSONArray("stg_beam_contact_medium_char");
		JSONArray contactMediumAssociation = inputJson.getJSONArray("stg_beam_contact_medium_assoc");
		JSONObject customer = (JSONObject)inputJson.getJSONArray("stg_beam_customer").get(0);
		JSONArray billingAccountArr = inputJson.getJSONArray("stg_beam_billing_act");
		JSONObject billCycleHist = (JSONObject)inputJson.getJSONArray("stg_beam_billycycle_hist").get(0);
		JSONArray billCycleHistArray = inputJson.optJSONArray("stg_beam_billycycle_hist");

		//String partyKey = SequenceGenerator.nextCustomSequence("RM_PR", "party1");

		///-----Party Start----------///
		CreateOrganizationPartyEntity organizationPartEntityObj = new CreateOrganizationPartyEntity();
		JSONArray organizationPartyEntity = organizationPartEntityObj.createOrganizationPartyEntity(party,contactMedium, contactMediumChar,mapping);
		output.put("organizationParty", organizationPartyEntity);
		///-----Party End------------///

		///-----Party Role Start----------///
		CreatePartyRoleEntity partyRoleEntityObj = new CreatePartyRoleEntity();
		JSONArray partyRoleEntity = partyRoleEntityObj.createPartyRoleEntity(partyRole,contactMediumAssociation,mapping,"OTHERS");
		output.put("partyRole", partyRoleEntity);
		///-----Party Role End------------///

		///-----Customer Start-------///
		CreateCustomerEntity customerEntityObj = new CreateCustomerEntity();
		JSONArray customerEntity = customerEntityObj.createCustomerEntity(customer,billingAccountArr,contactMedium,billCycleHistArray,mapping,partyRole,migration_type);
		output.put("customer", customerEntity)
		///-----Customer End---------///

		///-----Party Role IIR Start----------///
		//Creating the PARTY ROLE IIR as a separate entity, This entity needs to be created after customer creation
		JSONArray partyRoleEntityIIR = partyRoleEntityObj.createPartyRoleEntity(partyRole,contactMediumAssociation,mapping,"IIR");
		output.put("partyRoleIIR", partyRoleEntityIIR);
		///-----Party Role End------------///

		return output;
	}
}
