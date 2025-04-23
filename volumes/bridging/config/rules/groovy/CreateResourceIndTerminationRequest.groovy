package groovy;

import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.*;
import javax.net.ssl.*;
import java.net.URL;
import java.net.URI;
import java.net.HttpURLConnection;
import java.util.Optional;
import java.security.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.io.*;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.security.cert.CertificateException;
import java.io.IOException;
import org.springframework.http.HttpMethod;

import com.ericsson.datamigration.bridging.commons.core.util.IOUtils;
import com.ericsson.datamigration.bridging.rocksdb.ExtIdInfo;
import com.ericsson.datamigration.bridging.rocksdb.RocksDBRepository;

import com.ericsson.datamigration.log.dto.KafkaMsgIOLogBuilder;
import com.jsoniter.output.JsonStream;
import com.ericsson.datamigration.log.utils.LogUtil;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;

import org.apache.http.client.methods.HttpPatch;


import com.ericsson.datamigration.bss.transformation.utils.ResourceHandler;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;
import java.util.Calendar;


import java.nio.charset.StandardCharsets;


class CreateResourceIndTerminationRequest {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateResourceIndTerminationRequest.class);
	private JSONObject jsonObject = new JSONObject();

	private AtRulesConstant atrules = new AtRulesConstant();


	def String execute(String input){
		LOGGER.debug("executing groovy scripts: input {} ",input);
		JSONObject inputJson = new JSONObject(input);
		String chunkId = inputJson.optString("chunk_id");
		String luwId = inputJson.optString("luw_id");

		JSONObject inputBody = inputJson.getJSONObject("BODY");
		
		JSONObject party = (JSONObject)inputBody.getJSONArray("stg_beam_party").get(0);
		JSONArray partyRoleArray = inputBody.getJSONArray("stg_beam_party_role");
		JSONObject customer = (JSONObject)inputBody.getJSONArray("stg_beam_customer").get(0);
		JSONArray organizationParty = inputBody.getJSONArray("stg_beam_party");
		JSONObject contract = (JSONObject)inputBody.getJSONArray("stg_beam_contract").get(0);
		
		
		String operation = inputJson.optString("action");
	
		jsonObject.put("chunk_id",chunkId);
		jsonObject.put("luw_id",luwId);

		contractTermination(chunkId,luwId,contract);
		billingAccountTermination(chunkId,luwId,contract);
		customerTermination(chunkId,luwId,customer);
		partyTermination(chunkId,luwId,party);

		
		//deleteData(chunkId, luwId, inputJson);
		LOGGER.debug("Delete output {}", jsonObject);
		return jsonObject.toString();

	}
	private void partyTermination(String chunkId, String luwId, JSONObject party) throws Exception
	{
		
		String beam_protocol = "http";

		if(party != null){
			  	
		try
		{
			String hostport = atrules.BEAM_HOSTPORT;

				String partyExtId = party.optString("partyexternalid");

				//String uri = IOUtils.getBridgingProperty("beam.party.delete.uri");
				String uri = atrules.BEAM_PARTY_DELETE_URI;

				uri = uri.replaceAll("#1",partyExtId);
				//uri = "http://"+hostport+uri;
				uri = beam_protocol+"://"+hostport+uri;
			
			executeTermination(uri, "party", partyExtId, chunkId, luwId, "PartyTerminated");
			
		}
		catch (Exception e)
		{
			LOGGER.error("BEAM_DELETE Failed Exception {}",e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}
		}
	
	}

		private void customerTermination(String chunkId, String luwId, JSONObject customer) throws Exception
	{
		
		String beam_protocol = "http";

		if(customer != null){
			  	
		try
		{
			String hostport = atrules.BEAM_HOSTPORT;

				String customerExtId = customer.optString("customerexternalid");

				//String uri = IOUtils.getBridgingProperty("beam.party.delete.uri");
				String uri = atrules.BEAM_CUSTOMER_TERMINATE_URI;

				uri = uri.replaceAll("#1",customerExtId);
				//uri = "http://"+hostport+uri;
				uri = beam_protocol+"://"+hostport+uri;
			
			executeTermination(uri, "customer", customerExtId, chunkId, luwId, "CustomerTerminated");
			
		}
		catch (Exception e)
		{
			LOGGER.error("BEAM_DELETE Failed Exception {}",e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}
		}
	
	}

	private void billingAccountTermination(String chunkId, String luwId, JSONObject contract) throws Exception
	{
		
		String beam_protocol = "http";

		if(contract != null){
			  	
		try
		{
			String hostport = atrules.BEAM_HOSTPORT;

				String customerExtId = contract.optString("customerexternalid");
				String billingExt = contract.optString("billingaccountref");

				//String uri = IOUtils.getBridgingProperty("beam.party.delete.uri");
				String uri = atrules.BEAM_CUSTOMER_TERMINATE_URI;

				uri = uri.replaceAll("#1",customerExtId);
				//uri = "http://"+hostport+uri;
				uri = beam_protocol+"://"+hostport+uri;
			
			executeTermination(uri, "BillingAccount", billingExt, chunkId, luwId, "BillingAccountTerminated");
			
		}
		catch (Exception e)
		{
			LOGGER.error("BEAM_DELETE Failed Exception {}",e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}
		}
	
	}

	private void contractTermination(String chunkId, String luwId, JSONObject contract) throws Exception
	{
		
		String beam_protocol = "http";

		if(contract != null){
			  	
		try
		{
			String hostport = atrules.BEAM_HOSTPORT;

				String customerExtId = contract.optString("customerexternalid");
				String contractExtId = contract.optString("contractexternalid");

				//String uri = IOUtils.getBridgingProperty("beam.party.delete.uri");
				String uri = atrules.BEAM_CONTRACT_DELETE_URI;

				uri = uri.replaceAll("#1",customerExtId);
				uri = uri.replaceAll("#2",contractExtId);
				//uri = "http://"+hostport+uri;
				uri = beam_protocol+"://"+hostport+uri;
			
			executeTermination(uri, "contract", contractExtId, chunkId, luwId, "Terminated");
			
		}
		catch (Exception e)
		{
			LOGGER.error("BEAM_DELETE Failed Exception {}",e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}
		}
	
	}

	private int executeTermination (String uri, String entityName, String extId, String chunkId, String luwId,String action) throws Exception
	{
		KafkaMsgIOLogBuilder logBuilderInput = null;
                KafkaMsgIOLogBuilder logBuilderOutput = null;

       JSONArray account = new JSONArray();
	   JSONObject accountObj = new JSONObject();
		//------Request status Start -----------------//
		if(entityName.equals("BillingAccount")){
		 accountObj.put("externalId",extId);
		}
		JSONObject req = new JSONObject();
		JSONObject status = new JSONObject();
		status.put("status",action);
		JSONArray statusArr = new JSONArray();
		statusArr.put(status);

		if(entityName.equals("BillingAccount")){
		 accountObj.put("status",statusArr);
		 account.put(accountObj);
		 req.put("account",account);
		}
		else{
         req.put("status",statusArr);
		}

		
		//LOGGER.debug("Request Payload-----: "+req);
		//------Request status End -----------------//

		int respCode = 0;
		JSONObject jsonEle = new JSONObject();
                JSONObject jsonRequestPayload = new JSONObject();

		try{
			HttpPatch pm = new HttpPatch(uri);
			pm.setHeader("Accept", "application/json");
			pm.setHeader("Content-Type", "application/json");
			//pm.setHeader("ERICSSON.Cascade-Termination","true");
			HttpEntity httpEntity = new ByteArrayEntity(req.toString().getBytes("UTF-8"));
			pm.setEntity(httpEntity);
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse httpResponse = httpClient.execute(pm);

			println(httpResponse.getStatusLine().getStatusCode());
			//println(httpResponse.getStatusLine().getReasonPhrase());
			//println(httpResponse.getEntity().getContent().toString());
         // println(httpResponse.getEntity().getContent().toString());


			respCode = httpResponse.getStatusLine().getStatusCode();
			if(respCode == 200 || respCode == 204)
			{
              //jsonEle.put("requestPayload",req);
				jsonEle.put("status", entityName + " Termination successful");
				jsonEle.put("responseCode", respCode);
				jsonEle.put("externalId", extId);
                                jsonRequestPayload.put("requestPayload",req);
                                
				jsonObject.put(entityName, jsonEle);
				//LOGGER.debug("BEAM_DELETE Success {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilderOutput = new KafkaMsgIOLogBuilder(chunkId,luwId,"SUCCESS","IoT_DELETE_RESPONSE");
                                logBuilderInput = new KafkaMsgIOLogBuilder(chunkId,luwId,"SUCCESS","IoT_DELETE_REQUEST");

			}
			else
			{
                                jsonRequestPayload.put("requestPayload",req);

				jsonEle.put("status",entityName+ " Termination failed");

				jsonEle.put("responseCode", respCode);
				jsonEle.put("externalId", extId);
				jsonObject.put(entityName, jsonEle);
				//LOGGER.debug("BEAM_DELETE Failed {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilderOutput = new KafkaMsgIOLogBuilder(chunkId,luwId,"FAILED","IoT_DELETE_RESPONSE");
                logBuilderInput= new KafkaMsgIOLogBuilder(chunkId,luwId,"SUCCESS","IoT_DELETE_REQUEST");

			}

			LogUtil.logMsgJsonStr(LOGGER, JsonStream.serialize(logBuilderOutput), jsonEle.toString());
                        LogUtil.logMsgJsonStr(LOGGER, JsonStream.serialize(logBuilderInput), jsonRequestPayload.toString());


		}
		catch (Exception e){
			LOGGER.error("BEAM_DELETE Failed {} {} {}",luwId,chunkId,e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}

		return respCode;

	}


	private String getTerminationFormattedDate()
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		formatter.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
		String strDate = formatter.format(new Date());

		LOGGER.debug("Date:----"+strDate);
		Date dateObj = (Date)formatter.parse(strDate);

		Calendar c = Calendar.getInstance();
		c.setTime(dateObj);
		c.add(Calendar.MINUTE, 10);
		dateObj = c.getTime();
		String dateStr = formatter.format(dateObj)

		return dateStr;

	}

	private void deleteData(String chunkId, String luwId, JSONObject inputJson) throws Exception
	{
		JSONObject inputBody = inputJson.getJSONObject("BODY");
		JSONObject massResource = (JSONObject)inputBody.getJSONArray("stg_beam_resource").get(0);
		JSONObject party = (JSONObject)inputBody.getJSONArray("stg_beam_party").get(0);
		JSONArray partyRoleArray = inputBody.getJSONArray("stg_beam_party_role");
		JSONObject customer = (JSONObject)inputBody.getJSONArray("stg_beam_customer").get(0);
		JSONArray organizationParty = inputBody.getJSONArray("stg_beam_party");

		try
		{
			String hostport = atrules.BEAM_HOSTPORT;
			String beam_protocol = "http";

			if(atrules.IOT_BEAM_PROTOCOL.equalsIgnoreCase("https-K") || atrules.IOT_BEAM_PROTOCOL.equalsIgnoreCase("https-A")) {
				beam_protocol = "https";
			}

			if(massResource != null)
			{
				String contExtID = massResource.get("contractexternalid");
				String custExtID = massResource.get("customerexternalid");

				String uri = atrules.BEAM_MASS_RESOURCE_DELETE_URI;
				uri = uri.replaceAll("#1",custExtID);
				uri = uri.replaceAll("#2",contExtID);
				uri = beam_protocol+"://"+hostport+uri;

				executeDelete(uri, "massResource", massResource,  chunkId, luwId);
			}

		}
		catch (Exception e)
		{
			LOGGER.error("BEAM_DELETE Failed Exception {}",e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	private int executeDelete(String uri, String entityName, JSONObject massResource, String chunkId, String luwId) throws Exception
	{

		KafkaMsgIOLogBuilder logBuilder = null;
		JSONObject mapping = ResourceHandler.getResourceAsJSON("mapping.json");

		//------Request status Start -----------------//
		JSONObject resource = new JSONObject();
		JSONObject req = new JSONObject();
		//JSONObject validFor = getValidFor(getTerminationFormattedDate());
		
		//Add status to req
		JSONObject status = new JSONObject();
		status.put("status","MassResourceIdTerminated");
		status.put("metaStateId",1004);
		JSONArray statusArr = new JSONArray();
		statusArr.put(status);
		req.put("statuses",statusArr);

		//Add massResourceIdIdentifierObj to req
		JSONArray massResourceIdIdentifier = new JSONArray();
		JSONObject massResourceIdIdentifierObj = new JSONObject();
		massResourceIdIdentifierObj.put("resourceNumber",massResource.get("resourcenumber"));
		//massResourceIdIdentifierObj.put("externalId",massResource.get("resourceextid"));
		massResourceIdIdentifier.put(massResourceIdIdentifierObj);
		req.put("massResourceIdIdentifier", massResourceIdIdentifier);

		//Add massResourceIdSpec to req
		JSONObject massResourceIdSpec = new JSONObject();
		JSONObject resourceSpecificationExternalIdMapping = mapping.get('lookup').get('resourceSpecificationExternalId');
		String id = getFromMapping(resourceSpecificationExternalIdMapping, massResource, 'resourcespecificationexternalid')
		massResourceIdSpec.put("resourceSpecId",id );
		req.put("massResourceIdSpec", massResourceIdSpec);
		resource.put("resource",req);
		LOGGER.debug("URI -----: "+uri);
		LOGGER.debug("Request Payload-----: "+resource);
		//------Request status End -----------------//

		int respCode = 0;
		JSONObject jsonEle = new JSONObject();
		try{
			HttpPost pm = new HttpPost(uri);
			//pm.setHeader("Accept", "application/json");
			//pm.setHeader("ERICSSON.Cascade-Termination","true");
			pm.setHeader("Content-Type", "application/json");
			HttpEntity httpEntity = new ByteArrayEntity(resource.toString().getBytes("UTF-8"));
			pm.setEntity(httpEntity);
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse httpResponse = httpClient.execute(pm);

			println(httpResponse.getStatusLine().getStatusCode());
			println(httpResponse.getStatusLine().getReasonPhrase());
			println(httpResponse.getEntity().getContent().toString());

			respCode = httpResponse.getStatusLine().getStatusCode();
			if(respCode == 200 || respCode == 204)
			{
				jsonEle.put("status", "IoT Mass Resource Deleted");
				jsonEle.put("responseCode", respCode);
				//jsonEle.put("externalId", httpResponse.getEntity().getContent().get("massResourceIdIdentifier").get(0));
				jsonObject.put(entityName, jsonEle);
				LOGGER.debug("BEAM_DELETE Success {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"SUCCESS","BEAM_DELETE");
			}
			else
			{
				jsonEle.put("status", "IoT Mass Resource Delete Failed");
				jsonEle.put("responseCode", respCode);
				//jsonEle.put("externalId", extId);
				jsonObject.put(entityName, jsonEle);
				//LOGGER.debug("BEAM_DELETE Failed {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"FAILED","BEAM_DELETE");
			}

			LogUtil.logMsgJsonStr(LOGGER, JsonStream.serialize(logBuilder), jsonEle.toString());

		}
		catch (Exception e){
			LOGGER.error("BEAM_DELETE Failed {} {} {}",luwId,chunkId,e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}

		return respCode;

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

	private static SSLContext createInsecureSSLContext() {
		try {
			// Create a trust manager that trusts all certificates
			TrustManager[] trustAllCerts = [new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
			}
			] as TrustManager[];

			// Create an "insecure" SSL context
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			return sslContext;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static class InsecureClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

		private SSLContext sslContext;
		private HttpURLConnection connection;

		public InsecureClientHttpRequestFactory(SSLContext sslContext) {
			this.sslContext = sslContext;
		}

		@Override
		protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
			if (connection instanceof HttpsURLConnection) {
				((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
				((HttpsURLConnection) connection).setHostnameVerifier(new InsecureHostnameVerifier());
			}
			super.prepareConnection(connection, httpMethod);
			this.connection = connection;
		}

		public HttpURLConnection getConnection() {
			return connection;
		}
	}

	private static class InsecureHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			// Bypass hostname verification and trust all hostnames
			return true;
		}
	}

}
