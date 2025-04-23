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

public class ZOldAdjustmentProductEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZOldAdjustmentProductEntity.class);


    public JSONArray productAdjEntity(JSONArray stg_beam_balance, JSONObject mapping, JSONArray productArray) {
        
        
        JSONArray productEntity = new JSONArray();

        JSONObject commonMapping = mapping.get("common");
        JSONObject lookupMapping = mapping.get("lookup");

        //partyRecord.put("birthDate", getFormattedDate(party.get("birthdate")));
        //partyRecord.put("familyName", party.optString("familyname"));

       for(int i=0 ; i<stg_beam_balance.length() ;i++){

        JSONObject productAdjObj = stg_beam_balance.getJSONObject(i);
        if(productAdjObj.optString("bucketadjustmenttype").equals("pb_bucket")){
        JSONObject productRecord = new JSONObject();
        productRecord.put("triggerTime", getFormattedDate(productAdjObj.get("triggertime")));
        productRecord.put("customerExternalId", productAdjObj.optString("customerexternalid"));
        productRecord.put("contractExternalId", productAdjObj.optString("contractexternalid"));

        String bucketspecexternalid = productAdjObj.optString("bucketspecexternalid");
        productRecord.put("productExternalId", productAdjObj.optString("productexternalid"));
				
		String productOfferingExternalId = null;
		
        //Checks with source products - contractexternalid, productexternalid and check productofferingexternalid with mapping
		for(int j=0; j<productArray.length() ;j++){
          
          //String productOffExtId = productArray.getJSONObject(j).get("productofferingexternalid");
          //if(mapping.get("product_bucket").get(productAdjObj.optString("bucketspecexternalid")).get("productofferingexternalid").get(productOffExtId)) {  
          //}

		  if(productAdjObj.get("contractexternalid").equals(productArray.getJSONObject(j).get("contractexternalid"))
            &&  productAdjObj.get("productexternalid").equals(productArray.getJSONObject(j).get("productexternalid")))
            {
            LOGGER.debug("productAdjObj {} ",productAdjObj);
		    productOfferingExternalId = productArray.getJSONObject(j).get("productofferingexternalid");
            LOGGER.debug("productOfferingExternalId {} ",productOfferingExternalId);
		    }
		}
        productRecord.put("bucketSpecExternalId", (mapping.get("product_bucket")).get(productAdjObj.optString("bucketspecexternalid")).get("productofferingexternalid").get(productOfferingExternalId).get("ProductBucket"));
        
        String targetUnit = mapping.get("product_bucket").get(bucketspecexternalid).get("productofferingexternalid").get(productOfferingExternalId).get("TargetUnit");
        Double multiplicationFactor = Double.parseDouble(mapping.get("unit").get(productAdjObj.optString("unitofmeasure")).get("MultiplicationFactor"));
        int amountSource =Integer.parseInt(productAdjObj.optString("amount"));
        int decimalPlaces= Integer.parseInt(mapping.get("product_bucket").get(bucketspecexternalid).get("productofferingexternalid").get(productOfferingExternalId).get("TargetDecimal"));
        Double amountConversion = amountSource*multiplicationFactor;

        double power = Math.pow(10, decimalPlaces);

        int amountFinalAdj = amountConversion*power;




        JSONObject amount = new JSONObject();
        amount.put("number", amountFinalAdj);
        amount.put("decimalPlaces", decimalPlaces);
        productRecord.put("amount", amount);
       
        //JSONObject validFor = getValidFor(getFormattedDate(billAccountObj.get("startdatetime")));
           JSONObject validFor = getValidFor(getFormattedDate("2022-02-28T00:00:00"));


      //  productRecord.put("validFor",validFor);

        productRecord.put("action", productAdjObj.optString("action"));
        //productRecord.put("action", "Set");
        productRecord.put("unitOfMeasure", targetUnit);
        
        productEntity.put(productRecord);
        }

       }

        return productEntity;
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
