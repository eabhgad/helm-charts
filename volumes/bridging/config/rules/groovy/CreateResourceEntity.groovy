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
class CreateResourceEntity {

	private static final Logger logger = LoggerFactory.getLogger(CreateResourceEntity.class);

	def JSONArray createResourceEntity(JSONArray resources, JSONObject mapping) {
		JSONObject resourceRecord = new JSONObject();
		JSONArray resourceArr = new JSONArray();
		JSONObject commonMapping = mapping.get("common");
		JSONObject lookupMapping = mapping.get("lookup");
		JSONObject  custSpec = lookupMapping.get("CustSpec");

		JSONObject resourceObj = resources.getJSONObject(0);

        JSONObject massResourceIdSpec = new JSONObject();
        
		
                JSONObject resourceSpecificationExternalIdMapping = mapping.get('lookup').get('resourceSpecificationExternalId');
                String id = getFromMapping(resourceSpecificationExternalIdMapping, resourceObj, 'resourcespecificationexternalid')
                
		massResourceIdSpec.put("resourceSpecId",id );
		resourceRecord.put("massResourceIdSpec", massResourceIdSpec);

		JSONArray massResourceIdIdentifier = new JSONArray();
                
                JSONArray statuses = new JSONArray();
                JSONObject statusObj= new JSONObject();
                //statusObj.put("status","MassResourceIdActive");
                //statusObj.put("metaStateId",1000);
                statusObj.put("status",resourceObj.get("status"));
                statusObj.put("metaStateId",Integer.parseInt(resourceObj.get("metastateid")));

                statuses.put(statusObj);
                resourceRecord.put("statuses",statuses);
 

               JSONObject massResourceIdIdentifierObj = new JSONObject();
		massResourceIdIdentifierObj.put("resourceNumber",resourceObj.get("resourcenumber"));
		massResourceIdIdentifierObj.put("externalId",resourceObj.get("resourceextid"));
		massResourceIdIdentifier.put(massResourceIdIdentifierObj);
		resourceRecord.put("massResourceIdIdentifier", massResourceIdIdentifier);
		
		resourceArr.put(resourceRecord);
		
		return resourceArr;
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

	private String getFormattedDate(String dateStr)
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date dateObj = formatter.parse(dateStr);

		dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(dateObj);

		return dateStr;

	}

}
