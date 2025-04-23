package groovy

import org.json.JSONObject
import org.json.JSONArray

import com.ericsson.datamigration.bss.wfm.input.JSONInputData
import com.ericsson.datamigration.bridging.commons.core.util.DateUtils

/*
 * This class is responsible to create the Event Dispatched Started Event Request
 */
class EDStartedEventRequest {

	/*
	 * This method is responsible to construct the Started event to notify the event dispatched
	 * Input: List<Object>
	 * 	0- SourceData
	 * 	1- Workflowcontext
	 * Output:
	 * 	JSON String with required parameter for started event
	 */
	def String prepareStartedEventRequest(Object input) {
		List<Object> params = (List<Object>) input;

		JSONInputData converterInputData = (JSONInputData)params.get(0);
		JSONObject jsonInputValue = (JSONObject)converterInputData.getInputValues();
		JSONObject inputData = (JSONObject)jsonInputValue.get("inputData");

//		String correlationId = inputData.get("correlationId");
//		String orderId = "emt_"+correlationId;

		JSONObject requestObj = new JSONObject();
		requestObj.put("eventKey","emt.service.migration.started.v2.event");
		requestObj.put("routingKey","emt.service.migration.started.v2.event");
//		requestObj.put("correlationId", correlationId);
		requestObj.put("resourceURI", "/puente/v1/workflow/migration");
		requestObj.put("timestamp", DateUtils.getFormattedCurrentDateInUTCTimeZone(DateUtils.YYYY_MM_DD_T_HH_MM_SSZ_FORMAT));
		JSONObject dataObj = new JSONObject();
		dataObj.put("serviceNumber",inputData.get("serviceNumber"));
//		dataObj.put("packageId",inputData.get("packageId"));
//		dataObj.put("orderId",orderId);
//		dataObj.put("brand",inputData.get("brand"));
//		dataObj.put("commercialOffer",inputData.get("newPlan"));
//		dataObj.put("transitionType",inputData.get("transitionType"));
		dataObj.put("migrationType","True");
		dataObj.put("orderScenario","Activation");
		dataObj.put("subscriptionType","Prepaid");
		requestObj.put("data", dataObj);

		return requestObj.toString();
	}
}