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

public class AdjustmentBillAccountEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdjustmentBillAccountEntity.class);

    public JSONArray billAccountAdjEntity(JSONArray stgBeamBalance, JSONObject mapping,JSONArray billingActArray) {

        JSONArray bAEntity = new JSONArray();
        JSONObject billingBucketMapping = mapping.get("billing_bucket");
        JSONObject billingActSpecExtIdMapping = mapping.get("lookup").get("billingAccountSpecExternalId").get("mapping");
        Map<String,List<String>> bucketsAddedMap = new HashMap<String,List<String>>(); 


        //First loop through balance bucket from source
        for(JSONObject balanceBucket : stgBeamBalance) {
            if(balanceBucket.get("bucketadjustmenttype").equalsIgnoreCase("ba_bucket")) {
                String srcBuckExtId = balanceBucket.get("billingaccountexternalid");
                String srcBuckSpecExtId = balanceBucket.get("bucketspecexternalid");
                JSONObject targetBillingAccRecord = new JSONObject();
                
                if(billingBucketMapping.has(srcBuckSpecExtId)) {
                    JSONObject billActSpecExtIdMapping = billingBucketMapping.get(srcBuckSpecExtId).get("billingaccountspecexternalid");
                    
                    String srcBillActSpecExtId = null;
                    for(JSONObject billingAct : billingActArray) {
                        if(srcBuckExtId.equals(billingAct.get("billingaccountexternalid"))){
                            srcBillActSpecExtId = billingAct.get("billingaccountspecexternalid");
                        }
                    }

                    if(billActSpecExtIdMapping.has(srcBillActSpecExtId)) {
                        JSONObject billingActBuckObj = billActSpecExtIdMapping.get(srcBillActSpecExtId);
                        //System.out.println("billingActBuckObj" + billingActBuckObj);

                        // Fetching respective billing ext id from mapping
                        String billExternalId = billingActSpecExtIdMapping.get(srcBillActSpecExtId);
                        System.out.println("Billing External id Lookup: " + billExternalId);
                        if(!bucketsAddedMap.containsKey(billExternalId)) {
                            bucketsAddedMap.put(billExternalId, new ArrayList<String>());
                        }
                        bucketsAddedMap.get(billExternalId).add(billingActBuckObj.get("bABucket"));

                        
                        String srcBillingActBucketType = billingActBuckObj.get("sourcebillingaccountbuckettype");
                        String targetBillingActBucketType = billingActBuckObj.get("Targetbillingaccountbuckettype");
                        long targetBillingAccBucketMin = Long.parseLong(billingActBuckObj.get("Value1"));
                        long targetBillingAccBucketMax = Long.parseLong(billingActBuckObj.get("Value2"));
                        String targetBillingAccountAction = billingActBuckObj.get("Value3");
                        
                        String targetUnit = billingActBuckObj.get("TargetUnit");
                        Double multiplicationFactor = Double.parseDouble(mapping.get("unit").get(balanceBucket.optString("unitofmeasure")).get("MultiplicationFactor"));
                        int srcBillingActBucketAmt = balanceBucket.get("amount");
                        //System.out.println("Amount: " + srcBillingActBucketAmt);
                        int decimalPlaces= Integer.parseInt(billingActBuckObj.get("TargetDecimal"));
                        Double amountConversion = srcBillingActBucketAmt*multiplicationFactor;
                        double power = Math.pow(10, decimalPlaces);
                        long amountFinalAdj = amountConversion*power;
                        
                        JSONObject amount = new JSONObject();

                        if(srcBillingActBucketAmt >= 0) {
                            if(srcBillingActBucketType.equalsIgnoreCase(targetBillingActBucketType)) {
                                amount.put("number", amountFinalAdj);
                            } else {
                                amount.put("number", targetBillingAccBucketMax-amountFinalAdj);
                            }
                        } else {
                            if(srcBillingActBucketType.equalsIgnoreCase("desc") && targetBillingActBucketType.equalsIgnoreCase("desc")) {
                                amount.put("number", amountFinalAdj);
                            }
                        }

                        amount.put("decimalPlaces", decimalPlaces);
                        targetBillingAccRecord.put("amount",amount);
                        targetBillingAccRecord.put("action",targetBillingAccountAction);
                        targetBillingAccRecord.put("billingAccountBucketSpecExternalId",billingActBuckObj.get("bABucket"));
                        targetBillingAccRecord.put("billingAccountExternalId",srcBuckExtId);
                        targetBillingAccRecord.put("customerExternalId",balanceBucket.get("customerexternalid"));
                        targetBillingAccRecord.put("triggerTime",getFormattedDate(balanceBucket.get("triggertime")));
                        //Setting validFor
                        if(balanceBucket.has("startdatetime")) {
                            JSONObject validFor = getValidFor(getFormattedDate(balanceBucket.get("startdatetime")));
                            targetBillingAccRecord.put("validFor", validFor);
                        }

                        targetBillingAccRecord.put("unitOfMeasure",targetUnit);
                        targetBillingAccRecord.put("origin","source");
                        bAEntity.put(targetBillingAccRecord);
                    }
                }
            }
        }

        // Adding default billing accounts to billingBucket
        JSONObject babDefaultMapping = mapping.get("bab_default");
        JSONObject billingAccSpecExtIdMapping = mapping.get("lookup").get("billingAccountSpecExternalId").get("mapping");

        for(JSONObject billingAct : billingActArray) {
            String billingActExternalId = billingAct.get("billingaccountexternalid");

            if(billingAccSpecExtIdMapping.has(billingAct.get("billingaccountspecexternalid"))) {
                String billingAccSpecExtId = billingAccSpecExtIdMapping.get(billingAct.get("billingaccountspecexternalid"));
                String billingAccountExternalId = billingAct.get("billingaccountexternalid");
                if(babDefaultMapping.has(billingAccSpecExtId)) {
                    JSONObject baBucket = babDefaultMapping.get(billingAccSpecExtId).get("bABucket");

                    //Check for billingAccSpecExtId itself not present in source bucket, all default needs to be added
                    if(bucketsAddedMap.containsKey(billingAccSpecExtId)) {
                        Iterator<String> baBucketKeys = baBucket.keys();
                        while(baBucketKeys.hasNext()) {
                            String baBucketDefaultKey = baBucketKeys.next();
                            JSONObject baBucketObj = baBucket.get(baBucketDefaultKey);
                            System.out.println("bucketsAddedMap: " + bucketsAddedMap);
                            if(bucketsAddedMap.get(billingAccSpecExtId).contains(baBucketDefaultKey)) {
                                System.out.println("Billing Bucket [" + baBucketDefaultKey + "] exists in source, skipping default addition");
                            } else {
                                addDefaultProducts(baBucketObj, baBucketDefaultKey, billingAct, billingAccountExternalId, bAEntity);
                            }
                        }
                    } else {
                        Iterator<String> baBucketKeys = baBucket.keys();
                        System.out.println("bucketsAddedMap: " + bucketsAddedMap);
                        while(baBucketKeys.hasNext()) {
                            String baBucketDefaultKey = baBucketKeys.next();
                            JSONObject baBucketObj = baBucket.get(baBucketDefaultKey);
                            addDefaultProducts(baBucketObj, baBucketDefaultKey, billingAct, billingAccountExternalId, bAEntity);    
                        }
                    }
                }  
            }
        }

        System.out.println("Billing Adj Bucket: " + bAEntity);
        return bAEntity;
    }

    private void addDefaultProducts(JSONObject baBucketObj, String baBucketDefaultKey, JSONObject billingAct, String billingAccountExternalId, JSONArray bAEntity) {
        
        JSONObject targetBillingAccRecord = new JSONObject();
        JSONObject amount = new JSONObject();

        String targetBillingAccBucketType = baBucketObj.get("Targetbillingaccountbuckettype");
        String targetBillingAccountBucketMin = baBucketObj.get("Value1");
        String targetBillingAccountBucketMax = baBucketObj.get("Value2");
        String targetBillingAccountAction = baBucketObj.get("Value3");
        String targetDecimal = baBucketObj.get("TargetDecimal");
        String targetUnit = baBucketObj.get("TargetUnit");

        if(targetBillingAccBucketType.equalsIgnoreCase("asc")) {
            long amountTarget = Long.parseLong(targetBillingAccountBucketMax);
            int decimalPlaces = Integer.parseInt(targetDecimal);
            amount.put("number", amountTarget);
            amount.put("decimalPlaces", decimalPlaces);
            targetBillingAccRecord.put("amount",amount);
        } else if(targetBillingAccBucketType.equalsIgnoreCase("desc")) {
            long amountTarget = Long.parseLong(targetBillingAccountBucketMin);
            int decimalPlaces = Integer.parseInt(targetDecimal);
            amount.put("number", amountTarget);
            amount.put("decimalPlaces", decimalPlaces);
            targetBillingAccRecord.put("amount",amount);
        }
        targetBillingAccRecord.put("action",targetBillingAccountAction);
        targetBillingAccRecord.put("billingAccountBucketSpecExternalId",baBucketDefaultKey);
        targetBillingAccRecord.put("billingAccountExternalId",billingAccountExternalId);
        targetBillingAccRecord.put("customerExternalId",billingAct.get("customerexternalid"));

        //Setting current time as trigger time for SET operation
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        targetBillingAccRecord.put("triggerTime",getFormattedDate(sdf.format(new Date())));

        //Setting validFor
        if(billingAct.has("statusstartdatetime")) {
            JSONObject validFor = getValidFor(getFormattedDate(billingAct.get("statusstartdatetime")));
            targetBillingAccRecord.put("validFor", validFor);
        }

        targetBillingAccRecord.put("unitOfMeasure",targetUnit);
        targetBillingAccRecord.put("origin","default");
        bAEntity.put(targetBillingAccRecord);
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
