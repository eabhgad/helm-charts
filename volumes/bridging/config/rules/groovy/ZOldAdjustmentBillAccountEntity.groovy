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
 *
 * @author emubhka
 *
 */

public class ZOldAdjustmentBillAccountEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZOldAdjustmentBillAccountEntity.class);

    public JSONArray billAccountAdjEntity(JSONArray stg_beam_balance, JSONObject mapping,JSONArray billingActArray) {

        JSONArray bAEntity = new JSONArray();

        //JSONObject commonMapping = mapping.get("common");
        //JSONObject lookupMapping = mapping.get("lookup");

        //partyRecord.put("birthDate", getFormattedDate(party.get("birthdate")));
        //partyRecord.put("familyName", party.optString("familyname"));

       for(int i=0 ; i<stg_beam_balance.length() ;i++){

        JSONObject billAccountObj = stg_beam_balance.getJSONObject(i);
        if(billAccountObj.optString("bucketadjustmenttype").equals("ba_bucket"))
        {
        JSONObject billingAccountRecord = new JSONObject();
        billingAccountRecord.put("triggerTime", getFormattedDate(billAccountObj.get("triggertime")));
        billingAccountRecord.put("customerExternalId", billAccountObj.optString("customerexternalid"));
        String bucketspecexternalid = billAccountObj.optString("bucketspecexternalid");

        billingAccountRecord.put("billingAccountExternalId", billAccountObj.optString("billingaccountexternalid"));
       // billingAccountRecord.put("billingAccountBucketSpecExternalId", (mapping.get("billing_bucket")).get(bucketspecexternalid).get("bABucket"));
		
		String billAccountExtId = null;
		
		for(int j=0; j<billingActArray.length() ;j++){
		  if(billAccountObj.get("billingaccountexternalid").equals(billingActArray.getJSONObject(j).get("billingaccountexternalid"))){
		    
		  billAccountExtId = billingActArray.getJSONObject(j).get("billingaccountspecexternalid");
		  }
		}
        billingAccountRecord.put("billingAccountBucketSpecExternalId", (mapping.get("billing_bucket")).get(bucketspecexternalid).get("billingaccountspecexternalid").get(billAccountExtId).get("bABucket"));
        String targetUnit = mapping.get("billing_bucket").get(bucketspecexternalid).get("billingaccountspecexternalid").get(billAccountExtId).get("TargetUnit");
        Double multiplicationFactor = Double.parseDouble(mapping.get("unit").get(billAccountObj.optString("unitofmeasure")).get("MultiplicationFactor"));
        int amountSource =Integer.parseInt(billAccountObj.optString("amount"));
        int decimalPlaces= Integer.parseInt(mapping.get("billing_bucket").get(bucketspecexternalid).get("billingaccountspecexternalid").get(billAccountExtId).get("TargetDecimal"));
        Double amountConversion = amountSource*multiplicationFactor;

        double power = Math.pow(10, decimalPlaces);

        int amountFinalAdj = amountConversion*power;

        JSONObject amount = new JSONObject();
        amount.put("number", amountFinalAdj);
        amount.put("decimalPlaces", decimalPlaces);
        billingAccountRecord.put("amount", amount);

        //JSONObject validFor = getValidFor(getFormattedDate(billAccountObj.get("startdatetime")));
        JSONObject validFor = getValidFor(getFormattedDate("2022-02-28T00:00:00"));

       // billingAccountRecord.put("validFor",validFor);

        //billingAccountRecord.put("action", billAccountObj.optString("action"));
        billingAccountRecord.put("action", billAccountObj.optString("action"));
        billingAccountRecord.put("unitOfMeasure", targetUnit);

        bAEntity.put(billingAccountRecord);
        }

       }

        return bAEntity;
    }

    private String getFormattedDate(String dateStr)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date dateObj = formatter.parse(dateStr);

        dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(dateObj);

        return dateStr;

    }

    private JSONObject getValidFor(Object val) {
        JSONObject startDateTime = new JSONObject();
        startDateTime.put("startDateTime", val);
        return startDateTime;
    }
}
