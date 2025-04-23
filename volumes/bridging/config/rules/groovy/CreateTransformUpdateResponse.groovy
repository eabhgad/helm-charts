package groovy;

import java.util.HashMap

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC

import com.ericsson.datamigration.bss.transformation.utils.DateTimeUtil;
import com.ericsson.datamigration.bss.transformation.utils.ResourceHandler;
import com.ericsson.datamigration.bss.transformation.utils.SequenceGenerator;
import com.jayway.jsonpath.ReadContext;
import com.ericsson.datamigration.bridging.rocksdb.RocksDBRepository;
import com.ericsson.datamigration.bridging.rocksdb.ProviderConsumerInfo;
import org.rocksdb.RocksIterator;
import org.springframework.util.SerializationUtils;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;



class CreateTransformUpdateResponse {
	private static final Logger LOGGER = LoggerFactory.getLogger(CreateTransformUpdateResponse.class);
	private AtRulesConstant atrules = new AtRulesConstant();

	def String execute(String input){
		//LOGGER.debug("executing groovy scripts: input {} ",input);
		JSONObject inputJson = new JSONObject(input);
		JSONObject output = createUpdateRequest(inputJson);
		//LOGGER.debug("Update output {}", output);
		return output.toString();
	}
	private JSONObject createUpdateRequest(JSONObject input){
		JSONObject output =  new JSONObject();
		JSONObject inputJson = input.getJSONObject("BODY");
		String chunkId = input.optString("chunk_id");
		String luwId = input.optString("luw_id");
		String provCustExtId = "";
		
		////----------Retrieve Required Table Data from Source - Start-------------------//
		JSONArray provconsmapArr = inputJson.getJSONArray("provider_consumer_map");	
		JSONObject mapping = ResourceHandler.getResourceAsJSON("mapping.json");
		////----------Retrieve Required Table Data from Source - End-------------------//
		
		////----------Creating Contract Payload Start--------------///
		JSONArray contractArray = new JSONArray();
		
		if(provconsmapArr !=null && !provconsmapArr.empty)
		{
			//---------------------- Creating provider payload start------------------//
			for(JSONObject prov:provconsmapArr)
			{
				if(prov.optString("entitytype").equals("provider"))
				{
					JSONObject providerContract = new JSONObject();
					//providerContract.put("rmOperationKey","update");
					providerContract.put("externalId",prov.optString("contractexternalid"));
					
					//---------------relatedPartyInteractionRole start -------------//
					JSONArray relatedPartyInteractionRoleArr = new JSONArray();
					JSONObject relatedPartyInteractionRoleObj= new JSONObject();

					relatedPartyInteractionRoleObj.put("partyRoleExternalId", prov.optString("customerexternalid"));
					provCustExtId = prov.optString("customerexternalid"); // Provider Customer external id would be same

					relatedPartyInteractionRoleArr.put(relatedPartyInteractionRoleObj);
					providerContract.put("relatedPartyInteractionRole",relatedPartyInteractionRoleArr);
					//---------------relatedPartyInteractionRole end -------------//
					
					//---------------product start--------------------//
					JSONArray productArr = new JSONArray();
					JSONObject productObj= new JSONObject();
					productObj.put("externalId",prov.optString("productexternalid"));
					//productObj.put("productOfferingExternalId",prov.optString("productofferingexternalid"));
					productObj.put("productOfferingExternalId",getProductMapping(mapping.get("products"),prov,"productofferingexternalid"));
					
					//--------------sharingProvider start-------------------//
					JSONObject sharingProviderObj= new JSONObject();
					
					//---------------billingAccount start--------------------//
					JSONArray billingAccountArr = new JSONArray();
					JSONObject billingAccountObj= new JSONObject();
					billingAccountObj.put("externalId",prov.optString("billingaccountexternalid"));
					billingAccountArr.put(billingAccountObj);
					sharingProviderObj.put("billingAccount",billingAccountArr);
					//---------------billingAccount end--------------------//
					
					//---------------consumerList start------------------//
					JSONArray consumerListArr = new JSONArray();
					for(JSONObject cons:provconsmapArr)
					{
						if(prov.optString("contractexternalid").equals(cons.optString("providercontractexternalid")) &&
							prov.optString("productexternalid").equals(cons.optString("providerproductexternalid")) &&
							cons.optString("entitytype").equals("consumer"))
						{
							JSONObject consumerListObj= new JSONObject();
							consumerListObj.put("consumerContractExternalId",cons.optString("contractexternalid"));
							consumerListObj.put("consumerCustomerExternalId",cons.optString("customerexternalid"));
							consumerListObj.put("externalId",cons.optString("consumerlistentryexternalid"));
							consumerListArr.put(consumerListObj);
						}
					}
					sharingProviderObj.put("consumerList",consumerListArr);
					//---------------consumerList end------------------//
					
					productObj.put("sharingProvider",sharingProviderObj);
					//--------------sharingProvider end-------------------//
					
					productArr.put(productObj);
					providerContract.put("product",productArr);

					providerContract.put("action", "update");
					//---------------product end--------------------//
					
					contractArray.put(providerContract);
				}
			
			}
			//---------------------- Creating provider payload end------------------//

			
			//---------------------- Creating consumer payload start------------------//
			for(JSONObject cons:provconsmapArr)
			{
				if(cons.optString("entitytype").equals("consumer"))
				{
					JSONObject consumerContract = new JSONObject();
					//consumerContract.put("rmOperationKey","update");
					consumerContract.put("externalId",cons.optString("contractexternalid"));
					
					//---------------relatedPartyInteractionRole start -------------//
					JSONArray relatedPartyInteractionRoleArr = new JSONArray();
					JSONObject relatedPartyInteractionRoleObj= new JSONObject();

					relatedPartyInteractionRoleObj.put("partyRoleExternalId", cons.optString("customerexternalid"));

					relatedPartyInteractionRoleArr.put(relatedPartyInteractionRoleObj);
					consumerContract.put("relatedPartyInteractionRole",relatedPartyInteractionRoleArr);
					//---------------relatedPartyInteractionRole end -------------//
					
					//---------------product start--------------------//
					JSONArray productArr = new JSONArray();
					JSONObject productObj= new JSONObject();
					productObj.put("externalId",cons.optString("productexternalid"));
					//productObj.put("productOfferingExternalId",cons.optString("productofferingexternalid"));
					productObj.put("productOfferingExternalId",getProductMapping(mapping.get("products"),cons,"productofferingexternalid"));
					
					//---------------sharingConsumer start--------------------//
					JSONObject sharingConsumerObj= new JSONObject();
					sharingConsumerObj.put("consumerListEntryExternalId",cons.optString("consumerlistentryexternalid"));
					sharingConsumerObj.put("providerContractExternalId",cons.optString("providercontractexternalid"));
					sharingConsumerObj.put("providerProductExternalId",cons.optString("providerproductexternalid"));
					sharingConsumerObj.put("providerCustomerExternalId",provCustExtId);
					productObj.put("sharingConsumer", sharingConsumerObj);
					//---------------sharingConsumer end--------------------//
					
					//---------------status start--------------------//
					JSONArray statusArr = new JSONArray();
					JSONObject statustObj= new JSONObject();
					JSONObject validFor =  new JSONObject();
					
					
					statustObj.put("reason", cons.optString("productstatusreason"));
					//statustObj.put("reason", "PRA");
					statustObj.put("status", cons.optString("productstatus"));
					
					
					validFor = getValidFor(getFormattedDate(cons.get("productstatusvalidfrom")));
					if((!cons.optString("productstatusvalidto").equals("")))
					{
						validFor.put("endDateTime", getFormattedDate(cons.get("productstatusvalidto")));
					}
					
					statustObj.put("validFor", validFor);
					statusArr.put(statustObj);
					productObj.put("status", statusArr);
					//---------------status end--------------------//
					
					productArr.put(productObj);
					consumerContract.put("product",productArr);

					consumerContract.put("action", "update");
					//---------------product end--------------------//
					
					contractArray.put(consumerContract);
				}
			
			}
			
			//---------------------- Creating consumer payload end------------------//
		}
		
		
		if(contractArray.length() > 0)
		{
			output.put("contract", contractArray);
			output.put("isValid", "true");
			////----------Creating Contract Payload End--------------///
			
		}
		else
		{
			output.put("channelName", populateChannelInfo());
			output.put("isValid", "false");
			
			JSONObject warn = new JSONObject();
			warn.put(atrules.ERROR_CODE, atrules.ERR_INC01);
			warn.put(atrules.ERROR_MSG, atrules.ERR_MSG_INC01);
			printLog(warn, atrules.WARN_LEVEL, chunkId, luwId);
		}
		output.put("chunk_id", chunkId);
		output.put("luw_id", luwId);
		
		return output;
	
	}
	
	private JSONObject getValidFor(Object val) {
		JSONObject startDateTime = new JSONObject();
		startDateTime.put("startDateTime", val);
		return startDateTime;
	}
	
	private String getFormattedDate(String dateStr)
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date dateObj = formatter.parse(dateStr);
		
		dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(dateObj);
		
		return dateStr;
	
	}
	
	private JSONArray populateChannelInfo(){
		JSONArray channelInfo = new JSONArray();
		JSONObject topicInfo = new JSONObject();
		topicInfo.put("topic_name","CONSUMERS_TE-GROOVY");
		channelInfo.put(topicInfo);
		return channelInfo;
	}
	
	private void printLog(JSONObject error, int logLevel, String chunkId, String luwId)
	{
		MDC.clear();
		MDC.put(atrules.TE_ERR_RULE_KEY, atrules.TE_ERR_RULE);

		for(key in error.keySet()){
			MDC.put(key, error.get(key));
		}
		MDC.put(atrules.CHUNKID, chunkId);
		MDC.put(atrules.LUWID, luwId);
		logLevel == atrules.WARN_LEVEL? LOGGER.warn(""):LOGGER.error("");
		MDC.clear();
	}
	
	private String getProductMapping(JSONObject mapping, JSONObject sourceJson, String fieldName)
	{
		String externalId = null;
		for(String objKey : mapping.keySet()) 
		{
			// Validation parameters will be added in here
			/* JSONObject product = productmapping.get(objKey);
			if(product.get("is_provider").equals("true") || product.get("is_consumer").equals("true")) 
			{
				if(product.get("is_provider").equals("true") && product.get("is_consumer").equals("true"))	{
					//todo
				} else if(product.get("is_provider").equals("true")) {
					//todo
				} else if(product.get("is_consumer").equals("true")) {
					//todo
				} 
			} else {
				//if its not a provider or consumer
			}
			*/
			externalId = mapping.get(objKey).get("external_id");
			if(objKey.equals(sourceJson.optString(fieldName)))
			{
				break;
			}
		}
		return externalId;
	}
	
}
	
