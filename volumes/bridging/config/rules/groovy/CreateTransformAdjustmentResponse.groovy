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

class CreateTransformAdjustmentResponse {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateTransformAdjustmentResponse.class);

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
		
		output = createResponse(inputJson, chunkId, luwId);
		
		output.put("chunk_id", json.get("chunk_id"));
		output.put("luw_id", json.get("luw_id"));
        output.put("isValid", "true");
		return output;
	}	
	
	private JSONObject createResponse(JSONObject inputJson, String chunkId, String luwId)
	{
		JSONObject output = new JSONObject();

		JSONArray stg_beam_bucket_balance = inputJson.getJSONArray("stg_beam_bucket_balance");
        JSONArray billingAccountArr = inputJson.getJSONArray("stg_beam_billing_act");
        JSONArray products = (JSONArray)inputJson.getJSONArray("stg_beam_product");
		JSONObject customer = (JSONObject)inputJson.getJSONArray("stg_beam_customer").get(0);
		
		JSONObject mapping = ResourceHandler.getResourceAsJSON("mapping.json");	
		
		///-----BillAccountAdjustmentEntity Start----------///
		AdjustmentBillAccountEntity billAccountAdjObj = new AdjustmentBillAccountEntity();
		//JSONArray billAccountAdjEntity = billAccountAdjObj.billAccountAdjEntity(stg_beam_bucket_balance,mapping,billingAccountArr,"DEFAULT");
		JSONArray billAccountAdjEntity = billAccountAdjObj.billAccountAdjEntity(stg_beam_bucket_balance,mapping,billingAccountArr);
		
		// for(int i=0;i<billAccountAdjEntityRel.length();i++) {
		// 	billAccountAdjEntity.put(billAccountAdjEntityRel.get(i));
		// }
		output.put("bABucket", billAccountAdjEntity);
		///-----BillAccountAdjustmentEntity End------------///
		
		
		///-----ProductAdjustment Start-------///	
		AdjustmentProductEntity productAdjustmentEntity = new AdjustmentProductEntity();
		//JSONArray productAdjArray = productAdjustmentEntity.productAdjEntity(stg_beam_bucket_balance,mapping,products,"DEFAULT",customer);	
		JSONArray productAdjArray = productAdjustmentEntity.productAdjEntity(stg_beam_bucket_balance,mapping,products,customer);	
		// for(int i=0;i<productAdjArrayRel.length();i++) {
		// 	productAdjArray.put(productAdjArrayRel.get(i));
		// }
		output.put("productBucket", productAdjArray);
		///-----ProductAdjustment End-------///

		return output;
		
		
	}
}
