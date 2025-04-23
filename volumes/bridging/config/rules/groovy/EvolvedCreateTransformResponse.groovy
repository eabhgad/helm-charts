package groovy

import com.ericsson.datamigration.bss.transformation.utils.ResourceHandler
import com.ericsson.datamigration.log.dto.KafkaMsgIOLogBuilder
import com.ericsson.datamigration.log.utils.LogUtil
import com.jayway.jsonpath.JsonPath
import com.jsoniter.output.JsonStream
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

/**
 * @author emubhka
 */
class CreateTransformResponse {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateTransformResponse.class);

	private AtRulesConstant atrules = new AtRulesConstant();

	def String execute(String input){
		LOGGER.debug("executing groovy scripts: input {} ",input);
		long startTime = System.currentTimeMillis();
		JSONObject inputJson = new JSONObject(input);

		inputJson.put("source","evolved");

		JSONObject output = inputJson;
		if (output.optString("isValid").isEmpty())
			output.put("isValid", "true");

		//JSONObject output = createEntities(inputJson);
		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;
		System.out.println("Time taken: " + duration + " milliseconds");
		LOGGER.debug("output {}", output);
		return output.toString();
	}

	private JSONObject createEntities(JSONObject json) {

		JSONObject output = null;
		KafkaMsgIOLogBuilder logBuilder = null;
		JSONObject inputJson = json.getJSONObject("BODY");
		JSONObject topicJson = new JSONObject();
		JSONArray channelJson = new JSONArray();

		String chunkId = json.optString("chunk_id");
		String luwId = json.optString("luw_id");
		String action = json.optString("action");
        String migration_type = json.optString("migration_type");

		output = createResponse(inputJson, chunkId, luwId,action,migration_type);
		output.put("chunk_id", json.get("chunk_id"));
		output.put("luw_id", json.get("luw_id"));
		if (output.optString("isValid").isEmpty())
			output.put("isValid", "true");

		//System.out.println("output {} TE VALIDATION",output.toString());

		if(!output.has("channelName")){

			if(output.getJSONArray("party").getJSONObject(0).has("response") ||
					output.getJSONArray("customer").getJSONObject(0).has("response") ||
					output.getJSONArray("contract").getJSONObject(0).has("response")){
				//output.put("validation", "TE_VALIDATION");
			
				LOGGER.debug("Luw Rejected {} TE VALIDATION",luwId);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"REJECTED",atrules.VALIDATION_LOGCODE);

				topicJson.put("topic_name", atrules.ERROR_TOPIC_NAME);
				channelJson.put(topicJson);
				output.put("channelName", channelJson);
				output.put("isValid", "false");

				JSONObject te_error_log = new JSONObject();
				JSONArray messageLog = new JSONArray();
				te_error_log.put("error_generation_point",atrules.ERROR_GENERATION_POINT);
				String allErrorCodes="";

				if(output.getJSONArray("party").getJSONObject(0).has("response")){
					JSONArray allErrors = output.getJSONArray("party").getJSONObject(0).getJSONObject("response").getJSONArray("messages")
					for(int i=0 ;i<allErrors.length();i++){
						String err = allErrors.getJSONObject(i).optString("errorId")
						if(!allErrorCodes.isEmpty()){
							allErrorCodes = allErrorCodes + " | " +err
						}
						else{
							allErrorCodes= err
						}
					}
				}
				if(output.getJSONArray("customer").getJSONObject(0).has("response")){
					JSONArray allErrors = output.getJSONArray("customer").getJSONObject(0).getJSONObject("response").getJSONArray("messages")
					for(int i=0 ;i<allErrors.length();i++){
						String err = allErrors.getJSONObject(i).optString("errorId")
						if(!allErrorCodes.isEmpty()){
							allErrorCodes = allErrorCodes + " | " +err
						}
						else{
							allErrorCodes= err
						}
					}
				}
				if(output.getJSONArray("contract").getJSONObject(0).has("response")){
					JSONArray allErrors = output.getJSONArray("contract").getJSONObject(0).getJSONObject("response").getJSONArray("messages")
					for(int i=0 ;i<allErrors.length();i++){
						String err = allErrors.getJSONObject(i).optString("errorId")
						if(!allErrorCodes.isEmpty()){
							allErrorCodes = allErrorCodes + " | " +err
						}
						else{
							allErrorCodes= err
						}
					}
				}

				te_error_log.put("error_id",allErrorCodes);
				JSONObject mapping = ResourceHandler.getResourceAsJSON("mapping.json");
				JSONObject te_error_source= mapping.getJSONObject("te_error_source");
				JSONObject obj = null;

				for (String key : te_error_source.keySet()) {
					LOGGER.debug("Key = " + key);
					//String temp= te_error_source.getJSONObject(key).optString("FIELD_EXTERNAL_ID_PATH");
					Map<String, List<String>> subKeyValMap = new HashMap<>();
					int maxSubKeyValSize = 0;
					for (String subKey : te_error_source.getJSONObject(key).keySet()) {
						LOGGER.debug("Sub-key = " + subKey);
						List<String> subKeyValues = JsonPath.read(inputJson.toString(), te_error_source.getJSONObject(key).optString(subKey));
						LOGGER.debug("Sub-key values = " + subKeyValues.toString());
						subKeyValMap.put(subKey, subKeyValues);
						if (subKeyValues.size() > maxSubKeyValSize)
							maxSubKeyValSize = subKeyValues.size();
					}
					LOGGER.debug("maxSubKeyValSize = " + maxSubKeyValSize);
					for (int i = 0; i < maxSubKeyValSize; i++) {
						obj = new JSONObject();
						obj.put("field_type", key);
						for (Map.Entry<String, List<String>> entry : subKeyValMap.entrySet()) {
							if (i < entry.getValue().size())
								obj.put(entry.getKey(), entry.getValue().get(i));
						}
						messageLog.put(obj);
					}

					//String temp2= te_error_source.getJSONObject(key).optString("FIELD_EXTERNAL_ID_PATH");
				}

				te_error_log.put("messages",messageLog);
				output.put("te_error_log",te_error_log);
				LogUtil.logMsgJsonStr(LOGGER, JsonStream.serialize(logBuilder), output.toString());
				return output;
			}
		}

		return output;
	}

	private JSONObject createResponse(JSONObject inputJson, String chunkId, String luwId,String action,String migration_type)
	{
		JSONObject output = new JSONObject();

		JSONArray contactMediumAssociation = inputJson.getJSONArray("stg_beam_contact_medium_assoc");
		JSONArray partyRole = inputJson.getJSONArray("stg_beam_party_role");
		JSONObject mapping = ResourceHandler.getResourceAsJSON("mapping.json");
		JSONObject party = (JSONObject)inputJson.getJSONArray("stg_beam_party").get(0);
		JSONArray contactMediumArr = inputJson.getJSONArray("stg_beam_contact_medium");
		JSONArray contactMediumChar = inputJson.getJSONArray("stg_beam_contact_medium_char");
		JSONObject paymentMethod = (JSONObject)inputJson.optJSONArray("stg_beam_pay_method").opt(0);
		JSONObject customer = (JSONObject)inputJson.getJSONArray("stg_beam_customer").get(0);
		JSONArray billingAccountArr = inputJson.getJSONArray("stg_beam_billing_act");
		JSONObject billCycleHist = (JSONObject)inputJson.optJSONArray("stg_beam_billycycle_hist").opt(0);
		JSONArray billCycleHistArray = inputJson.optJSONArray("stg_beam_billycycle_hist");

		JSONArray contractArr = inputJson.getJSONArray("stg_beam_contract");
		JSONArray contractCharac = inputJson.getJSONArray("stg_beam_contr_charc");
		JSONArray contStatHistArr = inputJson.getJSONArray("stg_beam_cont_stat_hist");
		JSONObject partyInterRole = (JSONObject)inputJson.getJSONArray("stg_beam_party_inter_role").get(0);
		JSONObject finHead = (JSONObject)inputJson.optJSONArray("stg_beam_fin_head").opt(0);
		JSONObject finEntry = (JSONObject)inputJson.optJSONArray("stg_beam_fin_entry").opt(0);
		JSONArray finCharc = inputJson.optJSONArray("stg_beam_fin_charac");
		JSONArray resources = (JSONArray)inputJson.getJSONArray("stg_beam_resource");
		JSONArray products = (JSONArray)inputJson.getJSONArray("stg_beam_product");
        JSONArray priceArr = (JSONArray)inputJson.getJSONArray("stg_beam_product_price");
		JSONArray prodStatHistArr = (JSONArray)inputJson.getJSONArray("stg_beam_product_stat_hist");
		//JSONArray stg_beam_bucket_balance = inputJson.getJSONArray("stg_beam_bucket_balance");
		//String partyKey = SequenceGenerator.nextCustomSequence("RM_PR", "party1");

		//Rejecting the Luw based on Error Ids which is populated based SD stamping
		KafkaMsgIOLogBuilder logBuilder = null;

		//JSONObject TE_VALIDATION_PARTY = mapping.getJSONObject("te_error_validation").getJSONObject("Party").getJSONObject("error_id");
		
		
		JSONObject errorDetails = (JSONObject)inputJson.getJSONObject("stg_beam_luw");
		String errorIds = errorDetails.optString("error_id");

		if(!errorIds.isEmpty() && errorIds != null){
			LOGGER.debug("Luw Rejected {} Error Ids :{}",luwId,errorIds);
			logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"REJECTED",atrules.VALIDATION_LOGCODE);
			LogUtil.logMsgJsonStr(LOGGER, JsonStream.serialize(logBuilder), errorDetails.toString());

			JSONObject topicJson = new JSONObject();
			JSONArray channelJson = new JSONArray();

			topicJson.put("topic_name", atrules.ERROR_TOPIC_NAME);
			channelJson.put(topicJson);
			output.put("channelName", channelJson);

			output.put("isValid", "false");

			return output;

		} else {

			///-----Party Start----------///
			CreatePartyEntity partyEntityObj = new CreatePartyEntity();
			JSONArray partyEntity = partyEntityObj.createPartyEntity(party,contactMediumArr,contactMediumChar,paymentMethod,mapping);

			output.put("party", partyEntity);
			///-----Party End------------///

			///-----Customer Start-------///
			CreateCustomerEntity customerEntityObj = new CreateCustomerEntity();
			JSONArray customerEntity = customerEntityObj.createCustomerEntity(customer,billingAccountArr,contactMediumAssociation,billCycleHistArray,mapping,partyRole,migration_type);
			output.put("customer", customerEntity)
			///-----Customer End---------///

			///-----Contract Start-------///
			CreateContractEntity contractEntityObj = new CreateContractEntity();
			List<JSONArray> resp = contractEntityObj.createContractEntity(contractArr,billingAccountArr,partyInterRole,resources,contactMediumAssociation,products,prodStatHistArr,contractCharac,contStatHistArr,mapping,priceArr);
			output.put("contract", resp.get(0));
			///-----Contract End-------///

			/**
			 * Separting the B2B and B2C with below condition
			 * hierarchylevel = 0 and Should not contains the field organizationparentpartyid then it is B2C record
			 * hierarchylevel = 0 and organizationparentpartyid != null then it is B2B individual Create
			 */

			if((party.optString("hierarchylevel")).equals("0") && !party.has("organizationparentpartyid") ){
				///-----FinancialTransaction Start-------///
				if(finHead != null && finEntry != null && finCharc != null) {
					CreateFinancialTransactionEntity financialTransactionEntityObj = new CreateFinancialTransactionEntity();
					JSONArray financialTransactionEntity = financialTransactionEntityObj.createFinancialTransactionEntity(finHead,finEntry,finCharc,mapping);
					output.put("financialTransaction", financialTransactionEntity);
				}
				///-----FinancialTransaction End-------///
			}
			else if((party.optString("hierarchylevel")).equals("0") && party.optString("organizationparentpartyid") != null ){
				CreatePartyRoleEntity partyRoleEntityObj = new CreatePartyRoleEntity();
				JSONArray partyRoleEntity = partyRoleEntityObj.createPartyRoleEntity(partyRole,contactMediumAssociation,mapping,"OTHERS");
				output.put("partyRole", partyRoleEntity);
			}

			if(action.equals("CREATE_ADUSTMENT")){
				JSONArray stg_beam_bucket_balance = inputJson.getJSONArray("stg_beam_bucket_balance");
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
			}

			return output;
		}
	}

}