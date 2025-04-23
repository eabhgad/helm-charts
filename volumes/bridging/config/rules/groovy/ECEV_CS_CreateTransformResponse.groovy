package groovy;
/**
 * @author erganaa
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


class CreateTransformResponse {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateTransformResponse.class);
	private ECEV_CS_AtRulesConstant atrules = new ECEV_CS_AtRulesConstant();

	def String execute(String input){
		LOGGER.debug("executing groovy scripts: input {} ",input);
		JSONObject inputJson = new JSONObject(input);
		JSONObject output = createEntities(inputJson);
		LOGGER.debug("output {}", output);
		return output.toString();
	}

	private JSONObject createEntities(JSONObject json) {
		JSONObject output =  null;
		JSONObject inputJson = json.getJSONObject("BODY");
		JSONObject account = (JSONObject)inputJson.getJSONArray("Account").get(0);
		JSONObject subscriber = (JSONObject)inputJson.getJSONArray("Subscriber").get(0);

		String externalIdBefore = account.getString("id");

		String subscriberStatus = subscriber.optString("subscriber_status");

		int isValidStatus =  validateInput(subscriberStatus,account);
               //int isValidStatus=0;
		
		String chunkId = json.optString("chunk_id");
		String luwId = json.optString("luw_id");
		if(isValidStatus == atrules.SUCCESS || isValidStatus == atrules.AVAILABLE_STATUS){
			String currentTime =  DateTimeUtil.getDateFormatter(atrules.DATE_FORMATTER).getFormattedUTCTime();
			String epochTime = account.optString("activated");
			String startDate = getStartDate(isValidStatus, currentTime, epochTime);
			output = createResponse(inputJson, account, subscriber, externalIdBefore, startDate, currentTime, chunkId, luwId);			
			if(output.has(atrules.ERROR_CODE)){
				printLog(output,chunkId,luwId);				
				output = populateErrResp(output);
				output.put("channelName", populateChannelInfo());
				output.put("isValid", "false");
			}
			else{				
				output.put("isValid", "true");
			}			
		}
		else{
			JSONObject errorObj = createErrorResponse(isValidStatus, account, luwId, subscriberStatus,chunkId,externalIdBefore);
			output = populateErrResp(errorObj);
			output.put("channelName", populateChannelInfo());
			output.put("isValid", "false");
		}
		output.put("chunk_id", chunkId);
		output.put("luw_id", luwId);
		return output;
	}

	private JSONObject createResponse(JSONObject inputJson, JSONObject account,JSONObject subscriber, String originalExternalId,String dateTime, String currentTime, String chunkId, String luwId){

		JSONObject output = new JSONObject();

		ECEV_CS_CreatePartyEntity partyEntityObj = new ECEV_CS_CreatePartyEntity();
		ECEV_CS_CreateCustomerEntity customerEntityObj = new ECEV_CS_CreateCustomerEntity();
		ECEV_CS_CreateContractEntity contractEntityObj = new ECEV_CS_CreateContractEntity();
		ECEV_CS_BillingAccountEntity billingEntityObj = new ECEV_CS_BillingAccountEntity();
		
		//-----
		
		JSONObject simImsiJson = (JSONObject)inputJson.getJSONArray("SimImsi").get(0);
		String fname = simImsiJson.optString("firstname");
		String sname = simImsiJson.optString("lastname");
		
		//-----

		String externalId = originalExternalId;
		/************Saving External IDs in RocksDB cache--Start****************/
		try
		{
			ExtIdInfo extIdInfo = new ExtIdInfo();
			extIdInfo.setExtId(externalId);
			extIdInfo.setPrefix("party_");
			extIdInfo.setPrefix("cust_");
			extIdInfo.setPrefix("cont_");
			
			RocksDBRepository rocksDBRepository = RocksDBRepository.getRocksDBRepository();
			rocksDBRepository.save(chunkId+"_"+luwId, extIdInfo);
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		/************Saving External IDs in RocksDB cache--End****************/

		String partyKey = SequenceGenerator.nextCustomSequence("RM_PR", "party1");

		JSONObject validFor = getValidFor(dateTime);

		JSONObject mapping = ResourceHandler.getResourceAsJSON("mapping.json");		

		JSONObject common = mapping.get("common");

		List<JSONArray> resp = contractEntityObj.createContractEntity(externalId,dateTime,common,mapping, currentTime, validFor, account, subscriber, inputJson, chunkId, luwId);

		JSONArray contractRespArray = resp.get(0);

		JSONObject contractRespObj = contractRespArray.get(0);

		if(contractRespObj.has(atrules.ERROR_CODE)){
			output = contractRespObj;
		}
		else{
			output.put("contract", resp.get(0));

			//JSONArray partyEntity = partyEntityObj.createPartyEntity(externalId,partyKey,common,originalExternalId,validFor);
			JSONArray partyEntity = partyEntityObj.createPartyEntity(externalId,partyKey,common,originalExternalId,validFor,fname,sname);

			output.put("party", partyEntity);

			JSONArray customerEntity = customerEntityObj.createCustomerEntity(externalId,common, validFor);

			output.put("customer", customerEntity);

			if(atrules.WORKFLOW_TYPE == atrules.CREATE_UPDATE_BOTH){
			String units = account.getString("units");

			JSONArray billingAccount = billingEntityObj.createBillingAccountEntity(externalId, common,units, currentTime);

			output.put("bABucket", billingAccount);

			if(resp.size()>1){
				JSONArray prodBuc = (JSONArray)resp.get(1);
				output =  (prodBuc!=null && !prodBuc.empty)? output.put("productBucket", resp.get(1)):output;
				}
			}
			ECEV_CS_CreateProviderSubscriberEntity storeInfo = new ECEV_CS_CreateProviderSubscriberEntity();
			storeInfo.storeInformation(inputJson, mapping);
		}
		return output;
	}
	
	private JSONObject populateErrResp(JSONObject errObj){
		JSONObject errorResp = new JSONObject();
		JSONArray jsonArr = new JSONArray();
		errObj.put("level", atrules.ERROR);
		errObj.put("isValid", "false");
		jsonArr.put(errObj);
		errorResp.put("teRuleValdtError", jsonArr);
		
		return errorResp;		
	}

	private JSONObject createErrorResponse(int error, JSONObject account, String luwId, String subscriberStatus, String chunkId, String externalId){
		
		JSONObject errorOutput = new JSONObject();
		switch(error) {
			case atrules.INVALID_STATUS:
				errorOutput.put(atrules.ERROR_CODE,atrules.ERR_INC01);
				errorOutput.put(atrules.ERROR_MSG,atrules.ERR_MSG_INC01);
				errorOutput.put("sub_status", subscriberStatus);
				break;
			case atrules.INVALID_EPOCH:
				errorOutput.put(atrules.ERROR_CODE,atrules.ERR_INC02);
				errorOutput.put(atrules.ERROR_MSG,atrules.ERR_MSG_INC02);
				errorOutput.put("activated",account.optString("activated"));
				break;
			case atrules.INVALID_SFEE_EXPIRY:
				errorOutput.put(atrules.ERROR_CODE,atrules.ERR_INC02);
				errorOutput.put(atrules.ERROR_MSG,atrules.ERR_MSG_INC02);
				errorOutput.put("sfee_expiry",account.optString("sfee_expiry_date"));
				break;
			case atrules.INVALID_SUP_EXPIRY:
				errorOutput.put(atrules.ERROR_CODE,atrules.ERR_INC02);
				errorOutput.put(atrules.ERROR_MSG,atrules.ERR_MSG_INC02);
				errorOutput.put("sup_expiry",account.optString("sup_expiry_date"));
				break;
			case atrules.INVALID_SFEE_STATUS:
				errorOutput.put(atrules.ERROR_CODE,atrules.ERR_INC02);
				errorOutput.put(atrules.ERROR_MSG,atrules.ERR_MSG_INC02);
				errorOutput.put("sfee_status",account.optString("sfee_status"));
				break;
			case atrules.INVALID_SUP_STATUS:
				errorOutput.put(atrules.ERROR_CODE,atrules.ERR_INC02);
				errorOutput.put(atrules.ERROR_MSG,atrules.ERR_MSG_INC02);
				errorOutput.put("sup_status",account.optString("sup_status"));
				break;
			case atrules.SUP_LESS_THAN_ACTIVATED:
				errorOutput.put(atrules.ERROR_CODE,atrules.ERR_INC03);
				errorOutput.put(atrules.ERROR_MSG,atrules.ERR_MSG_INC03);
				errorOutput.put("sup_expiry",account.optString("sup_expiry_date"));
				errorOutput.put("activated",account.optString("activated"));
				break;
			case atrules.SFEE_LESS_THAN_ACTIVATED:
				errorOutput.put(atrules.ERROR_CODE,atrules.ERR_INC04);
				errorOutput.put(atrules.ERROR_MSG,atrules.ERR_MSG_INC04);
				errorOutput.put("sfee_expiry",account.optString("sfee_expiry_date"));
				errorOutput.put("activated",account.optString("activated"));
				break;
			case atrules.SUP_GREATER_THAN_SFEE:
				errorOutput.put(atrules.ERROR_CODE,atrules.ERR_INC05);
				errorOutput.put(atrules.ERROR_MSG,atrules.ERR_MSG_INC05);
				errorOutput.put("sfee_expiry",account.optString("sfee_expiry_date"));
				errorOutput.put("sup_expiry",account.optString("sup_expiry_date"));
				break;
			case atrules.SUB_ALREADY_TERMINATED:
				errorOutput.put(atrules.ERROR_CODE,atrules.ERR_INC10);
				errorOutput.put(atrules.ERROR_MSG,atrules.ERR_MSG_INC10);
				errorOutput.put("sfee_expiry",account.optString("sfee_expiry_date"));
				errorOutput.put("sfee_status",account.optString("sfee_status"));
				break;
		}	
		errorOutput.put("id", externalId);
		printLog(errorOutput,chunkId,luwId);
		return errorOutput;
	}

	private JSONArray populateChannelInfo(){
		JSONArray channelInfo = new JSONArray();
		JSONObject topicInfo = new JSONObject();
		topicInfo.put("topic_name","ERR_TE-GROOVY");
		channelInfo.put(topicInfo);
		return channelInfo;
	}

	private int validateInput(String subscriberStatus,JSONObject account){
		int isValid = atrules.SUCCESS;
		String activated = account.optString("activated");
        if(activated.equals("0")){
			isValid = atrules.AVAILABLE_STATUS;
            return isValid;
        }
		else if(subscriberStatus == null || subscriberStatus.isEmpty()){
			isValid = atrules.INVALID_STATUS;
		}
		else if(activated ==null || activated.isEmpty()){
			isValid = atrules.INVALID_EPOCH;
		}
		else{
			
				isValid = validateLifeCycleDates(account, activated);
			
		}
		return isValid;
	}

	private boolean checkStatus(String sub_status){
		boolean isAvailable = false;
		if(((Integer.parseInt(sub_status)) & 128) == 128){
			isAvailable = true;
		}
		return isAvailable;
	}

	private int validateLifeCycleDates(JSONObject account, String activated){
		int isValid = atrules.SUCCESS;
		String sfeeExpiryDatesEpoch = account.optString("sfee_expiry_date");
		String sfeeStatus = account.optString("sfee_status");
		String supStatus = account.optString("sup_status");
		String supExpiryDateEpoch = account.optString("sup_expiry_date");

		if(sfeeExpiryDatesEpoch==null || sfeeExpiryDatesEpoch.isEmpty()){
			isValid = atrules.INVALID_SFEE_EXPIRY;
		}
		else if(supExpiryDateEpoch==null || supExpiryDateEpoch.isEmpty()){
			isValid = atrules.INVALID_SUP_EXPIRY;
		}
		else if(sfeeStatus==null || sfeeStatus.isEmpty()){
			isValid = atrules.INVALID_SFEE_STATUS;
		}
		else if(supStatus==null || supStatus.isEmpty()){
			isValid = atrules.INVALID_SUP_STATUS;
		}
		else{
			isValid = moreValidationOnDates(activated, sfeeExpiryDatesEpoch,supExpiryDateEpoch,sfeeStatus);
		}

		return isValid;
	}

	private int moreValidationOnDates( String activated, String sfeeExpiryDatesEpoch,String supExpiryDateEpoch, String sfeeStatus){
		int isValid = atrules.SUCCESS;

		long supExpiryDate = Long.parseLong(supExpiryDateEpoch);
		long activatedDate = Long.parseLong(activated);
		long sfeeExpiryDate = Long.parseLong(sfeeExpiryDatesEpoch);
		long sfeeStatusInt = Long.parseLong(sfeeStatus);

		if(supExpiryDate<activatedDate){
			isValid = atrules.SUP_LESS_THAN_ACTIVATED;
		}
		else if(sfeeExpiryDate<activatedDate){
			isValid = atrules.SFEE_LESS_THAN_ACTIVATED;
		}
		else if(supExpiryDate>sfeeExpiryDate){
			isValid = atrules.SUP_GREATER_THAN_SFEE;
		}
		else {
		
		    long disConnectPeriod = sfeeStatusInt & 1023
			 if(disConnectPeriod == 0){
			   disConnectPeriod = 180;
			 }
			long endDateEpochInMillis = (disConnectPeriod + sfeeExpiryDate)*24*60*60*1000;

			long currentTimeinMillis = System.currentTimeMillis();

			if(endDateEpochInMillis<currentTimeinMillis){
				isValid = atrules.SUB_ALREADY_TERMINATED;
			}
		}
		return isValid;
	}

	private String getStartDate(int status, String currentTime, String activated){
		String dateTime = null;
		if(status == atrules.AVAILABLE_STATUS){
			dateTime =  currentTime;
		}
		else{
			dateTime = DateTimeUtil.getDateFormatter(atrules.DATE_FORMATTER).formatInLocalOfEpochInSecond(new Long(activated).longValue()*24*60*60);
		}
		return dateTime;
	}

	private void printLog(JSONObject error, String chunkId, String luwId){
		MDC.clear();
		MDC.put(atrules.TE_ERR_RULE_KEY, atrules.TE_ERR_RULE);

		for(objKey in error.keySet()){
			MDC.put(objKey, error.get(objKey));
		}
		MDC.put(atrules.CHUNKID, chunkId);
		MDC.put(atrules.LUWID, luwId);
		LOGGER.error("");
		MDC.clear();
	}

	private JSONObject getValidFor(String dateTime){
		JSONObject validFor = new JSONObject();
		validFor.put("startDateTime", dateTime);
		return validFor;
	}
}
