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

import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;
import java.util.Calendar;


import java.nio.charset.StandardCharsets;


class CreateZHierarchyTerminationRequest {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateZHierarchyTerminationRequest.class);
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
		
		String operation = inputJson.optString("action");
	
		jsonObject.put("chunk_id",chunkId);
		jsonObject.put("luw_id",luwId);
		//String cascadeTermination = IOUtils.getBridgingProperty("beam.delete.cascade.termination");
		String cascadeTermination = atrules.BEAM_DELETE_CASCADE_TERMINATION;
		
		//cascadeTerminationData(chunkId, luwId, inputJson);
		
		
		partyRoleTermination(chunkId,luwId,partyRoleArray,"Group");
		customerTermination(chunkId,luwId,customer);
		partyRoleTermination(chunkId,luwId,partyRoleArray,"IIR");
		partyRoleTermination(chunkId,luwId,partyRoleArray,"PR");
		
		//OrganizationPartyTermination()
		
		//Find highest level 
		 int maxLevel = 0;
		
		for (int i = 0; i < organizationParty.length(); i++) {
			JSONObject organizationPartyObj = organizationParty.getJSONObject(i);
			String hierarchylevel = organizationPartyObj.optString("hierarchylevel");
			int level=Integer.parseInt(hierarchylevel);
			
			if(level>maxLevel) {
			 maxLevel = level
			}
			
		}
		
		for(int i=maxLevel ; i>0 ;i--){
		  OrganizationPartyTermination(chunkId,luwId,organizationParty,i);
		}

		

		LOGGER.debug("Delete output {}", jsonObject);
		return jsonObject.toString();
	}

	
	private void partyRoleTermination(String chunkId, String luwId, JSONArray partyRoleArray ,String groupType) throws Exception
	{
		
		String beam_protocol = "http";
		
		for (int i = 0; i < partyRoleArray.length(); i++) {
			JSONObject partyRoleObj = partyRoleArray.getJSONObject(i);

		if(partyRoleObj.optString("partyroletype").equals(groupType)){
			  	
		try
		{
			String hostport = atrules.BEAM_HOSTPORT;

				String partyRoleExtID = partyRoleObj.optString("partyroleextid");

				//String uri = IOUtils.getBridgingProperty("beam.party.delete.uri");
				String uri = atrules.BEAM_PARTY_ROLE_TERMINATE_URI;

				uri = uri.replaceAll("#1",partyRoleExtID);
				//uri = "http://"+hostport+uri;
				uri = beam_protocol+"://"+hostport+uri;
			
			//PartyRole Changing status to Incative
			
			   executeTermination(uri, "partyRole", partyRoleExtID, chunkId, luwId, "PartyRoleInactive");
			   
			//PartyRole Changing status from Inactive to terminated
			
			executeTermination(uri, "partyRole", partyRoleExtID, chunkId, luwId, "PartyRoleTerminated");
				

			
		}
		catch (Exception e)
		{
			LOGGER.error("BEAM_DELETE Failed Exception {}",e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}
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
	
	private void OrganizationPartyTermination(String chunkId, String luwId, JSONArray organizationParty,int level) throws Exception
	{
		
		String beam_protocol = "http";
		
		for (int i = 0; i < organizationParty.length(); i++) {
			JSONObject organizationPartyObj = organizationParty.getJSONObject(i);
			
			int hierarchylevel = Integer.parseInt(organizationPartyObj.optString("hierarchylevel"));
		

		if(hierarchylevel == level){	  	
		try
		{
			String hostport = atrules.BEAM_HOSTPORT;

				String partyexternalid = organizationPartyObj.optString("partyexternalid");

				//String uri = IOUtils.getBridgingProperty("beam.party.delete.uri");
				String uri = atrules.BEAM_ORGANIZATION_PARTY_TERMINATE_URI;

				uri = uri.replaceAll("#1",partyexternalid);
				//uri = "http://"+hostport+uri;
				uri = beam_protocol+"://"+hostport+uri;
			
			executeTermination(uri, "organizationParty", partyexternalid, chunkId, luwId, "OrganizationPartyTerminated");
			
		}
		catch (Exception e)
		{
			LOGGER.error("BEAM_DELETE Failed Exception {}",e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}
		
	}
	}
	}



	private int executeTermination (String uri, String entityName, String extId, String chunkId, String luwId,String action) throws Exception
	{
		KafkaMsgIOLogBuilder logBuilder = null;


		//------Request status Start -----------------//
		JSONObject req = new JSONObject();
		JSONObject status = new JSONObject();
		status.put("status",action);
		JSONArray statusArr = new JSONArray();
		statusArr.put(status);
		req.put("status",statusArr);
		LOGGER.debug("Request Payload-----: "+req);
		//------Request status End -----------------//

		int respCode = 0;
		JSONObject jsonEle = new JSONObject();
		try{
			HttpPatch pm = new HttpPatch(uri);
			pm.setHeader("Accept", "application/json");
			pm.setHeader("Content-Type", "application/json");
			pm.setHeader("ERICSSON.Cascade-Termination","true");
			HttpEntity httpEntity = new ByteArrayEntity(req.toString().getBytes("UTF-8"));
			pm.setEntity(httpEntity);
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse httpResponse = httpClient.execute(pm);

			println(httpResponse.getStatusLine().getStatusCode());
			println(httpResponse.getStatusLine().getReasonPhrase());
			println(httpResponse.getEntity().getContent().toString());

			respCode = httpResponse.getStatusLine().getStatusCode();
			if(respCode == 200 || respCode == 204)
			{
				jsonEle.put("status", entityName + " Termination successful");
				jsonEle.put("responseCode", respCode);
				jsonEle.put("externalId", extId);
				jsonObject.put(entityName, jsonEle);
				//LOGGER.debug("BEAM_DELETE Success {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"SUCCESS","BEAM_DELETE");
			}
			else
			{
				jsonEle.put("status",entityName+ " Termination failed");
				jsonEle.put("responseCode", respCode);
				jsonEle.put("externalId", extId);
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


}
