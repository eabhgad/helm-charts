package groovy;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.*;
import java.time.format.DateTimeFormatter;

import com.ericsson.datamigration.bss.transformation.utils.DateTimeUtil

/**
 * @author erganaa
 */

public class ECEV_CS_CreateContractEntity{

	private static final Logger LOGGER = LoggerFactory.getLogger(ECEV_CS_CreateContractEntity.class);
	private ECEV_CS_AtRulesConstant atrules = new ECEV_CS_AtRulesConstant();

	private static final String ERROR = "4";

	public List<JSONArray> createContractEntity(String externalId, String dateTime, JSONObject commonMapping, JSONObject mapping, String currentTime, JSONObject validFor, JSONObject account, JSONObject subscriber, JSONObject inputJson, String chunkId, String luwId) {
		List<JSONArray> output = new ArrayList();
		JSONObject contractRecord = new JSONObject();
		JSONArray contractEntity = new JSONArray();

		Map info = getProductInfo(inputJson, mapping, account, currentTime, chunkId, luwId);
		
		if(info.containsKey(ERROR)){
			contractEntity.put(info.get(ERROR));
			output.add(contractEntity);
		}
		else{
			JSONObject simImsiJson = (JSONObject)inputJson.getJSONArray("SimImsi").get(0);

			String msisdn = simImsiJson.getString("msisdn");

			List<JSONArray> response = getProductArray(info,externalId,msisdn,currentTime, validFor,subscriber,inputJson,account,chunkId,luwId,mapping);

			if(!response.empty){
				contractRecord.put("product",response.get(0));
			}
			contractRecord.put(atrules.EXTERNAL_ID, atrules.getExternalId("cont", externalId));
			contractRecord.put("action", "create");

			JSONObject contractSpecification = new JSONObject();

			contractSpecification.put(atrules.EXTERNAL_ID, commonMapping.get("cont_spec"));

			contractRecord.put("contractSpecification", contractSpecification);

			contractRecord.put("paymentContext", "Prepaid");

			JSONArray resources = new JSONArray();

			JSONObject resourceObj1= new JSONObject();

			resourceObj1.put(atrules.EXTERNAL_ID,  atrules.getExternalId("lrs_msisdn", externalId));

			resourceObj1.put("resourceSpecificationExternalId", commonMapping.get("lrs_msisdn"));
			
			String countryCode = commonMapping.get("country_code");
			StringBuilder sb = new StringBuilder(countryCode);
			sb.append(msisdn);

			resourceObj1.put("resourceNumber", sb.toString());

			resourceObj1.put("validFor", validFor);

			JSONObject resourceObj2= new JSONObject();

			resourceObj2.put(atrules.EXTERNAL_ID, atrules.getExternalId("lrs_imsi", externalId));

			resourceObj2.put("resourceSpecificationExternalId", commonMapping.get("lrs_imsi"));

			resourceObj2.put("resourceNumber", simImsiJson.getString("imsi"));

			resourceObj2.put("validFor", validFor);

			resources.put(resourceObj1);

			resources.put(resourceObj2);

			contractRecord.put("resource",resources);

			JSONArray relatedPartyInteractionRole = new JSONArray();

			JSONObject relatedPartyInteractionObj= new JSONObject();

			relatedPartyInteractionObj.put("partyRoleExternalId", atrules.getExternalId("cust", externalId));
			relatedPartyInteractionObj.put("interactionRole", "LegalContractHolder");
			relatedPartyInteractionObj.put("validFor", validFor);

			relatedPartyInteractionRole.put(relatedPartyInteractionObj);
			contractRecord.put("relatedPartyInteractionRole",relatedPartyInteractionRole);

			JSONArray hostTimeZoneArray = new JSONArray();

			JSONObject hostTimeZoneObject = new JSONObject();

			hostTimeZoneObject.put("validFor",validFor);

			hostTimeZoneObject.put("timeZone",commonMapping.get("timezone"));

			hostTimeZoneArray.put(hostTimeZoneObject);

			contractRecord.put("homeTimeZone", hostTimeZoneArray);

			String defaultDisconnectPeriod = commonMapping.get("default_disconnect_period");

			if(defaultDisconnectPeriod==null || defaultDisconnectPeriod.isEmpty()) {
				defaultDisconnectPeriod = 0;
			}

			JSONArray statuses = getStatusArray(dateTime, validFor, account, subscriber, defaultDisconnectPeriod, mapping);

			contractRecord.put("status", statuses);

			contractEntity.put(contractRecord);

			output.add(contractEntity);

			if(response.size()>1){
				output.add(response.get(1));
			}
		}
		return output;
	}

	private Map getProductInfo(JSONObject inputJson, JSONObject mapping, JSONObject account,String dateTime, String chunkId, String luwId){

		HashMap infoMap = new HashMap();
		List<JSONObject> productList = new ArrayList();
		HashMap bundleOfferProdInfo = new HashMap();
		List<JSONObject> addonInfo = new ArrayList();
		String id = account.get("id");

		boolean isBaseOffer = false;

		JSONArray offersInInput = inputJson.optJSONArray("Offer");
		
		JSONObject offersInMapping = mapping.optJSONObject("offer");
		if(offersInInput!=null && !offersInInput.empty){
			for(int k =0;k<offersInInput.length();k++){
				JSONObject offerObj = (JSONObject)offersInInput.get(k);				
				isBaseOffer = getProductInfoForOffersInInput(offerObj, id, luwId, chunkId, bundleOfferProdInfo, offersInMapping, addonInfo, productList, dateTime, isBaseOffer);				
			}
		}
		else{
			LOGGER.debug("*** OFFERS IS MISSING FOR msisdn {} ",id );
		}
		if(!isBaseOffer){
			JSONObject serviceClass = mapping.optJSONObject("service_class");
			String accountClass = account.getString("account_class");
			JSONObject serviceClassObj = serviceClass.optJSONObject(accountClass);//(readContext.read("BODY.Account[0].account_class")));
			if(serviceClassObj!=null){
				JSONObject products = serviceClassObj.optJSONObject("product");
				if(products!=null && !products.empty){
					productList.add(products);
					addonInfo.add(null);
				}
			}
			else{
				// orig_account_class not mapped
				infoMap.put(ERROR, createOriginClassNotMappedError(id,accountClass));
			}
		}
		infoMap.put(1, productList);
		infoMap.put(2, bundleOfferProdInfo);
		infoMap.put(3, addonInfo);
		
		return infoMap;
	}
	
	private boolean getProductInfoForOffersInInput(JSONObject offerObj, String id, String luwId, String chunkId, HashMap bundleOfferProdInfo, JSONObject offersInMapping, List<JSONObject> addonInfo, List<JSONObject> productList, String dateTime, boolean isBaseOffer){
			String offerId = offerObj.optString("offer_id");
			JSONObject addonOffer = new JSONObject();
			boolean isOfferMapped = false;
			String bundleInstId = offerObj.optString("bundle_instance_id");
			String productId = offerObj.optString("product_id");
			String key = generateBundleOfferInfoKey(bundleInstId,offerId,productId);
			bundleOfferProdInfo.put(key, productId);
			String startDate = offerObj.optString("start_date");
			String expDate = offerObj.optString("expiry_date");
			String startSeconds = offerObj.optString("start_seconds");
			String expirySeconds =offerObj.optString("expiry_seconds");
			int validation = validateofferDates(startDate, expDate,startSeconds, expirySeconds);
			if(validation == atrules.SUCCESS){
			for(objKey in offersInMapping.keySet()){
				if(objKey.equals(offerId)){
					isOfferMapped = true;
						JSONObject offerObjInMapping = offersInMapping.get(objKey);
						JSONObject allProducts = offerObjInMapping.optJSONObject("product");

						if (allProducts!=null && !allProducts.empty){
							JSONObject offerDefinition = offerObjInMapping.get("offer_definition");
							JSONObject offerDefinition1 = offerDefinition.get("offer_definition1");
							
							addonOffer.put(atrules.OFFER_START_DATE, startDate);
							addonOffer.put(atrules.OFFER_EXP_DATE, expDate);
							addonOffer.put(atrules.OFFER_START_SEC, startSeconds);
							addonOffer.put(atrules.OFFER_EXP_SEC,expirySeconds);
							addonOffer.put(atrules.OFFER_TYPE, offerDefinition1.get("offer_type"));
							addonOffer.put(atrules.OFFER_DATE, dateTime);
							addonOffer.put(atrules.OFFER_BUNDLE_ID, bundleInstId);
							addonOffer.put(atrules.OFFER_ID, offerId);
							addonOffer.put(atrules.OFFER_PRODUCT_ID, productId);
															
							isBaseOffer = addBaseOfferIfExists(isBaseOffer, addonOffer, offerDefinition1);
							addonInfo.add(addonOffer);
							productList.add(allProducts);
						}
						break;
					}
				}
				if(!isOfferMapped){
					printOfferNotMappedWarning(offerId, id, chunkId, luwId);
				}
			}
			else if(validation == atrules.INVALID_OFFER_DATES ){
				printInvalidOfferDatesError(id,offerId,startDate,expDate, chunkId, luwId);
			}
			else if(validation == atrules.INVALID_START_SECONDS || validation == atrules.INVALID_EXPIRY_SECONDS){
				printInvalidOfferSecondsWarning(id, offerId, startSeconds, expirySeconds, chunkId, luwId);
			}
			return isBaseOffer;
		
	}
	
	private boolean addBaseOfferIfExists(boolean isBaseOffer, JSONObject addonOffer,JSONObject offerDefinition){		
		if(!isBaseOffer && isBaseOfferCheck(offerDefinition)){
			addonOffer.put(atrules.OFFER_BASE_OFFER, true);
			isBaseOffer = true;
		}		
		return isBaseOffer;
	}
	
	private String generateBundleOfferInfoKey(String bundleId,String offerId, String productId){
		StringBuilder sb = new StringBuilder();
		if(bundleId.toUpperCase().equals(atrules.NULL)){
			sb.append(offerId);
			sb.append(atrules.UNDERSCORE);
			sb.append(productId);
		}
		else{
			sb.append(bundleId);
		}
		sb.append(atrules.HASH_SEPARATOR);
		sb.append(offerId);		
		return sb.toString();
	}

	private int validateofferDates(String startDate, String expDate, String startSeconds, String expirySeconds){
		int isValid = atrules.SUCCESS;

		long startDateInt = Long.parseLong(startDate);
		long expDateInt = Long.parseLong(expDate);
		
		if(startDateInt==0 && expDateInt!=0){
			startDateInt = System.currentTimeMillis();			
			expDateInt = getOfferDateTimeInSeconds(expDateInt,expirySeconds);
			startDateInt = startDateInt/1000;
		}

		if(expDateInt!=0 && startDateInt>expDateInt){
			isValid = atrules.INVALID_OFFER_DATES;
		}
		else if(startSeconds==null || startSeconds.empty || startSeconds.equals(atrules.NULL)){
			isValid = atrules.INVALID_START_SECONDS;
		}
		else if(expirySeconds==null || expirySeconds.empty || expirySeconds.equals(atrules.NULL)){
			isValid = atrules.INVALID_EXPIRY_SECONDS;
		}
		return isValid;
	}

	private void printOfferNotMappedWarning(String offerId, String id, String chunkId, String luwId){
		JSONObject error = new JSONObject();
		error.put(atrules.ERROR_CODE, atrules.ERR_INC06);
		error.put("offerId", offerId);
		error.put("id", id);
		error.put(atrules.ERROR_MSG, atrules.ERR_MSG_INC06);
		printLog(error, atrules.WARN_LEVEL, chunkId, luwId);
	}

	private JSONObject createOriginClassNotMappedError(String id, String accountClass){
		JSONObject errorOutput = new JSONObject();
		errorOutput.put(atrules.ERROR_CODE,atrules.ERR_INC07);
		errorOutput.put(atrules.ERROR_MSG,atrules.ERR_MSG_INC07);
		errorOutput.put("id",id);
		errorOutput.put("accntClass",accountClass);
		return errorOutput;
	}

	private void printInvalidOfferDatesError(String id, String offerId, String startDate, String expiryDate, String chunkId, String luwId){
		JSONObject errorOutput = new JSONObject();
		errorOutput.put(atrules.ERROR_CODE,atrules.ERR_INC08);
		errorOutput.put(atrules.ERROR_MSG,atrules.ERR_MSG_INC08);
		errorOutput.put("id",id);
		errorOutput.put("offerId",offerId);
		errorOutput.put("start",startDate);
		errorOutput.put("expiry",expiryDate);
		printLog(errorOutput, atrules.WARN_LEVEL, chunkId, luwId);
	}

	private void printInvalidOfferSecondsWarning(String id, String offerId, String startSeconds, String expirySeconds, String chunkId, String luwId){
		JSONObject error = new JSONObject();

		error.put(atrules.ERROR_CODE, atrules.ERR_INC09);
		error.put("id", id);
		error.put("offerId", offerId);
		error.put("startSec",startSeconds);
		error.put("expirySec",expirySeconds);
		error.put(atrules.ERROR_MSG, atrules.ERR_MSG_INC09);
		printLog(error, atrules.ERROR_LEVEL, chunkId, luwId);
	}

	private boolean isBaseOfferCheck(JSONObject offerDefinition){
		String isBaseOfferInMapping = offerDefinition.get("is_base_offer");
		return Boolean.parseBoolean(isBaseOfferInMapping);
	}
	private List<JSONArray> getProductArray(Map info, String externalId, String msisdn,String currentTime,JSONObject validFor,JSONObject subscriber,JSONObject inputJson, JSONObject account,String chunkId, String luwId,JSONObject mapping){
		List<JSONArray> output = new ArrayList();
		JSONArray productArr=  new JSONArray();
		List<ECEV_CS_BucketInfo> bucketList = new ArrayList();
		List<JSONObject> productList = info.get(1);
		HashMap bundleOfferProdInfo = info.get(2);
		List<JSONObject> addonInfo = info.get(3);
		String extId = account.get("id");
		String accountClass = account.getString("account_class");


		for(int i = 0;i<productList.size();i++){
			JSONObject products = productList.get(i);
			JSONObject offerInfo = addonInfo.get(i);
			for(objKey in products.keySet()){
				JSONObject obj = products.get(objKey);
				if(obj!=null){
					JSONObject jsonObject = new JSONObject();
					String offeringID = obj.optString("po_external_id");
					jsonObject.put("productOfferingExternalId", offeringID);
					String extID = getExtIdForProdAndBuck(offerInfo, offeringID, accountClass);
					jsonObject.put(atrules.EXTERNAL_ID, extID);
					
					JSONObject prodBuckInfo =  getProductStatusArray(validFor,offerInfo,subscriber,obj,account,currentTime);
					JSONArray productStatusArray = prodBuckInfo.getJSONArray("productInformation");	
					JSONObject bucketValidFor = prodBuckInfo.getJSONObject("bucketInformation");
				
					jsonObject.put("status", productStatusArray);

					JSONObject charMapping = mapping.optJSONObject("char");

					addComprisedOf(obj, productStatusArray, offerInfo, accountClass, charMapping, inputJson, jsonObject,mapping);

					addSharingProvider(obj, productStatusArray, externalId, extID, jsonObject);

					addPrice(obj, offerInfo, accountClass, offeringID, jsonObject, mapping, objKey);

					addCharacteristics(obj, charMapping, offeringID, inputJson, jsonObject,mapping);
					productArr.put(jsonObject);
					JSONObject buckets = obj.optJSONObject("bucket");

					if(atrules.WORKFLOW_TYPE == atrules.CREATE_UPDATE_BOTH)
					storeBucketInfo(offerInfo,bundleOfferProdInfo,msisdn, buckets, bucketList,accountClass,bucketValidFor);
				}
			}
		}
		output.add(productArr);
		if(atrules.WORKFLOW_TYPE == atrules.CREATE_UPDATE_BOTH){
		output.add(createProdBucketEntity(inputJson, bucketList,externalId,currentTime,extId,chunkId,luwId));
		}

		return output;
	}
	
	private void addComprisedOf(JSONObject obj,JSONArray productStatusArray,JSONObject offerInfo,String accountClass, JSONObject charMapping, JSONObject inputJson, JSONObject jsonObject,JSONObject mapping){
		JSONObject comprisedOf = obj.optJSONObject("comprised_of");
		if(comprisedOf!=null && !comprisedOf.entrySet().isEmpty()){
			jsonObject.put("comprisedOf", getBundleProductArray(comprisedOf, productStatusArray, offerInfo, accountClass,charMapping,inputJson,mapping));
		}
	}
	
	private void addSharingProvider(JSONObject obj,JSONArray productStatusArray, String externalId, String extID, JSONObject jsonObject){
		String isSharingProvider = obj.optString("is_sharing_provider");
		if(isSharingProvider.equals("true")){
			JSONObject validForObj = productStatusArray.optJSONObject(0).optJSONObject("validFor");
			jsonObject.put("sharingProvider", createSharingProvider(externalId,validForObj));
			jsonObject.put("sharingConsumer", createSharingConsumer(externalId,extID));
		}
	}
	
	private void addPrice(JSONObject obj, JSONObject offerInfo,String accountClass,String offeringID,JSONObject jsonObject, JSONObject mapping, String objKey){
		
		JSONObject lookupMapping = mapping.get("lookup");
		JSONObject offerMapping = mapping.get("offer");
		if(offerInfo!=null) {
			JSONObject productOfferMapping = offerMapping.get(offerInfo.get(atrules.OFFER_ID)).get("product");
			JSONObject prodOfferPriceExtIdMapping = lookupMapping.get("productofferingpriceexternalid").get("mapping");
			String hasPoPrice = productOfferMapping.get(objKey).get("has_price");
			String isPoPrice = obj.optString("po_price_external_id");
			if(hasPoPrice!=null && isPoPrice!=null && !isPoPrice.empty && hasPoPrice == "true"){
				jsonObject.put("price", createPriceArray(isPoPrice,offerInfo,accountClass,offeringID,prodOfferPriceExtIdMapping));
			}
		}
		
	}
	
	private void addCharacteristics(JSONObject obj, JSONObject charMapping,String offeringID, JSONObject inputJson, JSONObject jsonObject, JSONObject mapping){
		boolean hasChar = obj.optBoolean("has_char");
		JSONArray charArray =  addCharacteristicIfExists(charMapping, hasChar, offeringID, inputJson, atrules.PRODUCT_ENTITY,mapping);
		if(charArray!=null && !charArray.empty){
			jsonObject.put("characteristic", charArray);
		}
	}
	
	private JSONArray addCharacteristicIfExists(JSONObject charMapping, boolean hasChar, String id, JSONObject inputJson, String entityType,JSONObject mapping){
		JSONArray charArray = null;
		if(hasChar && charMapping!=null){
			JSONObject charInfoMapping = charMapping.optJSONObject(id);
			if(charInfoMapping!=null){
				JSONObject charMappingForId = charInfoMapping.optJSONObject("char");
				if(charMappingForId!=null){
					charArray = getCharacteristicsFnF(charMappingForId, inputJson,entityType,mapping);
				}
			}
		}
		return charArray;
	}

	private String getExtIdForProdAndBuck(JSONObject addonInfo, String id,  String serviceClass){
		StringBuilder sb = new StringBuilder();
		sb.append(id);
		sb.append(atrules.UNDERSCORE);
		if(addonInfo == null){
			// service Class offer			
			sb.append(atrules.SERVICE_CLASS_IDENTIFIER);
			sb.append(atrules.UNDERSCORE);
			sb.append(serviceClass);
			sb.append(atrules.UNDERSCORE);
			sb.append(atrules.ZERO);
			sb.append(atrules.UNDERSCORE);
			sb.append(atrules.ZERO);
		}
		else{
			sb.append(atrules.OFFER_IDENTIFIER);
			sb.append(atrules.UNDERSCORE);
			sb.append(addonInfo.get(atrules.OFFER_ID));
			sb.append(atrules.UNDERSCORE);
			String bundleId = addonInfo.get(atrules.OFFER_BUNDLE_ID);
			bundleId.toUpperCase().equals(atrules.NULL)?sb.append(atrules.ZERO):sb.append(bundleId);
			sb.append(atrules.UNDERSCORE);
			sb.append(addonInfo.get(atrules.OFFER_PRODUCT_ID));
		}
		return sb.toString();
	}
	
	private String getProductId(HashMap bundleofferProdInfo, String bundleInsId, String offerId ,String productId){
		String value = null;
		String key =  generateBundleOfferInfoKey(bundleInsId, offerId, productId);
		if(bundleofferProdInfo.containsKey(key)){
			value = bundleofferProdInfo.get(key);
		}
		else{
			//COMBINATION NOT PRESENT
			LOGGER.info("***** Combination of bundle id ,offer id ,product id not present ");
		}

		return value;
	}

	 JSONArray createPriceArray(String poPriceExtId, JSONObject offerInfo, String serviceClass, String productPoExtId, JSONObject prodOfferPriceExtIdMapping){
		JSONArray priceArr = new JSONArray();

		Iterator<String> keys = prodOfferPriceExtIdMapping.keys();	
		while(keys.hasNext())
		{
			String key = keys.next();
			if(key.contains(productPoExtId)) {
				JSONObject priceObj = new JSONObject();
				priceObj.put("productOfferingPriceExternalId", prodOfferPriceExtIdMapping.get(key));
				String extId = atrules.getExternalId(poPriceExtId,productPoExtId);
				String externalId = getExtIdForProdAndBuck(offerInfo, extId, serviceClass);
				//priceObj.put("externalId", getExternalId("pop",externalId));
				priceObj.put(atrules.EXTERNAL_ID, externalId);
				priceArr.put(priceObj);
			}
		}
		//System.out.println("PriceArray is: " + priceArr);
		return priceArr;
	}
	
	private JSONArray getCharacteristicsFnF(JSONObject charInfoMapping, JSONObject inputJson, String entityType , JSONObject mapping){
		JSONArray charArr = null;
		JSONObject char1 = charInfoMapping.optJSONObject("char1");
		if(char1!=null && !char1.isEmpty()){			
			String charType = char1.getString("char_type");
			String entityTypeInChar = char1.getString("entity_type");
			if(charType.equals("FNF") && entityTypeInChar.equals(entityType)){
				charArr = new JSONArray();
				String charSpecExtId = char1.getString("char_spec_external_id");
				JSONObject charObj = addCharacteristicsFnF(charSpecExtId,inputJson,mapping);
				if(charObj!=null){
					charArr.put(charObj);
				}

			}
		}
		return charArr;
	}

	private JSONObject addCharacteristicsFnF(String charSpecExtId, JSONObject inputJson,JSONObject mapping){
		JSONObject charObj = null;
		JSONArray subscriberFaf = inputJson.optJSONArray("SubscriberFaf");

		JSONObject commonMapping = mapping.get("common");

		String countryCode = commonMapping.get("country_code");
		


		if(subscriberFaf!=null && !subscriberFaf.isEmpty()){
			charObj = new JSONObject();
			charObj.put("charSpecExternalId", charSpecExtId);
			int size =  subscriberFaf.length();
			JSONArray valueArr = new JSONArray();
			for(int i= 0;i<size; i++){
				JSONObject subFafObj = subscriberFaf.getJSONObject(i);
				JSONObject valObj = new JSONObject();
				StringBuilder sb = new StringBuilder(countryCode);
		        sb.append(subFafObj.optString("called_number"));
				valObj.put("value", sb.toString());
				valueArr.put(valObj);
			}
			charObj.put("value", valueArr);
		}
	}

	private void storeBucketInfo(JSONObject offerInfo,HashMap bundleOfferProdInfo, String msisdn, JSONObject buckets,List<ECEV_CS_BucketInfo> bucketList, String accountClass, JSONObject validFor){
		if(buckets!=null){
			for(objKey in buckets.keySet()){
				JSONObject bucketObj = buckets.get(objKey);
				ECEV_CS_BucketInfo bucket= new ECEV_CS_BucketInfo();
				bucket.setAccount_id(""+bucketObj.opt("source_bucket_id"));
				bucket.setAccount_type(bucketObj.optString("source_bucket_type"));
				bucket.setAction(bucketObj.optString("pb_action"));
				bucket.setDecimal_places(bucketObj.optInt("pb_decimal_places"));
				bucket.setMsisdn(msisdn);
				String pbSpecExtId = bucketObj.optString("pb_spec_external_id");
				bucket.setPb_spec_external_id(pbSpecExtId);
				String productId = "0";
				if(offerInfo == null){
					bucket.setProduct_id(productId);
				}
				else{
					String sourceOfferId = bucketObj.opt("source_offer_id")
					productId = getProductId(bundleOfferProdInfo, offerInfo.get(atrules.OFFER_BUNDLE_ID), sourceOfferId, offerInfo.get(atrules.OFFER_PRODUCT_ID));
					bucket.setProduct_id(productId);
				}
				//bucket.setPo_external_id(getExternalId(bucketObj.optString("pb_po_external_id"),parentProductId));
				String externalId = getExtIdForProdAndBuck(offerInfo, bucketObj.optString("pb_po_external_id"), accountClass);
				bucket.setPo_external_id(externalId);
				bucket.setUom(bucketObj.optString("pb_unit_of_measure"));
				
				bucket.setPb_current_triggerTime(bucketObj.optString("pb_with_current_triggertime"));
				bucket.setPb_needs_validFor(bucketObj.optString("pb_needs_validfor"));
				bucket.setPb_reset_amount(bucketObj.optString("pb_reset_amount"));
				bucket.setValidFor_startDate(validFor.optString("startDateTime"));
				String endDateTime = validFor.optString("endDateTime");
				if(endDateTime!=null){
					bucket.setValidFor_endDate(endDateTime);
				}
				//TODO need to store in db

				bucketList.add(bucket);
			}
		}
	}


	private JSONObject createSharingProvider(externalId, JSONObject validFor){
		JSONObject sharingProvider = new JSONObject();
		JSONArray billingAccount = new JSONArray();
		JSONObject billingAccObj = new JSONObject();
		billingAccObj.put(atrules.EXTERNAL_ID, atrules.getExternalId("ba",externalId));
		billingAccount.put(billingAccObj);
		sharingProvider.put("billingAccount", billingAccount);
		JSONArray consumerList = new JSONArray();
		JSONObject consumerObj = new JSONObject();
		consumerObj.put(atrules.EXTERNAL_ID, atrules.getExternalId("cl",externalId));
		consumerObj.put("consumerCustomerExternalId", atrules.getExternalId("cust",externalId));
		consumerObj.put("consumerContractExternalId", atrules.getExternalId("cont",externalId));
		consumerObj.put("validFor", validFor);

		consumerList.put(consumerObj);
		sharingProvider.put("consumerList", consumerList);
	}

	private JSONArray createProdBucketEntity(JSONObject inputJson, List<ECEV_CS_BucketInfo> bucketMap, String externalId, String currentTime,String originalExtId, String chunkId, String luwId){
		JSONArray productBucAdjEntity = new JSONArray();
		Map daAccount = null;
		Map uaAccount = null;
		for(int i = 0 ;i<bucketMap.size();i++){

			ECEV_CS_BucketInfo bucketInfo = (ECEV_CS_BucketInfo)bucketMap.get(i);

			String accountType = bucketInfo.getAccount_type().toUpperCase();

			if(accountType.equals("UC") || accountType.equals("DA") || accountType.equals("UA")){

				JSONObject productBucketRecord = new JSONObject();

				if(bucketInfo.getPb_current_triggerTime().equals("true")){
					productBucketRecord.put("triggerTime", currentTime);
				}
				else{
					productBucketRecord.put("triggerTime", bucketInfo.getValidFor_startDate());
				}
				productBucketRecord.put("customerExternalId", atrules.getExternalId("cust",externalId));
				productBucketRecord.put("contractExternalId", atrules.getExternalId("cont",externalId));

				productBucketRecord.put("productExternalId", bucketInfo.getPo_external_id());
				productBucketRecord.put("bucketSpecExternalId", bucketInfo.getPb_spec_external_id());

				productBucketRecord.put("unitOfMeasure", bucketInfo.getUom());

				if(bucketInfo.getPb_needs_validFor().equals("true")){
					JSONObject validFor = new JSONObject();
					validFor.put("startDateTime", bucketInfo.getValidFor_startDate());
					String endDateTime = bucketInfo.getValidFor_endDate();
					if(endDateTime!=null){
						validFor.put("endDateTime", endDateTime);
					}
					productBucketRecord.put("validFor",validFor);
				}
				if(accountType.equals("UC")){
					String ucAmt = getUcAmount(inputJson, bucketInfo.getMsisdn(),bucketInfo.getProduct_id(), bucketInfo.getAccount_id());
					if(ucAmt!=null){
						getProductBalAdjSetReq(bucketInfo.getPb_reset_amount(),productBucAdjEntity,productBucketRecord,bucketInfo.getDecimal_places());
						addProductBalAdjReq(bucketInfo.getAction(), productBucAdjEntity, productBucketRecord, bucketInfo.getDecimal_places(), ucAmt);
					}
					else{
						printSourceBucEmptyWarn(originalExtId, accountType, bucketInfo.getProduct_id(), bucketInfo.getAccount_id(), chunkId, luwId);
					}
				}
				else if(accountType.equals("DA")){
					if(daAccount == null){
						daAccount = getDAInformation(inputJson);
					}
					String balance = getDaAmount(daAccount,bucketInfo.getMsisdn(),bucketInfo.getProduct_id(), bucketInfo.getAccount_id());
					if(balance !=null){
						getProductBalAdjSetReq(bucketInfo.getPb_reset_amount(),productBucAdjEntity, productBucketRecord,bucketInfo.getDecimal_places());
						addProductBalAdjReq(bucketInfo.getAction(), productBucAdjEntity, productBucketRecord, bucketInfo.getDecimal_places(), balance);
					}
					else{
						printSourceBucEmptyWarn(originalExtId, accountType, bucketInfo.getProduct_id(), bucketInfo.getAccount_id(),chunkId,luwId);
					}
				}
				else{
					if(uaAccount == null){
						uaAccount = getUAInformation(inputJson);
					}
					String balance = getUaAmount(uaAccount,bucketInfo.getMsisdn(), bucketInfo.getAccount_id());
					if(balance !=null){
						getProductBalAdjSetReq(bucketInfo.getPb_reset_amount(),productBucAdjEntity, productBucketRecord,bucketInfo.getDecimal_places());
						addProductBalAdjReq(bucketInfo.getAction(), productBucAdjEntity, productBucketRecord, bucketInfo.getDecimal_places(), balance);
					}
					else{
						printSourceBucEmptyWarn(originalExtId, accountType, bucketInfo.getProduct_id(), bucketInfo.getAccount_id(), chunkId, luwId);
					}
				}
			}
		}
		return productBucAdjEntity;
	}

	private void getProductBalAdjSetReq(String resetNeeded, JSONArray productBucAdjEntity, JSONObject productAdj, int decimalPlaces){
		if(resetNeeded.equals("true")){
			JSONObject setProdBalance = new JSONObject(productAdj.toString());
			setProdBalance.put("action", "Set");
			JSONObject amountZero = new JSONObject();
			amountZero.put("decimalPlaces", decimalPlaces);
			amountZero.put("number",0);
			setProdBalance.put("amount", amountZero);
			productBucAdjEntity.put(setProdBalance);
		}
	}
	
	private void addProductBalAdjReq(String action, JSONArray productBucAdjEntity, JSONObject productBucketRecord, int decimalPlaces, String balance){
		long balanceInLong = Long.parseLong(balance);
		if(!(action.equals("Relative") && balanceInLong == 0)){
			productBucketRecord.put("action", action);
			JSONObject amount = new JSONObject();
			amount.put("decimalPlaces", decimalPlaces);
			amount.put("number", balanceInLong);
			productBucketRecord.put("amount", amount);
			productBucAdjEntity.put(productBucketRecord);
		}
	}

	private void printSourceBucEmptyWarn(String id, String accountType, String productId, String accountId, String chunkId, String luwId){
		JSONObject error = new JSONObject();

		error.put(atrules.ERROR_CODE, atrules.ERR_INC11);
		error.put("id", id);
		error.put("accntType", accountType);
		error.put("prodId",productId);
		error.put("accId",accountId);
		error.put(atrules.ERROR_MSG, atrules.ERR_MSG_INC11);
		printLog(error, atrules.WARN_LEVEL, chunkId, luwId);
	}

	private HashMap getDAInformation(JSONObject inputJson){
		Map map = null;
		JSONArray daAccounts = inputJson.optJSONArray("DedicatedAccount");

		if(daAccounts!=null && !daAccounts.empty){
			for(int i =0;i<daAccounts.length();i++){
				JSONObject daAccount = (JSONObject)daAccounts.get(i);
				String account_id = daAccount.optString("account_id");
				for (int j=1;j<11;j++){
					String id = daAccount.optString("id_"+j);
					if(id=="0"){
						break;
					}
					else{
						String product_id = daAccount.optString("product_id_"+j);
						String balance = daAccount.optString("balance_"+j);
						map = (map == null)?new HashMap():map;
						StringBuilder key = new StringBuilder();
						key.append(account_id);
						key.append(atrules.HASH_SEPARATOR);
						key.append(id);
						key.append(atrules.HASH_SEPARATOR);
						key.append(product_id);
						map.put(key.toString(), balance);
					}
				}
			}
		}
		else{
			LOGGER.info("*******DA account entity in the input is empty");
		}

		return map;
	}

	private HashMap getUAInformation(JSONObject inputJson){
		Map map = null;
		JSONArray accumulators = inputJson.optJSONArray("Accumulator");

		if(accumulators!=null && !accumulators.empty){
			for(int i =0;i<accumulators.length();i++){
				JSONObject acumulator = (JSONObject)accumulators.get(i);
				String account_id = acumulator.optString("account_id");
				for (int j=1;j<11;j++){
					String id = acumulator.optString("id_"+j);
					if(id=="0"){
						break;
					}
					else{
						String balance = acumulator.optString("value_"+j);

						map = (map == null)?new HashMap():map;
			
						StringBuilder key = new StringBuilder();
						key.append(account_id);
						key.append(atrules.HASH_SEPARATOR);
						key.append(id);
						map.put(key.toString(), balance);
					}
				}
			}
		}
		else{
			LOGGER.info("*******Accumulators account entity in the input is empty");
		}
		return map;
	}

	private String getUcAmount(JSONObject inputJson, String msisdn, String productId, String accountId) throws Exception{
		String amount = null;
			JSONArray usageCounters = inputJson.optJSONArray("UsageCounter");
			if(usageCounters!=null && !usageCounters.empty){
				for(int k =0;k<usageCounters.length();k++){

					JSONObject usageCounter = (JSONObject)usageCounters.get(k);

					if((usageCounter.optString("usage_counter_id").equals(accountId)) && (usageCounter.optString("product_id").equals(productId)) && ((usageCounter.optString("account_id").equals(msisdn)))){
						amount = usageCounter.optString("value");
						break;
					}
				}
			}
    	return amount;
	}


	private String getDaAmount(HashMap daAccounts, String msisdn, String productId, String accountId) throws Exception{
		String balance = null;
		if(daAccounts!=null){
			StringBuilder key = new StringBuilder();			
			key.append(msisdn);
			key.append(atrules.HASH_SEPARATOR);
			key.append(accountId);
			key.append(atrules.HASH_SEPARATOR);
			key.append(productId);

			balance = daAccounts.get(key.toString());


		}
		return balance;
	}
	
	private String getUaAmount(HashMap uaAccounts, String msisdn,String accountId) throws Exception{
		String balance = null;
		if(uaAccounts!=null){
			StringBuilder key = new StringBuilder();
			key.append(msisdn);
			key.append(atrules.HASH_SEPARATOR);
			key.append(accountId);
			balance = uaAccounts.get(key.toString());
		}
		return balance;
	}

	private JSONObject createSharingConsumer(externalId, String offeringId){
		JSONObject consumerObj = new JSONObject();
		consumerObj.put("providerCustomerExternalId", atrules.getExternalId("cust",externalId));
		consumerObj.put("providerContractExternalId", atrules.getExternalId("cont",externalId));
		consumerObj.put("providerProductExternalId", offeringId);
		consumerObj.put("consumerListEntryExternalId", atrules.getExternalId("cl",externalId));
		return consumerObj;
	}


	private JSONArray getStatusArray(String dateTime, JSONObject validFor, JSONObject account, JSONObject subscriber, String defaultDisconnectPeriod, JSONObject mapping){

		JSONArray statusesNew = new JSONArray();

		Object subscriberStatus = subscriber.get("subscriber_status");
		JSONObject lookupMapping = mapping.get("lookup");
		JSONObject contractStatusMapping = lookupMapping.get("statusContract").get("mapping");

		int intSubscriberStatus = ((String)subscriberStatus).toInteger();

		//if ((intSubscriberStatus & 128) == 128) {
		
		String activated = account.optString("activated");
                
        if(activated.equals("0")){
                     
			JSONObject statusNew = new JSONObject();
			statusNew.put("status", contractStatusMapping.get("SDP_Installed_Contract"));
			statusNew.put("validFor", validFor);
			statusesNew.put(statusNew);
		} else {
			Object sfeeExpiryDatesEpoch = account.get("sfee_expiry_date");
			Object sfeeStatus = account.get("sfee_status");

			Object supExpiryDateEpoch = account.get("sup_expiry_date");
			long intSfeeStatus = ((String)sfeeStatus).toLong();

			long activatedTS = new Long(activated).longValue()*24*60*60;
			long supExpiryDateTS = new Long(supExpiryDateEpoch).longValue()*24*60*60;
			long sfeeExpiryDateTS = new Long(sfeeExpiryDatesEpoch).longValue()*24*60*60;

			System.out.println("activatedTS: " + activatedTS + "\n supExpiryDateTS: " + supExpiryDateTS + "\n sfeeExpiryDateTS: " + sfeeExpiryDateTS);

			JSONObject statusNew1 = new JSONObject();
			JSONObject statusNew2 = new JSONObject();
			JSONObject statusNew3 = new JSONObject();
			JSONObject statusNew4 = new JSONObject();
			
			// Make sure that the LCM dates (ACTIVATED, SUP_EXPIRY_DATE and SFEE_EXPIRY_DATE)
      		// are different and that SFEE_EXPIRY_DATE > SUP_EXPIRY_DATE > ACTIVATED if they are equal

			//boolean act_sup = (activatedTS == supExpiryDateTS);
			boolean sup_sfee = (supExpiryDateTS == sfeeExpiryDateTS);

			//if(act_sup) {
				//supExpiryDateTS = supExpiryDateTS + 60;
			//}
			
			if(sup_sfee) {
				sfeeExpiryDateTS = supExpiryDateTS + 24*60*60;
			}	

			supExpiryDateTS =supExpiryDateTS + 24*60*60;
            sfeeExpiryDateTS = sfeeExpiryDateTS + 24*60*60;
			String supExpiryDate = DateTimeUtil.getDateFormatter(atrules.DATE_FORMATTER).formatInLocalOfEpochInSecond(supExpiryDateTS);
			String sfeeExpiryDate = DateTimeUtil.getDateFormatter(atrules.DATE_FORMATTER).formatInLocalOfEpochInSecond(sfeeExpiryDateTS);
			
			long disConnectPeriod = intSfeeStatus & 1023
			 if(disConnectPeriod == 0){
				if(Integer.parseInt(defaultDisconnectPeriod) > 0) {
						disConnectPeriod = Integer.parseInt(defaultDisconnectPeriod);
				} else {
					disConnectPeriod = 1;
				}
			 }
			 long endDateTS = (disConnectPeriod*24*60*60) + sfeeExpiryDateTS;
			//long endDateEpoch = (intSfeeStatus & 1023) + ((String)sfeeExpiryDatesEpoch).toLong();
			String endDate = DateTimeUtil.getDateFormatter(atrules.DATE_FORMATTER).formatInLocalOfEpochInSecond(endDateTS);
			
			statusNew1.put("status", contractStatusMapping.get("SDP_Active_Contract"));
			statusNew1.put("validFor", getValidForWithEndDate(dateTime, supExpiryDate));

			statusNew2.put("status", contractStatusMapping.get("SDP_SupervisionEx_Contract"));
			statusNew2.put("validFor", getValidForWithEndDate(supExpiryDate, sfeeExpiryDate));

			statusNew3.put("status", contractStatusMapping.get("SDP_ServiceEx_Contract"));
			statusNew3.put("validFor", getValidForWithEndDate(sfeeExpiryDate, endDate));

			statusNew4.put("status", contractStatusMapping.get("SDP_Disconnect_Contract"));
			statusNew4.put("validFor", getValidFor(endDate));

			statusesNew.put(statusNew1);
			statusesNew.put(statusNew2);
			statusesNew.put(statusNew3);
			statusesNew.put(statusNew4);
		}

		return statusesNew;
	}

	private JSONObject getProductStatusArray(JSONObject validFor, JSONObject offerInfo, JSONObject subscriber, JSONObject productObjInMapping,JSONObject account, String currentTime){
		JSONObject productInfo = new JSONObject();
		JSONArray statuses = new JSONArray();
		JSONObject validForObj = validFor;

		String subscriberStatus = subscriber.getString("subscriber_status");

		int subscriberStatusInt = Integer.parseInt(subscriberStatus);
		JSONObject status = new JSONObject();

		int currentTimeEpoch = LocalDate.now().toEpochDay();

    // Need to be changed with account.activated == 0
		//if((subscriberStatusInt & 128) == 128){
		String activated = account.optString("activated");
		String supExpiryDate = account.get("sup_expiry_date");

        if(activated.equals("0")){
			status.put("status", "ProductReadyForActivation");
		}
		else {
			status.put("status", "ProductActive");

			if(offerInfo != null){
				boolean isBaseOffer = offerInfo.optBoolean(atrules.OFFER_BASE_OFFER, false);
				validForObj =getOfferValidFor(offerInfo.get(atrules.OFFER_START_DATE),offerInfo.get(atrules.OFFER_EXP_DATE),offerInfo.get(atrules.OFFER_START_SEC),offerInfo.get(atrules.OFFER_EXP_SEC),offerInfo.get(atrules.OFFER_TYPE),offerInfo.get(atrules.OFFER_DATE),isBaseOffer);
			}
		}
		
		String hasReference = productObjInMapping.optString("has_recurrence","false");
		
		String endTimeBucket = validForObj.optString("endDateTime");
		String startTimeBucket = validForObj.optString("startDateTime");
		
		JSONObject bucketTime = new JSONObject();
		bucketTime.put("startDateTime", startTimeBucket);
		
		if(endTimeBucket!=null){
			if(hasReference.equals("true")){
				validForObj.remove("endDateTime");
			}
			bucketTime.put("endDateTime", endTimeBucket);
		}		
		status.put("validFor",validForObj);
		statuses.put(status);
		productInfo.put("productInformation", statuses);
		productInfo.put("bucketInformation", bucketTime);

		return productInfo;
	}

	private JSONArray getBundleProductArray(JSONObject comprisedOf, JSONArray statuses, JSONObject offerInfo, String serviceClass, JSONObject charMapping, JSONObject inputJson,JSONObject mapping){
		JSONArray comprisedArray = new JSONArray();
		for(objKey in comprisedOf.keySet()){

			JSONObject comprisedObj = comprisedOf.get(objKey);

			JSONObject childProduct = new JSONObject();

			childProduct.put("status", statuses);

			String po_external_id = comprisedObj.optString("po_external_id");

			childProduct.put("productOfferingExternalId", po_external_id);

			//childProduct.put("externalId", getExternalId(po_external_id,productId));

			childProduct.put(atrules.EXTERNAL_ID, getExtIdForProdAndBuck(offerInfo, po_external_id, serviceClass));

			boolean hasChar = comprisedObj.optBoolean("has_char",false);

			JSONArray charArray =  addCharacteristicIfExists(charMapping, hasChar, po_external_id, inputJson, atrules.COMPRISED_OFF_ENTITY,mapping);
			if(charArray!=null && !charArray.empty){
				childProduct.put("characteristic", charArray);
			}
			comprisedArray.put(childProduct);
		}

		return comprisedArray;
	}
	private JSONObject getValidFor(String dateTime){
		JSONObject validFor = new JSONObject();
		validFor.put("startDateTime", dateTime);
		return validFor;
	}

	private JSONObject getValidForWithEndDate(String startDateTime, String endDateTime){
		JSONObject validFor = new JSONObject();
		validFor.put("startDateTime", startDateTime);
		validFor.put("endDateTime", endDateTime);
		return validFor;
	}

	private String getOfferDate(long offerDate, String inputSeconds){
		long timeInSeconds = getOfferDateTimeInSeconds(offerDate,inputSeconds);		
		return DateTimeUtil.getDateFormatter(atrules.DATE_FORMATTER).formatInLocalOfEpochInSecond(timeInSeconds);
	}
	
	private long getOfferDateTimeInSeconds(long offerDate, String inputSeconds){
		long timeInSeconds = 0;
		if(inputSeconds == "NULL" ){
			timeInSeconds = offerDate*60*60*24;
		}
		else{
			long tarSec = 0;
			long seconds = Long.parseLong(inputSeconds);
			if(seconds <0){
				tarSec= seconds + 65535;
				timeInSeconds=(offerDate*65536)+tarSec;
			}
			else{
				tarSec= seconds;
				timeInSeconds=(offerDate*65536)+tarSec;
			}
		}
		return timeInSeconds;
	}

	private JSONObject getOfferValidFor(String startDate, String expiryDate, String startSeconds,  String expirySeconds, String offerType, String currentTime, boolean isBaseOffer){
		String startDateTime = null;
		String endDateTime = null;
		long inputStartDate = Long.parseLong(startDate);
		long inputExpiryDate = Long.parseLong(expiryDate);
		if(offerType.equals("account")){
			if(inputStartDate==0){
				StringBuilder currentDate = new StringBuilder(DateTimeUtil.getDateFormatter("yyyy-MM-dd").getFormattedUTCTime());
				currentDate.append("T00:00:00.000Z");
				startDateTime = currentDate.toString();
			}
			else{
				startDateTime = getOfferDate(inputStartDate, "NULL");
			}
			if(inputExpiryDate != 0 && !isBaseOffer){
				endDateTime = getOfferDate(inputExpiryDate, "NULL");
			}
		}
		else if(offerType.equals("timer")){
			if(inputStartDate==0){
				startDateTime = currentTime;
			}
			else{
				startDateTime = getOfferDate(inputStartDate, startSeconds);
			}
			if(inputExpiryDate != 0  && !isBaseOffer){
				endDateTime = getOfferDate(inputExpiryDate, expirySeconds);
			}
		}
		else{
			LOGGER.error("ERROR: offer Type is not valid in the mapping file");
		}

		JSONObject validFor = new JSONObject();
		validFor.put("startDateTime", startDateTime);
		if(endDateTime!=null){
			validFor.put("endDateTime", endDateTime);
		}
		return validFor;
	}

	private void printLog(JSONObject error, int logLevel, String chunkId, String luwId){
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
}