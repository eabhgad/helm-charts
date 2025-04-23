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

public class AdjustmentProductEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdjustmentProductEntity.class);


    public JSONArray productAdjEntity(JSONArray stgBeamBalance, JSONObject mapping, JSONArray productArray, JSONObject customer) {
              
        JSONArray productEntity = new JSONArray();
        JSONObject productBucketMapping = mapping.get("product_bucket");
        JSONObject productsMapping = mapping.get("products");
        Map<String,List<String>> bucketsAddedMap = new HashMap<String,List<String>>(); 

        for(JSONObject balanceBucket : stgBeamBalance) {
            if(balanceBucket.get("bucketadjustmenttype").equalsIgnoreCase("pb_bucket")) {
                String srcBuckExtId = balanceBucket.get("productexternalid");
                String srcBuckSpecExtId = balanceBucket.get("bucketspecexternalid");
                JSONObject targetProdOfferingRecord = new JSONObject();

                if(productBucketMapping.has(srcBuckSpecExtId)) {
                    JSONObject prodOffSpecExtIdMapping = productBucketMapping.get(srcBuckSpecExtId).get("productofferingexternalid");

                    String srcProdSpecExtId = null;
                    for(JSONObject srcProduct : productArray) {
                        if(srcBuckExtId.equals(srcProduct.get("productexternalid"))){
                            srcProdSpecExtId = srcProduct.get("productofferingexternalid");
                        }
                    }

                    if(prodOffSpecExtIdMapping.has(srcProdSpecExtId)) {
                        JSONObject prodOffBuckObj = prodOffSpecExtIdMapping.get(srcProdSpecExtId);
                        System.out.println("Prod Off Bucket Obj: " + prodOffBuckObj);

                        // Fetching respective product ext id from mapping
                        String prodExternalId = productsMapping.get(srcProdSpecExtId).get("external_id");
                        System.out.println("Product External Id: " + prodExternalId);
                        if(!bucketsAddedMap.containsKey(prodExternalId)) {
                            bucketsAddedMap.put(prodExternalId, new ArrayList<String>());
                        }
                        bucketsAddedMap.get(prodExternalId).add(prodOffBuckObj.get("ProductBucket"));

                        // Creating the product bucket
                        String prodOffBucketSpecExtId = prodOffBuckObj.get("ProductBucket");
                        String sourceProdBucketType = prodOffBuckObj.get("sourceproductbuckettype");
                        String targetProdBucketType = prodOffBuckObj.get("Targetproductbuckettype");
                        long targetProdBucketMin = Long.parseLong(prodOffBuckObj.get("Value1"));
                        long targetProdBucketMax = Long.parseLong(prodOffBuckObj.get("Value2"));
                        String targetProdBucketAction = prodOffBuckObj.get("Value3");

                        String targetUnit = prodOffBuckObj.get("TargetUnit");
                        Double multiplicationFactor = Double.parseDouble(mapping.get("unit").get(balanceBucket.optString("unitofmeasure")).get("MultiplicationFactor"));
                        int srcProdBucketAmt = balanceBucket.get("amount");
                        int decimalPlaces= Integer.parseInt(prodOffBuckObj.get("TargetDecimal"));
                        Double amountConversion = srcProdBucketAmt*multiplicationFactor;
                        double power = Math.pow(10, decimalPlaces);
                        long amountFinalAdj = amountConversion*power;

                        JSONObject amount = new JSONObject();

                        if(srcProdBucketAmt >= 0) {
                            if(sourceProdBucketType.equalsIgnoreCase(targetProdBucketType)) {
                                amount.put("number", amountFinalAdj);
                            } else {
                                amount.put("number", targetProdBucketMax-amountFinalAdj);
                            }
                        } else {
                            if(sourceProdBucketType.equalsIgnoreCase("desc") && targetProdBucketType.equalsIgnoreCase("desc")) {
                                amount.put("number", amountFinalAdj);
                            }
                        }

                        amount.put("decimalPlaces", decimalPlaces);
                        targetProdOfferingRecord.put("amount",amount);
                        targetProdOfferingRecord.put("action",targetProdBucketAction);
                        targetProdOfferingRecord.put("bucketSpecExternalId",prodOffBucketSpecExtId);
                        targetProdOfferingRecord.put("productExternalId",srcBuckExtId);
                        targetProdOfferingRecord.put("triggerTime", getFormattedDate(balanceBucket.get("triggertime")));
                        //Setting validFor
                        if(balanceBucket.has("startdatetime")) {
                            JSONObject validFor = getValidFor(getFormattedDate(balanceBucket.get("startdatetime")));
                            targetProdBucket.put("validFor", validFor);
                        }

                        targetProdOfferingRecord.put("customerExternalId", balanceBucket.get("customerexternalid"));
                        targetProdOfferingRecord.put("contractExternalId", balanceBucket.get("contractexternalid"));
                        targetProdOfferingRecord.put("unitOfMeasure", targetUnit);
                        targetProdOfferingRecord.put("origin","source");
                        productEntity.put(targetProdOfferingRecord);
                    }
                }
            }
        }
        System.out.println("Product buckets added from Source: " + bucketsAddedMap);

        //Adding Default products to product bucket
        JSONObject pbDefaultMapping = mapping.get("pb_default");
        String customerExtId = customer.get("customerexternalid");

        for(JSONObject product : productArray) {
            String contractExtId = product.get("contractexternalid");
            String productExternalId = product.get("productexternalid");

            if(productsMapping.has(product.get("productofferingexternalid"))) {
                String pbDefaultExternalId = productsMapping.get(product.get("productofferingexternalid")).get("external_id");

                if(pbDefaultMapping.has(pbDefaultExternalId)) {
                    JSONObject prodBucketDefault = pbDefaultMapping.get(pbDefaultExternalId).get("ProductBucket");

                    //Check for pbDefaultExternalId itself not present in source bucket, all default needs to be added?
                    if(bucketsAddedMap.containsKey(pbDefaultExternalId)) {
                        Iterator<String> prodBucketDefaultKeys = prodBucketDefault.keys();
                        while(prodBucketDefaultKeys.hasNext()) {
                            String prodBucketDefaultKey = prodBucketDefaultKeys.next();
                            JSONObject prodBucketObj = prodBucketDefault.get(prodBucketDefaultKey);
                            System.out.println("bucketsAddedMap: " + bucketsAddedMap);
                            if(bucketsAddedMap.get(pbDefaultExternalId).contains(prodBucketDefaultKey)) {
                                System.out.println("Product Bucket [" + prodBucketDefaultKey + "] exists in source, skipping default addition");
                            } else {
                                addDefaultProducts(prodBucketObj, prodBucketDefaultKey, product, customerExtId, contractExtId, productEntity);
                            }
                        }
                    } else {
                        Iterator<String> prodBucketDefaultKeys = prodBucketDefault.keys();
                        System.out.println("bucketsAddedMap: " + bucketsAddedMap);
                        while(prodBucketDefaultKeys.hasNext()) {
                            String prodBucketDefaultKey = prodBucketDefaultKeys.next();
                            JSONObject prodBucketObj = prodBucketDefault.get(prodBucketDefaultKey);
                            addDefaultProducts(prodBucketObj, prodBucketDefaultKey, product, customerExtId, contractExtId, productEntity);    
                        }
                    }
                }
            }
        }

        System.out.println("Product Adj Bucket: " + productEntity);
        return productEntity;
    }


    private void addDefaultProducts(JSONObject prodBucketObj, String prodBucketDefaultKey, JSONObject product, String customerExtId, String contractExtId, JSONArray productEntity) 
    {    
        JSONObject targetProdBucket = new JSONObject();
        JSONObject amount = new JSONObject();
        String targetProdBucketType = prodBucketObj.get("Targetproductbuckettype");
        String targetProdBucketMin = prodBucketObj.get("Value1");
        String targetProdBucketMax = prodBucketObj.get("Value2");
        String targetProdBucketAction = prodBucketObj.get("Value3")
        String targetUnit = prodBucketObj.get("TargetUnit");
        String targetDecimal = prodBucketObj.get("TargetDecimal");

        if(targetProdBucketType.equalsIgnoreCase("asc")) {
            long amountTarget = Long.parseLong(targetProdBucketMax);
            int decimalPlaces = Integer.parseInt(targetDecimal);
            amount.put("number", amountTarget);
            amount.put("decimalPlaces", decimalPlaces);
            targetProdBucket.put("amount",amount);
        } else if(targetProdBucketType.equalsIgnoreCase("desc")) {
            long amountTarget = Long.parseLong(targetProdBucketMin);
            int decimalPlaces = Integer.parseInt(targetDecimal);
            amount.put("number", amountTarget);
            amount.put("decimalPlaces", decimalPlaces);
            targetProdBucket.put("amount",amount);
        }

        targetProdBucket.put("action",targetProdBucketAction);
        targetProdBucket.put("bucketSpecExternalId",prodBucketDefaultKey);
        targetProdBucket.put("productExternalId",product.get("productexternalid"));

        //Setting current time as trigger time for SET operation
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        targetProdBucket.put("triggerTime", getFormattedDate(sdf.format(new Date())));

        //Setting validFor
        if(product.has("statusvalidfrom")) {
            JSONObject validFor = getValidFor(getFormattedDate(product.get("statusvalidfrom")));
            targetProdBucket.put("validFor", validFor);
        }

        targetProdBucket.put("customerExternalId", customerExtId);
        targetProdBucket.put("contractExternalId", contractExtId);
        targetProdBucket.put("unitOfMeasure", targetUnit);
        targetProdBucket.put("origin","default");
        productEntity.put(targetProdBucket);
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
