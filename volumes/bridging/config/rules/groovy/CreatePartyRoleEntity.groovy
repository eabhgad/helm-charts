package groovy;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ericsson.datamigration.log.utils.LogUtil;

import java.text.MessageFormat;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;


/**
 * @author abhay
 */
public class CreatePartyRoleEntity {

	private static final Logger logger = LoggerFactory.getLogger(CreatePartyRoleEntity.class);

	JSONArray createPartyRoleEntity(JSONArray partyRole, JSONArray contactMediumAsso, JSONObject mapping,String type) {


		JSONArray partyArr = new JSONArray();
		JSONArray languageArr = new JSONArray();

		JSONObject commonMapping = mapping.get("common");
		JSONObject lookupMapping = mapping.get("lookup");

		JSONObject genderMapping = mapping.get("lookup").get("gender");
		JSONObject nationalityMapping = mapping.get("lookup").get("nationality");
		JSONObject languageMapping = mapping.get("lookup").get("language");
		JSONObject maritalStatusMapping = mapping.get("lookup").get("marital_status");
		JSONObject titleMapping = mapping.get("lookup").get("title");
		JSONObject statusPartyRoleMapping = mapping.get("lookup").get("statusPartyRole");


		for (int i = 0; i < partyRole.length(); i++) {
			JSONObject partyRoleObj = partyRole.getJSONObject(i);
			 if (partyRoleObj.optString("partyroletype").equals("Member")) {
                continue;
            }

			if(partyRoleObj.optString("partyroletype").equals("IIR") && type.equals("IIR")){
				JSONObject partyRecord = new JSONObject();

				partyRecord.put("externalId", partyRoleObj.optString("partyroleextid"));

				partyRecord.put("name", partyRoleObj.optString("partyrolename"));

				//get status getFormattedDate
				JSONObject validFor = getValidFor(getFormattedDate(partyRoleObj.get("statusstartdatetime")));
				JSONObject status = new JSONObject();
				status.put("status", getFromMapping(statusPartyRoleMapping, partyRoleObj, "status"));
				status.put("validFor", validFor);

				//get status arr
				JSONArray statusArr = new JSONArray();
				statusArr.put(status);

				//put into party
				partyRecord.put("status", statusArr);
				JSONArray contactMedAsso = getContactMediumAssoArray(contactMediumAsso, commonMapping, lookupMapping, partyRoleObj);
				if (contactMedAsso.length() > 0) {
					partyRecord.put("contactMediumAssociation", contactMedAsso);
				}

				if (partyRoleObj.optString("associationextid") != null && !partyRoleObj.optString("associationextid").isEmpty()) {
					partyRecord.put("relatedParty", getRelatedParty(partyRoleObj,commonMapping));
				}
				JSONObject engagedParty = new JSONObject();
				engagedParty.put("externalId", partyRoleObj.optString("engagedpartyextid"));
				//engagedParty.put("@referredType", "Organization");
				partyRecord.put("engagedParty", engagedParty);
				JSONObject partyRoleSpecification = new JSONObject();
				JSONObject partyRoleSpecMapping = mapping.get("lookup").get("partyRoleSpec");
				//logger.debug("-----------heyyyy-------------------------" + partyRoleObj.optString("partyrolespecexternalid"));

				partyRoleSpecification.put("externalId", getFromMapping(partyRoleSpecMapping, partyRoleObj, "partyrolespecexternalid"));
				partyRecord.put("partyRoleSpecification", partyRoleSpecification);
				partyRecord.put("action", "create");

				partyArr.put(partyRecord);
			}
			else{
				if(!partyRoleObj.optString("partyroletype").equals("IIR") && !type.equals("IIR")){
					JSONObject partyRecord = new JSONObject();

					partyRecord.put("externalId", partyRoleObj.optString("partyroleextid"));

					partyRecord.put("name", partyRoleObj.optString("partyrolename"));

					//get status getFormattedDate
					JSONObject validFor = getValidFor(getFormattedDate(partyRoleObj.get("statusstartdatetime")));
					JSONObject status = new JSONObject();
					status.put("status", getFromMapping(statusPartyRoleMapping, partyRoleObj, "status"));
					status.put("validFor", validFor);

					//get status arr
					JSONArray statusArr = new JSONArray();
					statusArr.put(status);

					//put into party
					partyRecord.put("status", statusArr);


					JSONArray contactMedAsso = getContactMediumAssoArray(contactMediumAsso, commonMapping, lookupMapping, partyRoleObj);
					if (contactMedAsso.length() > 0) {
						partyRecord.put("contactMediumAssociation", contactMedAsso);
					}

					if (partyRoleObj.optString("associationextid") != null && !partyRoleObj.optString("associationextid").isEmpty()) {
						partyRecord.put("relatedParty", getRelatedParty(partyRoleObj,commonMapping));
					}

					JSONObject engagedParty = new JSONObject();
					engagedParty.put("externalId", partyRoleObj.optString("engagedpartyextid"));
					//engagedParty.put("@referredType", "Organization");
					partyRecord.put("engagedParty", engagedParty);

					JSONObject partyRoleSpecification = new JSONObject();
					JSONObject partyRoleSpecMapping = mapping.get("lookup").get("partyRoleSpec");
					partyRoleSpecification.put("externalId", getFromMapping(partyRoleSpecMapping, partyRoleObj, "partyrolespecexternalid"));

					partyRecord.put("partyRoleSpecification", partyRoleSpecification);
					partyRecord.put("action", "create");

					partyArr.put(partyRecord);
				}
			}
		}
		//LogUtil.logDebug(logger, MessageFormat.format("Party Array = {0}", partyArr.toString()));
		return partyArr;

	}


	private String getFromMapping(JSONObject mapping, JSONObject sourceJson, String fieldName) {
		String object = null;

		for (String objKey : mapping.get("mapping").keySet()) {

			object = mapping.get("mapping").get(objKey);
			if (objKey.equals(sourceJson.optString(fieldName))) {
				break;
			}
		}



		return object;
	}

	private JSONObject getValidFor(Object val) {
		JSONObject startDateTime = new JSONObject();
		startDateTime.put("startDateTime", val);
		return startDateTime;
	}

	private JSONArray getRelatedParty(JSONObject partyRoleObj,commonMapping) {

		JSONArray relatedPartyArr = new JSONArray();
		JSONObject relatedParty = new JSONObject();
		JSONObject targetEntity = new JSONObject();
		JSONObject validFor = new JSONObject();

		validFor.put("startDateTime",getFormattedDate(partyRoleObj.get("relatedpartystartdatetime")));
		validFor.put("endDateTime",getFormattedDate(partyRoleObj.get("relatedpartyenddatetime")));


		//relatedParty.put("targetEntity", targetEntity);
		targetEntity.put("externalId", partyRoleObj.optString("targetextid"));

		targetEntity.put("entityType",commonMapping.get("IIR_Entity_Type"));
		if(!partyRoleObj.optString("targetextid").isEmpty()){
			relatedParty.put("targetEntity", targetEntity);
		}
		relatedParty.put("externalId", partyRoleObj.optString("relatedpartyextid"));

		if (partyRoleObj.get("associationextid") != null) {
			relatedParty.put("associationExternalId", partyRoleObj.optString("associationextid"));

		}
		// common_mapping("IIR_RefferedType") for PartyRole IIR and  common_mapping("Member_RefferedType") for PartyRole Member
		if(partyRoleObj.optString("partyroletype").equals("IIR")){
			relatedParty.put("@referredType", commonMapping.get("IIR_RefferedType"));
		}
		else if(partyRoleObj.optString("partyroletype").equals("Member")){
			relatedParty.put("@referredType", commonMapping.get("Member_RefferedType"));
		}
		relatedParty.put("validFor",validFor);

		//relatedParty.put("externalId",associationextid);

		relatedPartyArr.put(relatedParty);
		return relatedPartyArr;

	}

	private JSONArray getContactMediumAssoArray(JSONArray contactMediumAssociationArr, JSONObject commonMapping, JSONObject lookupMapping, JSONObject partyRoleObj) {
		JSONArray cMediumArr = new JSONArray();
		JSONObject contactRole_Med_ASSMapping = lookupMapping.get("contactRole_Med_ASS");
		JSONObject language_Med_ASSMapping = lookupMapping.get("language_Med_ASS");
		JSONObject enabled_Med_ASSMapping = lookupMapping.get("Enabled_Med_ASS");


		for (int i = 0; i < contactMediumAssociationArr.length(); i++) {
			JSONObject contactMediumAssociation = contactMediumAssociationArr.getJSONObject(i);

			if ((contactMediumAssociation.optString("associatedentitytype").equals("PartyRole")) &&
				(contactMediumAssociation.optString("associatedentityexternalid").equals(partyRoleObj.optString("partyroleextid")))) {

				JSONObject cMediumAsso = null;
				JSONObject validFor = null;
				validFor = getValidFor(getFormattedDate(contactMediumAssociation.get("startdatetime")));

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

	private JSONArray getPaymentMethodArray(JSONObject pMethod, JSONObject commonMapping) {
		JSONObject paymentMethod = new JSONObject();
		paymentMethod.put("externalId", pMethod.get("partyexternalid"));

		JSONObject paymentMethodSpecification = new JSONObject();
		//paymentMethodSpecification.put("externalId",commonMapping.get("payment_method"));
		//paymentMethodSpecification.put("id","2FCCA6676BFE495B8E47DF9183D0F34B");
		paymentMethodSpecification.put("id", commonMapping.get("payment_method_id"));
		paymentMethod.put("paymentMethodSpecification", paymentMethodSpecification);

		JSONObject validFor = getValidFor(getFormattedDate(pMethod.get("startdatetime")));
		paymentMethod.put("validFor", validFor);
		JSONArray paymentMethodArr = new JSONArray();
		paymentMethodArr.put(paymentMethod);

		return paymentMethodArr;

	}


	private JSONArray getMaritalStatusArray(JSONObject party, JSONObject maritalStatusMappingArr) {
		JSONObject maritalStatus = new JSONObject();

		//maritalStatus.put("status",party.optString("maritalstatus"));
		maritalStatus.put("status", getFromMapping(maritalStatusMappingArr, party, "maritalstatus"));

		//JSONObject validFor = getValidFor(getFormattedDate(status.get("MaritalStatusStartDatetime")));
		JSONObject validFor = getValidFor(getFormattedDate(party.get("statusstartdatetime")));
		maritalStatus.put("validFor", validFor);
		JSONArray maritalStatusArr = new JSONArray();
		maritalStatusArr.put(maritalStatus);

		return maritalStatusArr;

	}

	private String getFormattedDate(String dateStr) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date dateObj = formatter.parse(dateStr);

		dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(dateObj);

		return dateStr;

	}

}
