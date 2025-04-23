package groovy;

import org.json.JSONArray
import org.json.JSONObject

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ericsson.datamigration.log.utils.LogUtil;
import java.text.MessageFormat;

import com.ericsson.datamigration.bss.transformation.utils.SequenceGenerator
/**
 * 
 * @author erganaa
 *
 */
class CreateCustomerEntity {

	private static final Logger logger = LoggerFactory.getLogger(CreateCustomerEntity.class);
	private AtRulesConstant atrules = new AtRulesConstant();

	def JSONArray createCustomerEntity(JSONObject customer,JSONArray billingAccountArr,JSONArray contactMediumAssoArr,JSONArray billCycleHist, JSONObject mapping,JSONArray partyRole,String migration_type) {
		JSONObject customerRecord = new JSONObject();
		JSONArray customerArr = new JSONArray();
		JSONObject commonMapping = mapping.get("common");
		JSONObject lookupMapping = mapping.get("lookup");
		JSONObject  custSpec = lookupMapping.get("CustSpec");

		JSONObject response = new JSONObject()
        response.put("errorGenerationPoint",atrules.ERROR_GENERATION_POINT)
        JSONArray messages = new JSONArray()
		JSONObject TE_VALIDATION_CUSTOMER = mapping.getJSONObject("te_error_validation").getJSONObject("Customer").getJSONObject("error_id");


		customerRecord.put("externalId", customer.get("customerexternalid"));
		customerRecord.put("engagedPartyExternalId", customer.get("engagedpartyexternalid"));
		customerRecord.put("relatedPartyExternalId", customer.get("engagedpartyexternalid"));

		JSONObject customerSpecification = new JSONObject();
		//customerSpecification.put("externalId", commonMapping.get("cust_spec"));
		
		//CustomerSpecification externalId changed from copt to read it fromthe mapping file.
		
        String customerspecexternalidField = getFromMappingWithValidation(custSpec,customer,"customerspecexternalid")
        
        
        if(customerspecexternalidField == null){
           messages = populateErrorResponse(messages,atrules.CUSTOMER_CUSTOMERSPECEXTERNALID_ERROR_CODE,TE_VALIDATION_CUSTOMER,"stg_beam_customer.customerspecexternalid",customer.optString("customerspecexternalid"))
        }
        else{
          customerSpecification.put("externalId",customerspecexternalidField);
        }
		customerRecord.put("customerSpecification", customerSpecification);
		customerRecord.put("account", getAccountArray(billingAccountArr,billCycleHist,contactMediumAssoArr,mapping,migration_type));


		//-------------Customer Status Start---------------//
		JSONObject validFor = getValidFor(getFormattedDate(customer.get("statusstartdatetime")));
		JSONObject status = new JSONObject();
		status.put("status","CustomerActive");
		status.put("validFor",validFor);
		JSONArray statusArr = new JSONArray();
		statusArr.put(status);
		customerRecord.put("status",statusArr);
		//-------------Customer Status End----------------//



		//-------------homeTimeZone Start-----------------//
		JSONObject homeTimeZone = new JSONObject();
		homeTimeZone.put("timeZone",getFromMapping(mapping.get("lookup").get("homeTimeZone"),customer,"hometimezone"));
		validFor = getValidFor(getFormattedDate(customer.get("statusstartdatetime")));
		homeTimeZone.put("validFor",validFor);
		JSONArray homeTimeZoneArr = new JSONArray();
		homeTimeZoneArr.put(homeTimeZone);
		customerRecord.put("homeTimeZone",homeTimeZoneArr);
		//-------------homeTimeZone End-----------------//


		//-------------contactMediumAssociation Start---------------//
        
		//Source data changed from contactmedium table to contactmediumAsso table
		//contactMediumAssociation creating the JSONArray from contactmediumassociation table.
		JSONArray contactMedAsso = getContactMediumAssoArray(contactMediumAssoArr, commonMapping, lookupMapping, customer);
		if (contactMedAsso.length() > 0) {
			customerRecord.put("contactMediumAssociation", contactMedAsso);
		}
		//-------------contactMediumAssociation End---------------//
       
	   if(partyRole.length() > 0){
		JSONObject partyRoleObj = partyRole.getJSONObject(0);
		JSONArray relatedPartyArr = new JSONArray();
		JSONObject relatedParty = new JSONObject();

		 if (partyRoleObj.optString("partyroletype").equals("Member")) {
			relatedParty.put("associationExternalId",partyRoleObj.optString("associationextid"));
             JSONObject RelvalidFor = new JSONObject();
			 RelvalidFor.put("startDateTime",getFormattedDate(partyRoleObj.get("relatedpartystartdatetime")));
			 RelvalidFor.put("endDateTime",getFormattedDate(partyRoleObj.get("relatedpartyenddatetime")));
			 relatedParty.put("validFor",RelvalidFor);
			 relatedParty.put("@referredType",mapping.get("common").get("Member_RefferedType"));
			 relatedParty.put("externalId",partyRoleObj.optString("relatedpartyextid"));

         }
		relatedPartyArr.put(relatedParty);
        if(relatedParty.has("associationExternalId")){
		customerRecord.put("relatedParty",relatedPartyArr);
		}
	   }

		JSONObject engagedParty = new JSONObject();
		engagedParty.put("externalId",customer.get("engagedpartyexternalid"));
		customerRecord.put("engagedParty",engagedParty);

		response.put("messages",messages)
        if(messages.length()> 0){
         customerRecord.put("response",response)
         customerRecord.put("success",false)

        }

		customerRecord.put("action", "create");

		customerArr.put(customerRecord);

		//LogUtil.logDebug(logger, MessageFormat.format("Customer Array = {0}", customerArr.toString()));

		return customerArr;
	}

	private JSONArray populateErrorResponse(JSONArray messages,String errorCode,JSONObject TE_VALIDATION_PARTY,String type,String value){
          JSONObject erroCodeObj = TE_VALIDATION_PARTY.getJSONObject(errorCode)
          JSONObject errorDetails = new JSONObject()
          errorDetails.put("action",atrules.VALIDATION_ACTION)
          errorDetails.put("errorId",errorCode)
          String detailsMsg = atrules.DETAILS_MSG.replace("TABLE_INFO",type).replace("SOURCE_VALUE",value)
          errorDetails.put("details",detailsMsg)
          errorDetails.put("errorSeverity",erroCodeObj.get("ERROR_SEVERITY"))
          errorDetails.put("errorFieldType",type)
          errorDetails.put("errorFieldValue",value)
          messages.put(errorDetails);

        return messages

    }

	private JSONArray getContactMediumAssoArray(JSONArray contactMediumAssociationArr, JSONObject commonMapping, JSONObject lookupMapping, JSONObject entity) {
		JSONArray cMediumArr = new JSONArray();
		for (int i = 0; i < contactMediumAssociationArr.length(); i++) {
			JSONObject contactMediumAssociation = contactMediumAssociationArr.getJSONObject(i);			
			if((contactMediumAssociation.optString("associatedentitytype").equals("Customer")) &&
			contactMediumAssociation.optString("associatedentityexternalid").equals(entity.optString("customerexternalid"))) {
				JSONObject cMediumAsso = getContactMediumAssoObject(contactMediumAssociation, lookupMapping);
				cMediumArr.put(cMediumAsso);
			}
		}
		return cMediumArr;
	}

	private JSONArray getContactMediumAssoArrayForAccount(JSONArray contactMediumAssociationArr, JSONObject commonMapping, JSONObject lookupMapping, JSONObject entity) {
		JSONArray cMediumArr = new JSONArray();
		for (int i = 0; i < contactMediumAssociationArr.length(); i++) {
			JSONObject contactMediumAssociation = contactMediumAssociationArr.getJSONObject(i);
			if((contactMediumAssociation.optString("associatedentitytype").equals("BillingAccount")) && 
			contactMediumAssociation.optString("associatedentityexternalid").equals(entity.optString("billingaccountexternalid"))) {				
				JSONObject cMediumAsso = getContactMediumAssoObject(contactMediumAssociation, lookupMapping);
				cMediumArr.put(cMediumAsso);
			}
		}
		return cMediumArr;
	}
	

	private JSONObject getContactMediumAssoObject(JSONObject contactMediumAssociation, JSONObject lookupMapping) {
		
		JSONObject cMediumAsso = new JSONObject();
		JSONObject contactRole_Med_ASSMapping = lookupMapping.get("contactRole_Med_ASS");
		JSONObject language_Med_ASSMapping = lookupMapping.get("language_Med_ASS");
		JSONObject enabled_Med_ASSMapping = lookupMapping.get("Enabled_Med_ASS");
		JSONObject validFor = null;
		validFor = getValidFor(getFormattedDate(contactMediumAssociation.get("startdatetime")));
		
		String contactRole = getFromMapping(contactRole_Med_ASSMapping, contactMediumAssociation, "contactrole");
		String contactMediumExternalId = contactMediumAssociation.optString("contactmediumexternalid");
		String language = getFromMapping(language_Med_ASSMapping, contactMediumAssociation, "language");
		String enabled = getFromMapping(enabled_Med_ASSMapping, contactMediumAssociation, "enabled");
		boolean enabledBoolean = Boolean.parseBoolean(enabled);

		cMediumAsso.put("contactRole", contactRole);
		cMediumAsso.put("contactMediumExternalId", contactMediumExternalId);
		cMediumAsso.put("language", language);
		cMediumAsso.put("enabled", enabledBoolean);
		cMediumAsso.put("validFor", validFor);

		return cMediumAsso;

	}

	private JSONArray getAccountArray(JSONArray billingAccountArr,JSONArray billCycleHist,JSONArray contactMediumAssoArr, JSONObject mapping,String migration_type) {

		JSONObject commonMapping = mapping.get("common");
		JSONObject lookupMapping = mapping.get("lookup");
		JSONObject billingAccountSpecMapping = lookupMapping.get("billingAccountSpecExternalId");
		JSONObject billCycleSpecMapping = lookupMapping.get("billCycleSpecExternalId");
		JSONObject billPresentationMedia = lookupMapping.get("BillPresentationMedia");

		JSONArray accountArr = new JSONArray();

		for(int indx=0;indx<billingAccountArr.length();indx++)
		{
			JSONObject billingAccount = billingAccountArr.getJSONObject(indx);

			JSONObject account = new JSONObject();
			account.put("externalId",billingAccount.get("billingaccountexternalid"));
			account.put("billingAccountSpecExternalId",getFromMapping(billingAccountSpecMapping,billingAccount,"billingaccountspecexternalid"));
             
			JSONObject regularBillingAccount = new JSONObject();
			JSONObject regBillingAccount = new JSONObject();
			if(billingAccount.has("regularbillingaccountexternalid")) {
				String regularbillingaccountexternalid=billingAccount.optString("regularbillingaccountexternalid");
				if(regularbillingaccountexternalid != null || !regularbillingaccountexternalid.isEmpty()){
					regBillingAccount.put("externalId",regularbillingaccountexternalid);
					regularBillingAccount.put("billingAccount",regBillingAccount);
				}
				JSONObject regRelatedParty= new JSONObject();

				String regularbillingAccountCustExternalId = billingAccount.optString("regularbillingaccountcustexternalid");
				if(regularbillingAccountCustExternalId != null || !regularbillingAccountCustExternalId.isEmpty()){
					regRelatedParty.put("externalId",regularbillingAccountCustExternalId);
					regRelatedParty.put("@referredType",mapping.get("common").get("Customer_RefferedType"));
					regularBillingAccount.put("relatedParty",regRelatedParty);
				}
				if(regBillingAccount.has("externalId")){
					account.put("regularBillingAccount",regularBillingAccount);
				}
			}		


			//----Status Start-------//
			JSONObject accountStatus = new JSONObject();
			JSONObject validFor = getValidFor(getFormattedDate(billingAccount.get("statusstartdatetime")));
			accountStatus.put("validFor", validFor);
			accountStatus.put("status", "BillingAccountActive");
			JSONArray accountStatusArr = new JSONArray();
			accountStatusArr.put(accountStatus);
			account.put("status",accountStatusArr);
			//----Status End---------//

			//----customerBillCycleSpecification Start-------//
			JSONObject billCycleSpec = new JSONObject();
			if(!regBillingAccount.has("externalId")) {

				for(int t =0 ; t<billCycleHist.length();t++ ){
					if(billCycleHist.getJSONObject(t).get("billingaccountexternalid").equals(billingAccount.get("billingaccountexternalid"))){
                     billCycleSpec.put("externalId",billCycleHist.getJSONObject(t).get("billcycleexternalid"));
				     billCycleSpec.put("billCycleSpecExternalId",getFromMapping(billCycleSpecMapping,billCycleHist.getJSONObject(t),"billcyclespecexternalid"));
				     validFor = getValidFor(getFormattedDate(billCycleHist.getJSONObject(t).get("validfromdate")));
				     billCycleSpec.put("validFor", validFor);
					}
				
				}
				
				JSONArray billCycleSpecArr = new JSONArray();
				billCycleSpecArr.put(billCycleSpec);
				account.put("customerBillCycleSpecification",billCycleSpecArr);
			}
			
			//----customerBillCycleSpecification End-------//

			//----contactMediumAssoc for Account Start-------//
			JSONArray contactMedAsso = getContactMediumAssoArrayForAccount(contactMediumAssoArr, commonMapping, lookupMapping, billingAccount);
			if (contactMedAsso.length() > 0) {
				account.put("contactMediumAssociation", contactMedAsso);
			}
			//----contactMediumAssoc for Account End-------//
             if(migration_type.equals("BILLER_EBEV")){
			//----billStructure Start-------//
			 JSONObject billStructure = new JSONObject();
			 JSONObject billStructureSpecification = new JSONObject();
			 JSONObject format = new JSONObject();
			 JSONObject billFormatSpecification = new JSONObject();
			 JSONObject presentationMedia = new JSONObject();
			 JSONObject billPresentationMediaSpecification = new JSONObject();

			// //billStructure.put("externalId","bs_external-0001");
			 billStructure.put("externalId","bs_"+billingAccount.get("customerexternalid"));
			// //billStructureSpecification.put("externalId","MH_BillStructSpec_02");
			 billStructureSpecification.put("externalId",mapping.get("common").get("bill_structure_spec"));
			 billStructure.put("billStructureSpecification",billStructureSpecification);
			// format.put("externalId","fm_external-0001");
			 format.put("externalId","fm_"+billingAccount.get("customerexternalid"));
			 validFor = getValidFor(getFormattedDate(billingAccount.get("statusstartdatetime")));
			 format.put("validFor", validFor);

			 billFormatSpecification.put("externalId","MH_DocumentSpec_Inv_01");
			 format.put("billFormatSpecification",billFormatSpecification);
			 presentationMedia.put("externalId","bsm_external-0001");
			 presentationMedia.put("validFor", validFor);
			 billPresentationMediaSpecification.put("externalId",getFromMapping(billPresentationMedia,billingAccount,"billpresentationmedia"));
			 presentationMedia.put("billPresentationMediaSpecification",billPresentationMediaSpecification);
			 JSONArray presentationMediaArr = new JSONArray();
			 presentationMediaArr.put(presentationMedia);
			 format.put("presentationMedia",presentationMediaArr);
			 JSONArray formatArr = new JSONArray();
			 formatArr.put(format);
			 billStructure.put("format", formatArr);

			 if(!regBillingAccount.has("externalId")){
			 account.put("billStructure",billStructure);
			}

			 }
			//----billStructure End-------//

			accountArr.put(account);
		}

		return accountArr;
	}

	private JSONObject getValidFor(Object val) {
		JSONObject startDateTime = new JSONObject();
		startDateTime.put("startDateTime", val);
		return startDateTime;
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
	private String getFromMappingWithValidation(JSONObject mapping, JSONObject sourceJson, String fieldName) {
        String object = null

        for (String objKey : mapping.get('mapping').keySet()) {
            
            if (objKey.equals(sourceJson.optString(fieldName))) {
                object = mapping.get('mapping').get(objKey)
                break
            }
        }

        return object
    }

	private String getFormattedDate(String dateStr)
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date dateObj = formatter.parse(dateStr);

		dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(dateObj);

		return dateStr;

	}

}
