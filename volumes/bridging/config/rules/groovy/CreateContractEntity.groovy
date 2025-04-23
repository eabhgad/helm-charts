package groovy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;

import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ericsson.datamigration.log.utils.LogUtil;

public class CreateContractEntity{

	private static final Logger logger = LoggerFactory.getLogger(CreateContractEntity.class);

	public List<JSONArray> createContractEntity(JSONArray contractArrIn, JSONArray billingAccountArrIn, JSONObject partyInterRole, JSONArray resources, JSONArray contactMediumAssoArr, JSONArray products, JSONArray prodStatHistArr, JSONArray contractCharacArr, JSONArray contStatHistArr, JSONObject mapping,JSONArray priceArr) {
		List<JSONArray> output = new ArrayList();
		JSONArray contractArr = new JSONArray();
		JSONObject contractSpecMapping = mapping.get("lookup").get("contractSpec");
		JSONObject commonMapping = mapping.get("common");
		JSONObject lookupMapping = mapping.get("lookup");

		for(int indx=0;indx<contractArrIn.length();indx++) {
			JSONObject contractRecord = new JSONObject();
			JSONObject contract = contractArrIn.getJSONObject(indx);


			// BillingAccounrRefrence is added B2B and B2C
			JSONObject billingAccountReference1 = new JSONObject();
			billingAccountReference1.put("externalId",(products.getJSONObject(0)).get("billinmgaccountref"));
			contractRecord.put("billingAccountReference",billingAccountReference1);
			contractRecord.put("externalId", contract.get("contractexternalid"));
			contractRecord.put("paymentContext", getFromMapping(mapping.get("lookup").get("paymentContext"),contract,"paymentcontext"));

			//-------------contractSpecification Start-----------------//
			JSONObject contractSpecification = new JSONObject();
			//contractSpecification.put("externalId", contract.get("contractspecexternalid"));

			// contractSpecification externalId logic changed from copt to read it from the mapping file
			contractSpecification.put("externalId",getFromMapping(contractSpecMapping, contract, "contractspecexternalid"));
			contractRecord.put("contractSpecification",contractSpecification);


			//-------------contractSpecification End-----------------//


			//-------------homeTimeZone Start-----------------//
			JSONObject homeTimeZone = new JSONObject();
			homeTimeZone.put("timeZone",contract.get("timezone"));
			JSONObject validFor = getValidFor(getFormattedDate(contract.get("statusvalidfrom")));
			homeTimeZone.put("validFor",validFor);
			JSONArray homeTimeZoneArr = new JSONArray();
			homeTimeZoneArr.put(homeTimeZone);
			contractRecord.put("homeTimeZone",homeTimeZoneArr);
			//-------------homeTimeZone End-----------------//


			//-------------taxExemption Start-----------------//
			if(contract.has("tax_type")){
			JSONArray taxExemptionArr = new JSONArray();
			JSONObject taxExemptionEle = new JSONObject();
			JSONObject taxExemptionSpecification = new JSONObject();
			//taxExemptionSpecification.put("externalId","TAX_Exe_Spec_MIG");
			taxExemptionSpecification.put("externalId",getFromMapping(mapping.get("lookup").get("taxExemptionSpecExtId"),contract,"tax_type"));
			taxExemptionEle.put("taxExemptionSpecification",taxExemptionSpecification);

			validFor = getValidFor(getFormattedDate(contract.get("statusvalidfrom")));
			if((!contract.optString("statusvalidto").equals("")))
			{
				validFor.put("endDateTime", getFormattedDate(contract.get("statusvalidto")));
			}
			taxExemptionEle.put("validFor",validFor);

			taxExemptionArr.put(taxExemptionEle);
			contractRecord.put("taxExemption",taxExemptionArr);
			}
			//-------------taxExemption End-----------------//


			//-------------Customer Status Start---------------//
			validFor = getValidFor(getFormattedDate(contract.get("statusvalidfrom")));
			JSONObject status = new JSONObject();
			//status.put("status","Activated");
			status.put("status",getFromMapping(mapping.get("lookup").get("statusContract"),contract,"status"));

			status.put("validFor",validFor);
			JSONArray statusArr = new JSONArray();
			statusArr.put(status);
			contractRecord.put("status",statusArr);
			//-------------Customer Status End----------------//


			//-------------resources Start---------------//
			JSONArray resourceArr = new JSONArray();

			for(int i=0;i<resources.length();i++)
			{
				JSONObject resourceSource = resources.getJSONObject(i);

				if(contract.optString("contractexternalid").equals(resourceSource.optString("contractexternalid")))
				{

					JSONObject resourceTarget = new JSONObject();
					resourceTarget.put("externalId","resource-extId-"+resourceSource.optString("resourcenumber"));

					resourceTarget.put("resourceNumber",resourceSource.get("resourcenumber"));
					//resourceTarget.put("resourceSpecificationExternalId",resourceSource.get("resourcespecificationexternalid"));
					resourceTarget.put("resourceSpecificationExternalId",getFromMapping(mapping.get("lookup").get("resourceSpecificationExternalId"),resourceSource,"resourcespecificationexternalid"));

					//validFor = getValidFor(getFormattedDate(resourceSource.get("startdatetime")));
					//validFor = getValidFor(getFormattedDate(getCurrentFormattedDate())); //Temporary solution

					if(!resourceSource.optString("startdatetime").equals(""))
					{
						validFor = new JSONObject();
						validFor.put("startDateTime", getFormattedDate(resourceSource.get("startdatetime")));
						if(!resourceSource.optString("enddatetime").equals(""))
						{
							validFor.put("endDateTime", getFormattedDate(resourceSource.get("enddatetime")));
						}
						resourceTarget.put("validFor",validFor);
					}

					/*status = new JSONObject();
					status.put("status","ResourceActive");
					status.put("validFor",validFor);
					statusArr = new JSONArray();
					statusArr.put(status);
					resourceTarget.put("status",statusArr);*/
					resourceArr.put(resourceTarget);
				}
			}
			contractRecord.put("resource",resourceArr);
			//-------------resources end---------------//

			//-------------Adding Contact Medium Association---------------//
			JSONArray contactMedAsso = getContactMediumAssoArray(contactMediumAssoArr, commonMapping, lookupMapping, contract);
			if (contactMedAsso.length() > 0) {
				contractRecord.put("contactMediumAssociation", contactMedAsso);
			}

			//-------------products Start---------------//
			JSONArray productArrayBPO = new JSONArray();
			JSONArray productArr = new JSONArray();
			Set<String> serPoSet = new HashSet<>();

			for(int i=0;i<products.length();i++)
			{
				JSONObject productSource = products.getJSONObject(i);
				//LogUtil.logDebug(logger, MessageFormat.format("products = {0}", products));
				if(contract.optString("contractexternalid").equals(productSource.optString("contractexternalid")))
				{
					JSONObject productTarget = new JSONObject();

					JSONObject billingAccountReference = new JSONObject();
					JSONObject baRefForBillCycleAlignedRecurrence = new JSONObject();

					productTarget.put("externalId",productSource.get("productexternalid"));
					//productTarget.put("productOfferingExternalId","Fusion_Mobile");
					//productTarget.put("productOfferingExternalId",productSource.get("productofferingexternalid"));
					productTarget.put("productOfferingExternalId",getProductMapping(mapping.get("products"),productSource,"productofferingexternalid"));

					validFor = getValidFor(getFormattedDate(productSource.get("statusvalidfrom")));
					status = new JSONObject();
					//status.put("status","ProductActive");
					//status.put("status",productSource.get("status"));
					status.put("status","ProductActive");

					status.put("validFor",validFor);
					statusArr = new JSONArray();
					statusArr.put(status);

					//-------------Product Status History Start---------------//
					for(int j = 0; j < prodStatHistArr.length(); j++)
					{
						JSONObject prodStatHist = prodStatHistArr.getJSONObject(j);
						if(productSource.optString("contractexternalid").equals(prodStatHist.optString("contractexternalid")) && productSource.optString("productofferingexternalid").equals(prodStatHist.optString("productofferingexternalid")) && productSource.optString("productexternalid").equals(prodStatHist.optString("productexternalid")) && productSource.optString("productname").equals(prodStatHist.optString("productname")) && mapping.get("products").get(productSource.optString("productofferingexternalid")).get("is_status_hist_req").equalsIgnoreCase("true"))
						{
							status = new JSONObject();
							if(!prodStatHist.optString("status").equals(""))
							{
								status.put("status", mapping.get("lookup").get("statusProduct").get("mapping").get(prodStatHist.get("status")));
							}
							if(!prodStatHist.optString("statusvalidfrom").equals(""))
							{
								validFor = new JSONObject();
								validFor.put("startDateTime", getFormattedDate(prodStatHist.get("statusvalidfrom")));
								if(!prodStatHist.optString("statusvalidto").equals(""))
								{
									validFor.put("endDateTime", getFormattedDate(prodStatHist.get("statusvalidto")));
								}
								status.put("validFor", validFor);
							}
							statusArr.put(status);
						}
					}
					//-------------Product Status History End---------------//

					productTarget.put("status",statusArr);

					billingAccountReference.put("externalId",productSource.get("billinmgaccountref"));
					productTarget.put("billingAccountReference",billingAccountReference);

					//baRefForBillCycleAlignedRecurrence.put("externalId",productSource.get("billinmgaccountref"));
					//productTarget.put("baRefForBillCycleAlignedRecurrence",baRefForBillCycleAlignedRecurrence);
					
					JSONObject productMap = new JSONObject();
					productMap = mapping.get("products").get(productSource.optString("productofferingexternalid"));
                    
					if(productMap.optString("is_bill_cycle_aligned").equals("true") && productMap.optString("has_recurrence").equals("true")){
                        baRefForBillCycleAlignedRecurrence.put("externalId",productSource.get("billinmgaccountref"));
					    productTarget.put("baRefForBillCycleAlignedRecurrence",baRefForBillCycleAlignedRecurrence);
					}
					
                    


					if(productMap.optString("has_price").equals("true")){
                                          JSONArray priceEntity = new JSONArray();
					
					
					String productExtId = productSource.optString("productexternalid");

					for(int t=0 ;t<priceArr.length() ;t++){
						JSONObject priceTableObj = priceArr.getJSONObject(t);
						JSONObject priceObj = new JSONObject();
						if(priceTableObj.optString("productexternalid").equals(productExtId)){
							
							priceObj.put("productOfferingPriceExternalId",mapping.get("lookup").get("productofferingpriceexternalid").get("mapping").get(priceTableObj.optString("productofferingpriceexternalid")));
							JSONObject validForPrice = new JSONObject();
								validForPrice.put("startDateTime", getFormattedDate(priceTableObj.get("startdatetime")));
								if(priceTableObj.has("enddatetime") && priceTableObj.get("enddatetime") != null)
								{
									validForPrice.put("endDateTime", getFormattedDate(priceTableObj.get("enddatetime")));
								}
							priceObj.put("validFor",validForPrice);

							priceEntity.put(priceObj);
						}
					}
					    productTarget.put("price",priceEntity);
					}

					//LogUtil.logDebug(logger, MessageFormat.format("productTarget = {0}", productTarget));

					productArr.put(productTarget);

					//Adding technicalPO with respect to productSource
					if(mapping.get("products").get(productSource.get("productofferingexternalid")).has("technical_po")) { 
						String technicalPO = mapping.get("products").get(productSource.get("productofferingexternalid")).get("technical_po");
						if(!serPoSet.contains(technicalPO)) {
							productArrayBPO.put(addTechnicalProducts(mapping, contract, technicalPO));
							serPoSet.add(technicalPO);
						}
					}

					//Adding serviceabilityPO with respect to ProductSource
					JSONObject productSerPO = new JSONObject();
					if(mapping.get("products").get(productSource.get("productofferingexternalid")).has("serviceability_po")) {
						String serviceabilityPO = mapping.get("products").get(productSource.get("productofferingexternalid")).get("serviceability_po");
						if(!serPoSet.contains(serviceabilityPO)) {
							productSerPO = addServiceabilityPO(mapping,contract,serviceabilityPO);
							if(productSerPO.length() != 0) {
								productArrayBPO.put(productSerPO);
							}	
							serPoSet.add(serviceabilityPO);
						}
					}		
				}
			}
			LogUtil.logDebug(logger, MessageFormat.format("productArr = {0}", productArr));

			//Previous scenario: Loop over each bundle-> then each product -> then compare with each child of bundle -> Gets stuck at adding non-bundle POs
			//Current scenario: Loop over product array-> loop over each bundle -> compare each child -> if all childs exist, add to bundleArr, add to productsArr

			Map productsToBundleMap = new HashMap();
			JSONObject bundleProducts = mapping.get("bundle_products");
			Iterator<String> bundlePOKeys = bundleProducts.keys();

			while(bundlePOKeys.hasNext()) {
				String bundleName = bundlePOKeys.next();
				//LogUtil.logDebug(logger, MessageFormat.format(" bundleData = {0}", bundleProducts.get(bundleName).get("Child")));
				JSONArray productsInBundle = bundleProducts.get(bundleName).get("Child");

				productsInBundle.each { product ->
					productsToBundleMap.put(product,bundleName);
				}
				//LogUtil.logDebug(logger, MessageFormat.format(" productToBundleMap = {0}", productsToBundleMap));
			}

			Map<String, List<JSONObject>> organizedBundles = [:].withDefault { [] };

			for(int prodInd=0;prodInd<productArr.length();prodInd++) {
				Iterator<String> mapPOKeys = mapping.get("products").keys();
				while(mapPOKeys.hasNext()) {
					String key = mapPOKeys.next();
					if(mapping.get("products").get(key).get("external_id").equalsIgnoreCase(productArr.getJSONObject(prodInd).get("productOfferingExternalId"))) {
						if(mapping.get("products").get(key).has("external_ID_Bundle_PO")) {
							//LogUtil.logDebug(logger, MessageFormat.format("Is part of bundle = {0}", mapping.get("products").get(key).get("external_ID_Bundle_PO")));
							String bundleName = mapping.get("products").get(key).get("external_ID_Bundle_PO");
							if(productsToBundleMap.containsKey(mapping.get("products").get(key).get("external_id"))) {
								organizedBundles[productsToBundleMap.get(mapping.get("products").get(key).get("external_id"))] << productArr.getJSONObject(prodInd);
								//LogUtil.logDebug(logger, MessageFormat.format("organizedBundles = {0}", organizedBundles));
							}
						} else {
							productArrayBPO.put(productArr.get(prodInd));
							//LogUtil.logDebug(logger, MessageFormat.format("Is not part of any bundle = {0}", productArrayBPO));
						}
					}
				}
			}
			LogUtil.logDebug(logger, MessageFormat.format("organizedBundles = {0}", organizedBundles.toString()));

			organizedBundles.each { String bundle, List<JSONObject> productsInOrgBundle ->
				if(productsInOrgBundle.size().equals(mapping.get("bundle_products").get(bundle).get("Child").length())) {
					//LogUtil.logDebug(logger, MessageFormat.format("BundlePO can be formed = {0}", organizedBundles.toString()));
					JSONObject bundlePO = new JSONObject();
					//Creating the Bundle
					bundlePO.put("productOfferingExternalId",bundle);
					bundlePO.put("externalId",generateExtId("bpo",contract.get("luw_id"),bundle));

					//fetching startDateTime of first product of the bundle and attaching the same to bundle as status
					validFor = getValidFor(productsInOrgBundle.get(0).get("status").get(0).get("validFor").get("startDateTime"));
					status = new JSONObject();
					status.put("status","ProductActive");
					status.put("validFor",validFor);
					statusArr = new JSONArray();
					statusArr.put(status);
					bundlePO.put("status",statusArr);

					JSONArray bundlePriceArr = mapping.get("bundle_products").get(bundle).get("price");
					JSONArray bundlePriceArrOut = new JSONArray();
					for(int i=0;i<bundlePriceArr.length();i++) {
						if(bundlePriceArr.get(i) != null && !bundlePriceArr.get(i).trim().isEmpty()) {
							JSONObject bundlePrice = new JSONObject();
							bundlePrice.put("productOfferingPriceExternalId",bundlePriceArr.get(i));
							bundlePrice.put("validFor",validFor);
							bundlePriceArrOut.put(bundlePrice);
						}
					}
					bundlePO.put("price",bundlePriceArrOut);

					//JSONObject baRefForBillCycleAlignedRec = new JSONObject();
					//String baRefExternalId = productsInOrgBundle.get(0).get("baRefForBillCycleAlignedRecurrence").get("externalId");
					//baRefForBillCycleAlignedRec.put("externalId",baRefExternalId);
					//bundlePO.put("baRefForBillCycleAlignedRecurrence",baRefForBillCycleAlignedRec);

					JSONObject billingAccountRef = new JSONObject();
					String billingAccountRefExternalId = productsInOrgBundle.get(0).get("billingAccountReference").get("externalId");
					billingAccountRef.put("externalId",billingAccountRefExternalId);
					bundlePO.put("billingAccountReference",billingAccountRef);

					//comprisedOf - Adding individual POs to Bundle
					JSONArray comprisedOf = new JSONArray();
					for(JSONObject prodInOrgBundle : productsInOrgBundle) {
						prodInOrgBundle.remove("baRefForBillCycleAlignedRecurrence");
						comprisedOf.put(prodInOrgBundle);
					}
					bundlePO.put("comprisedOf",comprisedOf);
					LogUtil.logDebug(logger, MessageFormat.format("BundlePO formed = {0}", bundlePO));
					productArrayBPO.put(bundlePO);
				} else {
					LogUtil.logDebug(logger, MessageFormat.format("BundlePO cannot be formed = {0}", productsInOrgBundle));
					for(JSONObject prodInOrgBundle : productsInOrgBundle) {
						Iterator<String> mapPOKeys = mapping.get("products").keys();
						while(mapPOKeys.hasNext()) {
							String key = mapPOKeys.next();
							if(prodInOrgBundle.get("productOfferingExternalId").equalsIgnoreCase(mapping.get("products").get(key).get("external_id"))) {
								String indProvision = mapping.get("products").get(key).get("individual_provisioned");
								if(indProvision.equalsIgnoreCase("Y")) {
									productArrayBPO.put(prodInOrgBundle);
									LogUtil.logDebug(logger, MessageFormat.format("productArrayBPO if indProv is Y = {0}", productArrayBPO));
								}
							}
						}
					}
				}
			}

			contractRecord.put("product",productArrayBPO);

			//-------------products End---------------//


			//-------------relatedPartyInteractionRole Start---------------//
			JSONObject relatedPartyInteractionRole = new JSONObject();
			relatedPartyInteractionRole.put("partyRoleExternalId",partyInterRole.get("partyroleexternalid"));
			relatedPartyInteractionRole.put("interactionRole",partyInterRole.get("interactionrole"));
			validFor = getValidFor(getFormattedDate(partyInterRole.get("validfrom")));
			relatedPartyInteractionRole.put("validFor",validFor);
			JSONArray relatedPartyInteractionRoleArr = new JSONArray();
			relatedPartyInteractionRoleArr.put(relatedPartyInteractionRole);
			contractRecord.put("relatedPartyInteractionRole",relatedPartyInteractionRoleArr);
			//-------------relatedPartyInteractionRole End----------------//


			//-------------characteristic Start---------------//
			JSONArray characteristicArr = new JSONArray();
			for(int i=0;i<contractCharacArr.length();i++)
			{
				JSONObject contractCharac = contractCharacArr.getJSONObject(i);

				if(contract.optString("contractexternalid").equals(contractCharac.optString("contractexternalid")))
				{
					JSONObject characteristic = new JSONObject();
					characteristic.put("charSpecExternalId",contractCharac.get("characteristicspecexternalid"));
					//validFor = getValidFor(getFormattedDate(partyInterRole.get("validfrom")));

					JSONObject value = new JSONObject();
					JSONArray valueArr = new JSONArray();

					if((!contractCharac.optString("startdatetime").equals("")))
					{
						validFor = new JSONObject();
						validFor.put("startDateTime", getFormattedDate(contractCharac.get("startdatetime")));
						if((!contractCharac.optString("enddatetime").equals("")))
						{
							validFor.put("endDateTime", getFormattedDate(contractCharac.get("enddatetime")));
						}
						characteristic.put("validFor",validFor);
						value.put("value", contractCharac.get("value"));

						if((!contractCharac.optString("unitofmeasure").equals("")))
						{
							value.put("unitOfMeasure", contractCharac.get("unitofmeasure"));
						}

						valueArr.put(value);
						characteristic.put("value",valueArr);
						characteristicArr.put(characteristic);
					}
				}

			}
			contractRecord.put("characteristic",characteristicArr);
			//-------------characteristic End----------------//


			//-------------Status Change History Start---------------//
			statusArr = contractRecord.getJSONArray("status");
			for(int i = 0; i < contStatHistArr.length(); i++)
			{
				JSONObject contStatHist = contStatHistArr.getJSONObject(i);
				if(contract.optString("contractexternalid").equals(contStatHist.optString("contractexternalid")))
				{
					status = new JSONObject();
					if(!contStatHist.optString("status").equals(""))
					{
						status.put("status", mapping.get("lookup").get("statusContract").get("mapping").get(contStatHist.get("status")));
					}
					/*if(!contStatHist.optString("statusreason").equals(""))
					{
						status.put("reason", mapping.get("lookup").get("statusReasonContract").get("mapping").get(contStatHist.optString("statusreason")));
					}*/
					if(!contStatHist.optString("statusvalidfrom").equals(""))
					{
						validFor = new JSONObject();
						validFor.put("startDateTime", getFormattedDate(contStatHist.get("statusvalidfrom")));
						if(!contStatHist.optString("statusvalidto").equals(""))
						{
							validFor.put("endDateTime", getFormattedDate(contStatHist.get("statusvalidto")));
						}
						status.put("validFor", validFor);
					}
					statusArr.put(status);
				}
				contractRecord.put("status", statusArr);
			}
			//-------------Status Change History End----------------//

			contractRecord.put("action", "create");

			contractArr.put(contractRecord);
		}

		LogUtil.logDebug(logger, MessageFormat.format("Contract Array = {0}", contractArr.toString()));

		output.add(contractArr);
		return output;
	}

	private JSONArray getContactMediumAssoArray(JSONArray contactMediumAssociationArr, JSONObject commonMapping, JSONObject lookupMapping, JSONObject cont) {
		JSONArray cMediumArr = new JSONArray();
		JSONObject contactRole_Med_ASSMapping = lookupMapping.get("contactRole_Med_ASS");
		JSONObject language_Med_ASSMapping = lookupMapping.get("language_Med_ASS");
		JSONObject enabled_Med_ASSMapping = lookupMapping.get("Enabled_Med_ASS");


		for (int i = 0; i < contactMediumAssociationArr.length(); i++) {
			JSONObject contactMediumAssociation = contactMediumAssociationArr.getJSONObject(i);
			
			if((contactMediumAssociation.optString("associatedentitytype").equals("Contract")) &&
			contactMediumAssociation.optString("associatedentityexternalid").equals(cont.optString("contractexternalid"))) {

				JSONObject cMediumAsso = null;
				JSONObject validFor = null;

				validFor = getValidFor(getFormattedDate(contactMediumAssociation.get("startdatetime")))

				String contactRole = getFromMapping(contactRole_Med_ASSMapping, contactMediumAssociation, "contactrole");
				String contactMediumExternalId = contactMediumAssociation.optString("contactmediumexternalid");
				String language = getFromMapping(language_Med_ASSMapping, contactMediumAssociation, "language");
				String enabled = getFromMapping(enabled_Med_ASSMapping, contactMediumAssociation, "enabled");
				boolean enabledBoolean = Boolean.parseBoolean(enabled);

				cMediumAsso = new JSONObject();
				cMediumAsso.put("contactRole", contactRole);
				cMediumAsso.put("contactMediumExternalId", contactMediumExternalId);
				cMediumAsso.put("language", language);
				cMediumAsso.put("enabled", enabledBoolean);
				cMediumAsso.put("validFor", validFor);

				cMediumArr.put(cMediumAsso);
			}
		}
		return cMediumArr;

	}

	private String getFromMapping(JSONObject mapping, JSONObject sourceJson, String fieldName)
	{
		String object = null;
		for(String objKey : mapping.get("mapping").keySet())
		{
			object = mapping.get("mapping").get(objKey);
			if(objKey.equals(sourceJson.optString(fieldName)))
			{
				break;
			}
		}
		return object;
	}

	private String getProductMapping(JSONObject productmapping, JSONObject sourceJson, String fieldName)
	{
		String externalId = null;
		for(String objKey : productmapping.keySet())
		{
			// Validation parameters will be added in here
			externalId = productmapping.get(objKey).get("external_id");
			if(objKey.equals(sourceJson.optString(fieldName)))
			{
				break;
			}
		}
		return externalId;
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

	private String getCurrentFormattedDate()
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date dateObj = new Date();
		String dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(dateObj);

		return dateStr;

	}

	private JSONObject addTechnicalProducts(JSONObject mapping, JSONObject contract, String technicalPO)
	{
		JSONObject productTarget = new JSONObject();

		if(mapping.get("default_products").has(technicalPO) 
			&& mapping.get("default_products").get(technicalPO).get("is_technical").equals("true")) {
				JSONObject sharingProvider = new JSONObject();
				JSONObject sharingConsumer = new JSONObject();

				JSONObject currentProd = mapping.get("default_products").get(technicalPO);

				productTarget.put("productOfferingExternalId",currentProd.get("external_id"));
				productTarget.put("externalId",generateExtId("tpo",contract.get("luw_id"),currentProd.get("external_id")));

				//--------------add status start---------------//
				JSONObject validFor = new JSONObject();
				validFor.put("startDateTime",getFormattedDate(contract.get("statusvalidfrom")));
				if(contract.has("statusvalidto")) {
					if(!contract.get("statusvalidto").equals("")) {
						validFor.put("endDateTime",getFormattedDate(contract.get("statusvalidto")));
					}
				}

				//lookup("statusProduct",status)

				JSONObject status = new JSONObject();
				status.put("reason","PRA");
				status.put("status","ProductActive");
				status.put("validFor",validFor);
				JSONArray statusArr = new JSONArray();
				statusArr.put(status);
				productTarget.put("status",statusArr);
				//--------------add status end---------------//

				//--------------sharing provider start---------------//
				JSONObject billingAccountObj = new JSONObject();
				JSONArray billingAccountArr = new JSONArray();

				billingAccountObj.put("externalId",contract.get("billingaccountref"));
				billingAccountArr.put(billingAccountObj);
				sharingProvider.put("billingAccount",billingAccountArr);

				JSONArray consumerListArr = new JSONArray();
				JSONObject consumerListObj = new JSONObject();

				consumerListObj.put("externalId",generateExtId("cl",contract.get("luw_id"),currentProd.get("external_id")));
				consumerListObj.put("consumerCustomerExternalId",contract.get("customerexternalid"));
				consumerListObj.put("consumerContractExternalId",contract.get("contractexternalid"));

				//ValidFor with starttime added for the consumerList

				JSONObject validForCL = new JSONObject();
				validForCL.put("startDateTime",getFormattedDate(contract.get("statusvalidfrom")));
				consumerListObj.put("validFor",validForCL);
				consumerListArr.put(consumerListObj);
				sharingProvider.put("consumerList",consumerListArr);
				productTarget.put("sharingProvider",sharingProvider);

				//--------------sharing provider end---------------//

				//--------------sharing consumer start---------------//
				sharingConsumer.put("providerCustomerExternalId",contract.get("customerexternalid"));
				sharingConsumer.put("providerContractExternalId",contract.get("contractexternalid"));
				sharingConsumer.put("providerProductExternalId", generateExtId("tpo",contract.get("luw_id"),currentProd.get("external_id")));
				sharingConsumer.put("consumerListEntryExternalId", generateExtId("cl",contract.get("luw_id"),currentProd.get("external_id")));
				productTarget.put("sharingConsumer",sharingConsumer);
		}
		return productTarget;
	}

	private JSONObject addServiceabilityPO(JSONObject mapping, JSONObject contract, String serviceabilityPO) {
		JSONObject productSerPO = new JSONObject();
		if(mapping.get("serviceability_products").has(serviceabilityPO)) {
			String productOfferingExternalId = serviceabilityPO;
			productSerPO.put("productOfferingExternalId",productOfferingExternalId);

			String serPoExternalId = generateExtId("spo",contract.get("luw_id"),productOfferingExternalId);
			productSerPO.put("externalId",serPoExternalId);

			JSONObject status = new JSONObject();
			JSONObject validFor = getValidFor(getFormattedDate(contract.get("statusvalidfrom")));
			if((!contract.optString("statusvalidto").equals("")))
			{
				validFor.put("endDateTime", getFormattedDate(contract.get("statusvalidto")));
			}
			status.put("status","ProductActive");
			status.put("validFor",validFor);
			JSONArray statusArr = new JSONArray();
			statusArr.put(status);

			productSerPO.put("status",statusArr);

			JSONArray outPricePOArr = new JSONArray();
			LogUtil.logDebug(logger, MessageFormat.format("productOfferingExternalId = {0}", productOfferingExternalId));
			JSONArray inPricePOArr = mapping.get("serviceability_products").get(productOfferingExternalId).get("price");
			for(int i=0; i< inPricePOArr.length();i++) {
				String productOfferingPriceExternalId = inPricePOArr[i];
				validFor = getValidFor(getFormattedDate(contract.get("statusvalidfrom")));
				if((!contract.optString("statusvalidto").equals("")))
				{
					validFor.put("endDateTime", getFormattedDate(contract.get("statusvalidto")));
				}
				JSONObject outPricePO = new JSONObject();
				outPricePO.put("productOfferingPriceExternalId",productOfferingPriceExternalId);
				outPricePO.put("validFor",validFor);

				outPricePOArr.put(outPricePO);
			}
			productSerPO.put("price",outPricePOArr);

			if(mapping.get("serviceability_products").get(productOfferingExternalId).get("is_billingAccountReference").equals("Y")) {
				JSONObject billingAccountReferencePO = new JSONObject();
				billingAccountReferencePO.put("externalId",contract.get("billingaccountref"));
				productSerPO.put("billingAccountReference",billingAccountReferencePO);
			}
			LogUtil.logDebug(logger, MessageFormat.format("productSerPO = {0}", productSerPO));
		}
		return productSerPO;
	}

	private String generateExtId(String prefix, String luwId, String productExtId)
	{
		String str = null;
		str = prefix + "_" + luwId + "_" + productExtId;

		return str;
	}
}