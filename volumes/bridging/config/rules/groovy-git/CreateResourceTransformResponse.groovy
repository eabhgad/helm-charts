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

class CreateResourceTransformResponse {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateResourceTransformResponse.class);

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
		JSONObject topicJson = new JSONObject();
		JSONArray channelJson = new JSONArray();

		String chunkId = json.optString("chunk_id");
		String luwId = json.optString("luw_id");
		String action = json.optString("action");
                String migration_type = json.optString("migration_type");

		output = createResourceResponse(inputJson, chunkId, luwId,action,migration_type);

		output.put("chunk_id", json.get("chunk_id"));
		output.put("luw_id", json.get("luw_id"));
		output.put("isValid", "true");
		//topicJson.put("topic_name", "ATOMICINBOUNDP10");
		//channelJson.put(topicJson);
		//output.put("channelName", channelJson);
		return output;
	}

	private JSONObject createResourceResponse(JSONObject inputJson, String chunkId, String luwId,String action,String migration_type)
	{
		JSONObject output = new JSONObject();

	        JSONObject mapping = ResourceHandler.getResourceAsJSON("mapping.json");
		JSONArray resources = (JSONArray)inputJson.getJSONArray("stg_beam_resource");

		JSONObject resourceObj = resources.getJSONObject(0);
		
		///-----ResourceEntity Start----------///
		CreateResourceEntity resourceEntityObj = new CreateResourceEntity();
		JSONArray resourceEntity = resourceEntityObj .createResourceEntity(resources,mapping);
		output.put("resource", resourceEntity);
		output.put("contractexternalid",resourceObj.get("contractexternalid"));
		output.put("customertexternalid",resourceObj.get("customerexternalid"));
		///-----ResourceEntity End------------///

		
		return output;
	}

}