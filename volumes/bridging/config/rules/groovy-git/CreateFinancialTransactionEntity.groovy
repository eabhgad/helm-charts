package groovy;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;
import java.util.UUID;

import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ericsson.datamigration.log.utils.LogUtil;

public class CreateFinancialTransactionEntity{

	private static final Logger logger = LoggerFactory.getLogger(CreateFinancialTransactionEntity.class);

	def JSONArray createFinancialTransactionEntity(JSONObject finHead, JSONObject finEntry, JSONArray finCharc, JSONObject mapping)
	{
		JSONArray output = new ArrayList();
		JSONObject financialTransactionRecord = new JSONObject();
		JSONObject financialTransaction = new JSONObject();
		JSONObject commonMapping = mapping.get("common");
		JSONObject lookupMapping = mapping.get("lookup");
		
		//----relatedParty Start---///
		JSONArray relatedPartyArr = new JSONArray();
		JSONObject relatedParty = new JSONObject();
		relatedParty.put("@referredType","Customer");
		relatedParty.put("externalId",finHead.get("customerexternalid"));
		//relatedParty.put("externalId","party_ext_1001");
		relatedPartyArr.put(relatedParty);
		financialTransaction.put("relatedParty",relatedPartyArr);
		//----relatedParty End---///

		//----financialHeader Start---///
		JSONArray financialHeaderArr = new JSONArray();
		JSONObject financialHeader = new JSONObject();
		JSONObject financialHeaderSpecification = new JSONObject();
		JSONArray financialEntryArr = new JSONArray();
		JSONObject financialEntry = new JSONObject();
		JSONObject amount = new JSONObject();
		JSONObject value = new JSONObject();
		JSONObject financialEntrySpecification = new JSONObject();
		JSONObject billingAccount = new JSONObject();
		JSONObject financialEntry_2 = new JSONObject();
		JSONArray financialEntryArr_2 = new JSONArray();
		
		//financialHeader.put("externalId",finHead.get("fintaskexternalid"));
		//Date currentTime = new Date();
		//financialHeader.put("externalId",currentTime.getTime()+"");
		String uniqueID = UUID.randomUUID().toString();
		financialHeader.put("externalId",uniqueID);
		
		//----billingAccount Start----//
		billingAccount.put("externalId",finHead.optString("billingaccountexternalid"));
		financialHeader.put("billingAccount",billingAccount);
		//----billingAccount End----//
		
		
		//----characteristic Start----//
		JSONArray characteristicArr = new JSONArray();
		
		JSONArray characteristicHeadArr = new JSONArray();
		
		for(int i=0;i<finCharc.length();i++)
		{
			JSONObject characteristic = new JSONObject();
			JSONArray valueArr = new JSONArray();
			JSONObject charac_value = new JSONObject();
						
			JSONObject finCharcEle = finCharc.getJSONObject(i);
						
			if(!finCharcEle.optString("entryitemid").equals(""))
			{
				characteristic.put("charSpecExternalId",finCharcEle.get("charspecexternalid"));
				charac_value.put("value", finCharcEle.get("charvalue"));
				charac_value.put("unitOfMeasure", finCharcEle.get("uom"));
				valueArr.put(charac_value);	
				characteristic.put("value",valueArr);
				characteristicArr.put(characteristic);
			}
			else
			{
				characteristic.put("charSpecExternalId",finCharcEle.get("charspecexternalid"));
				charac_value.put("value", finCharcEle.get("charvalue"));
				charac_value.put("unitOfMeasure", finCharcEle.get("uom"));
				valueArr.put(charac_value);	
				characteristic.put("value",valueArr);
				characteristicHeadArr.put(characteristic);
			}
						
		}
		//financialEntry.put("characteristic",characteristicArr); // Characteristics for Finantial Entry
		//financialHeader.put("characteristic",characteristicHeadArr); // Characteristics for Financial Header
		//----characteristic End----//
		

		//----financialHeaderSpecification Start----//
		//financialHeaderSpecification.put("externalId","Payment_SI_XXX");
		financialHeaderSpecification.put("externalId",getFromMapping(lookupMapping.get("financialHeaderSpecExtId"),finHead,"transactiontype"));
		//financialHeaderSpecification.put("id","ED881107D2224C4083C33B10C7ADE531");
		financialHeader.put("financialHeaderSpecification",financialHeaderSpecification);
		//----financialHeaderSpecification End----//
		
		//----financialEntry Start----//
		financialEntry.put("externalId",finEntry.get("entryitemid"));
		
		if(!finEntry.optString("commonname").equals(""))
		{
			financialEntry.put("name",finEntry.get("commonname")); 
		}
		
		if(!finEntry.optString("billingaccountbooking").equals(""))
		{
			financialEntry.put("billingAccountBooking",finHead.get("billingaccountbooking"));
		}
		
		//----amount Start----//
		value.put("number", finEntry.get("amount"));
		amount.put("value",value);
		amount.put("unitOfMeasure",commonMapping.get("financial_entry_uom"));
		financialEntry.put("amount",amount);
		//----amount End----//
		
		//----financialEntry_2 Start----//
		if(!finEntry.optString("fintaskexternalid").equals(""))
		{
			financialEntry_2.put("financialHeaderExternalId",finEntry.get("fintaskexternalid"));
		}
		financialEntryArr_2.put(financialEntry_2);
		//financialEntry.put("financialEntry",financialEntryArr_2);
		//----financialEntry_2 End----//
		
		//----financialEntrySpecification Start----//
		financialEntrySpecification.put("externalId",getFromMapping(lookupMapping.get("financialEntrySpecExtId"),finHead,"transactiontype"));
		//financialEntrySpecification.put("id","66FB63768E9645EFB30843853B48F041");
		financialEntry.put("financialEntrySpecification",financialEntrySpecification);
		//----financialEntrySpecification End----//
		
		//----declarationOfIntent Start----//
		JSONObject declarationOfIntent = new JSONObject();
		if(!finHead.optString("paymentInitiation").equals(""))
		{
			declarationOfIntent.put("retainCredit",finHead.get("paymentInitiation"));
			financialEntry.put("declarationOfIntent",declarationOfIntent);
		}
		//----declarationOfIntent End----//
		
		financialEntryArr.put(financialEntry);
		financialHeader.put("financialEntry",financialEntryArr);
		//----financialEntry End----//
		
		if(!finHead.optString("description").equals(""))
		{
			financialHeader.put("description",finHead.get("description"));
		}
		
		if(!finHead.optString("duedate").equals(""))
		{
			financialHeader.put("dueDate",getFormattedDate(finHead.get("duedate")));
		}
		
		if(!finHead.optString("paymentinitiation").equals(""))
		{
			financialHeader.put("paymentInitiation",getFormattedDate(finHead.get("paymentinitiation")));
		}

		financialHeaderArr.put(financialHeader)
		financialTransaction.put("financialHeader",financialHeaderArr);
		//----financialHeader End---///
		
		financialTransaction.put("sourceName",finHead.get("channel"));
		financialTransaction.put("transactionDate",getFormattedDate(finHead.get("paymentdate")));
		financialTransaction.put("transactionType",getFromMapping(lookupMapping.get("transactionType"),finHead,"transactiontype"));
		financialTransactionRecord.put("financialTransaction",financialTransaction);
		financialTransactionRecord.put("action", "create");
		
		//LogUtil.logDebug(logger, MessageFormat.format("Financial Trans Record = {0}", financialTransactionRecord.toString()));

		output.put(financialTransactionRecord);
		return output;
	}
	
	
	private String getFormattedDate(String dateStr)
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date dateObj = formatter.parse(dateStr);
		
		dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(dateObj);
		
		return dateStr;
	
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

}