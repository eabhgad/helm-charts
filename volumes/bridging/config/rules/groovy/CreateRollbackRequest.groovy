package groovy

import com.ericsson.datamigration.bridging.commons.core.httputil.HttpCloudSSLContext
import com.ericsson.datamigration.bridging.commons.core.httputil.TokenUtils
import com.ericsson.datamigration.bridging.commons.core.util.IOUtils
import com.ericsson.datamigration.bss.transformation.utils.DateTimeUtil
import com.ericsson.datamigration.log.dto.KafkaMsgIOLogBuilder
import com.ericsson.datamigration.log.utils.LogUtil
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.ReadContext
import com.jsoniter.output.JsonStream
import org.apache.commons.lang3.time.DateUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPatch
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.net.ssl.*
import java.security.KeyStore
import java.security.SecureRandom
import java.text.SimpleDateFormat

class CreateRollbackRequest {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateRollbackRequest.class);
	private AtRulesConstant atrules = new AtRulesConstant();
	private JSONObject jsonObject = new JSONObject();

	def String execute(String input){
		LOGGER.debug("executing groovy scripts: input {} ",input);
		ReadContext readContext = JsonPath.parse(input);
		//String systemWideDelete = atrules.AUTOMATIC_ROLLBACK_SYSTEM_WIDE_DELETE;
		KafkaMsgIOLogBuilder logBuilder = null;
		
		String systemWideDelete="YES";
		
		println("---- Roll back request"+systemWideDelete);

		String chunkId = readContext.read("chunk_id");
		String luwId = readContext.read("luw_id")
		jsonObject.put("chunk_id", chunkId);
		jsonObject.put("luw_id", luwId);
		
		//Checking Party status
		String partyRespstat = readContext.read("party[0].success");
		
		if (!systemWideDelete.equalsIgnoreCase("YES")) {
			jsonObject = createEntities(readContext);
			LOGGER.debug("executing groovy scripts: output {}", jsonObject);
		}
		else {
			logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"SUCCESS","TE_ROLLBACK_IN");
			LogUtil.logMsgJsonStr(LOGGER, JsonStream.serialize(logBuilder), "Message consumed successfully for rollback");
			if (partyRespstat.equalsIgnoreCase("true")) {
				String partyExtId = readContext.read("party[0].response.externalId");
				//JSONObject partyObj = readContext.read("party[0]");
				//cascadeTerminationData(readContext.read("chunk_id"), readContext.read("luw_id"), partyObj);
				retrieveCascadeData(chunkId, luwId, partyExtId)
			}
			else {
				logBuilder.update("status","WARNING").update("logCode","TE_ROLLBACK_WARN");
				LogUtil.logMsgJsonStr(LOGGER, JsonStream.serialize(logBuilder), "Party doesn't exist for LUW, hence rollback is skipped");
			}
		}
		
		LOGGER.debug("executing groovy scripts: output {}", jsonObject);
		return jsonObject.toString();
	}
	
	private void cascadeTerminationData(String chunkId, String luwId, String partyExtId) throws Exception
	{
		
		String beam_protocol = "http";

		try
		{
			String hostport = atrules.BEAM_HOSTPORT;

			if(partyExtId != null)
			{
				//String partyExtId = party.optString("partyexternalid");

				//String uri = IOUtils.getBridgingProperty("beam.party.delete.uri");
				String uri = atrules.BEAM_PARTY_DELETE_URI;

				uri = uri.replaceAll("#1",partyExtId);
				//uri = "http://"+hostport+uri;
				uri = beam_protocol+"://"+hostport+uri;
				
				executeCascadeTermination(uri, "party", partyExtId, chunkId, luwId);

			}
		}
		catch (Exception e)
		{
			LOGGER.error("ROLLBACK_SYSTEM_WIDE_DELETE Failed Exception {}",e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}
	}
	
	private int executeCascadeTermination (String uri, String entityName, String extId, String chunkId, String luwId) throws Exception
	{
		KafkaMsgIOLogBuilder logBuilder = null;


		//------Request status Start -----------------//
		JSONObject req = new JSONObject();
		//JSONObject validFor = getValidFor(getTerminationFormattedDate());
		JSONObject status = new JSONObject();
		status.put("status","PartyTerminated");
		//status.put("validFor",validFor);
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
				jsonEle.put("status", "System wide Termination successful");
				jsonEle.put("responseCode", respCode);
				jsonEle.put("externalId", extId);
				jsonObject.put(entityName, jsonEle);
				//LOGGER.debug("ROLLBACK_SYSTEM_WIDE_DELETE Success {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"SUCCESS","ROLLBACK_SYSTEM_WIDE_DELETE");
			}
			else
			{
				jsonEle.put("status", "Cascade Termination failed");
				jsonEle.put("responseCode", respCode);
				jsonEle.put("externalId", extId);
				jsonObject.put(entityName, jsonEle);
				//LOGGER.debug("ROLLBACK_SYSTEM_WIDE_DELETE Failed {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"FAILED","ROLLBACK_SYSTEM_WIDE_DELETE");
			}

			LogUtil.logMsgJsonStr(LOGGER, JsonStream.serialize(logBuilder), jsonEle.toString());

		}
		catch (Exception e){
			LOGGER.error("ROLLBACK_SYSTEM_WIDE_DELETE Failed {} {} {}",luwId,chunkId,e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}

		return respCode;

	}


	private JSONObject createEntities(ReadContext readContext) {

		JSONObject output = new JSONObject();
		output.put("chunk_id", readContext.read("chunk_id"));
		output.put("luw_id", readContext.read("luw_id"));
		
		String startDateTime = DateTimeUtil.getDateFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").formatTime(new Date());
		String endDateTime = DateTimeUtil.getDateFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").formatTime( DateUtils.addMinutes(new Date(),5));

		
		//Creating Party Payload
		String partyRespstat = readContext.read("party[0].success");
		if(partyRespstat.equalsIgnoreCase("true"))
		{
			JSONObject partyRecord = new JSONObject();
			JSONArray partyEntity = new JSONArray();
			partyRecord.put("externalId",readContext.read("party[0].response.externalId"));
			
			
			String[] statusMsg = new String[2];
			statusMsg[0] = "PartyInactive";
			statusMsg[1] = "PartyTerminated";	
			
			
			JSONArray statuses = getStatusArray(statusMsg, startDateTime,endDateTime );
			partyRecord.put("status", statuses);
			
			partyEntity.put(partyRecord);
			output.put("party", partyEntity);
		}
		
		//Creating Customer Payload
		String custRespstat = readContext.read("customer[0].success");
		if(custRespstat.equalsIgnoreCase("true"))
		{
			JSONObject custRecord = new JSONObject();
			JSONArray custEntity = new JSONArray();
			
			custRecord.put("externalId",readContext.read("customer[0].response.externalId"));
			custRecord.put("engagedPartyExternalId",readContext.read("customer[0].response.engagedPartyExternalId"));
			
			String[] statusMsg = new String[2];
			statusMsg[0] = "CustomerInactive";
			statusMsg[1] = "CustomerTerminated";
						
			JSONArray statuses = getStatusArray(statusMsg, startDateTime,endDateTime );
			custRecord.put("status", statuses);
			
			//Creating account payload
			JSONArray accountEntity = new JSONArray();
			JSONObject accountRecord = new JSONObject();
			accountRecord.put("externalId",readContext.read("customer[0].response.account[0].externalId"));
			statusMsg[0] = "BillingAccountInactive";
			statusMsg[1] = "BillingAccountTerminated";
			
			statuses = getStatusArray(statusMsg, startDateTime,endDateTime );
			accountRecord.put("status", statuses);
			accountEntity.put(accountRecord);
			custRecord.put("account",accountEntity);
			//// account end
			
			
			custEntity.put(custRecord);
			output.put("customer", custEntity);
		}
		
		
		return output;
	}

	private JSONArray getStatusArray(String[] statusMsg, String startDateTime, String endDateTime) {
		JSONArray statuses = new JSONArray();
		
		for(int i=0;i < statusMsg.length; i++)
		{	
			JSONObject status = new JSONObject();
			status.put("status", statusMsg[i]);
			if(statusMsg[i].equalsIgnoreCase("PartyTerminated") || statusMsg[i].equalsIgnoreCase("CustomerTerminated")
						|| statusMsg[i].equalsIgnoreCase("BillingAccountTerminated"))
			{
				status.put("validFor", getValidFor(startDateTime,endDateTime, true));
			}
			else
			{
				status.put("validFor", getValidFor(startDateTime,endDateTime, false));
			}
			status.put("reason", "CPD")
			statuses.put(status);
		}
		
		return statuses;
	}
	
	private JSONObject getValidFor(String startDateTime, String endDateTime, boolean isOnlyStartDate) {
		JSONObject validFor = new JSONObject();
		if(isOnlyStartDate)
		{
			validFor.put("startDateTime", endDateTime);
		}
		else
		{
			validFor.put("startDateTime", startDateTime);
			validFor.put("endDateTime", endDateTime);
		}
		
		return validFor;
	}
	
		private void retrieveCascadeData(String chunkId, String luwId, String partyId) throws Exception {
		
		String[] prefixes = null;
		String token = "";
		String extid;
		String hostport = IOUtils.getBridgingProperty("beam.hostport");
		String connprotocol = IOUtils.getBridgingProperty("connection.protocol");
		
		String protocol = "http";
		if(connprotocol.equalsIgnoreCase("https-C")) {
			protocol = "https";
			TokenUtils tokenUtils = new TokenUtils();
			token = tokenUtils.getSecurityToken();
			LOGGER.debug("Fetched Token: {} ",token);
		} else if((connprotocol.equalsIgnoreCase("https-K") || connprotocol.equalsIgnoreCase("https")) && !isSSLSocketConfigured) {
			protocol = "https";
			setSSLSocketFactory();
		} else if(connprotocol.equalsIgnoreCase("https-A")) {
			protocol = "https";
		}
		try
		{
		
			//RocksDBRepository rocksDBRepository = RocksDBRepository.getRocksDBRepository();
			//Optional op = rocksDBRepository.find(chunkId+"_"+luwId);
			//if(op.isPresent())
			//{
				//ExtIdInfo extIdInfo = (ExtIdInfo)op.get();
				//prefixes = extIdInfo.getPrefix().split("###");
				//extid = extIdInfo.getExtId();
			//}

			//for(int i=0;prefixes!=null && i<prefixes.length;i++)
			//{
				//if(prefixes[i].startsWith("party"))
				//{
					//String partyExtID = party.optString("partyexternalid");
					//party.JSONObject("response").optString("partyexternalid");
					
					String uri = IOUtils.getBridgingProperty("beam.party.delete.uri");
					uri = uri.replaceAll("#1",partyId);
					uri = protocol+"://"+hostport+uri;
				    LOGGER.debug("used  uri: {} ",uri);
					executeCascadeDelete(uri, "party", partyId, chunkId, luwId, connprotocol, token);
				//}	
			//}
			
			
		} 
		catch (Exception e) 
		{
		   LOGGER.error("BEAM_DELETE Failed Exception {}",e.printStackTrace());
           e.printStackTrace();
		   throw new Exception(e);
        }
	}

	private int executeCascadeDelete(String uri, String entityName, String extId, String chunkId, String luwId, String connprotocol, String token) throws Exception {
		
		//HttpURLConnection connection = null;
		//HttpsURLConnection httpsConnection = null;
		JSONObject jsonEle = new JSONObject();
		int line = 0;
		KafkaMsgIOLogBuilder logBuilder = null;
		String accept = "application/json";

		LOGGER.debug("Request token -----: "+token);

		//------Request status start -------//
		JSONObject req = new JSONObject();
		JSONObject validFor = getValidFor(getTerminationFormattedDate());
		JSONObject status = new JSONObject();
		status.put("status","PartyTerminated");
		JSONArray statusArr = new JSONArray();
		statusArr.put(status);
		req.put("status",statusArr);
		LOGGER.debug("Request Payload-----: "+req);
		
		try {
			URL url = new URL(uri);

			LOGGER.debug("Request uri-----: "+uri);

			HttpPatch hp = new HttpPatch(uri);
			HttpResponse httpResponse;
			hp.setHeader("Accept", accept);
			hp.setHeader("ERICSSON.Cascade-Termination","true");
			HttpEntity httpEntity = new ByteArrayEntity(req.toString().getBytes("UTF-8"));
			hp.setEntity(httpEntity);

			if(connprotocol.equalsIgnoreCase("https-C")) {
				HttpCloudSSLContext cloudSSLContext = new HttpCloudSSLContext();
				SSLContext sslContext = cloudSSLContext.getSSLContext();
				hp.setHeader("Content-Type", "application/json");
				hp.setHeader("Authorization",token);
				HttpClientBuilder clientbuilder = HttpClients.custom();
				clientbuilder = clientbuilder.setSSLContext(sslContext);
				HttpClient httpClient = clientbuilder.build();
				httpResponse = httpClient.execute(hp);
			} else {
				HttpClient httpClient = new DefaultHttpClient();
				httpResponse = httpClient.execute(hp);
			}
			

			println(httpResponse.getStatusLine().getStatusCode());
			println(httpResponse.getStatusLine().getReasonPhrase());
			println(httpResponse.getEntity().getContent().toString());

			line = httpResponse.getStatusLine().getStatusCode();
			
			if(line == 200 || line == 204)
			{
				jsonEle.put("status", "Cascade delete successful");
				jsonEle.put("responseCode", line);
				jsonEle.put("externalId", extId);
				jsonObject.put(entityName, jsonEle);
				//LOGGER.debug("BEAM_DELETE Success {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"SUCCESS","TE_ROLLBACK_OUT");
			}
			else
			{
				jsonEle.put("status", "Cascade delete failed");
				jsonEle.put("responseCode", line);
				jsonEle.put("externalId", extId);
				jsonObject.put(entityName, jsonEle);
				//LOGGER.debug("BEAM_DELETE Failed {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"FAILED","TE_ROLLBACK_OUT");
			}
			LogUtil.logMsgJsonStr(LOGGER, JsonStream.serialize(logBuilder), jsonEle.toString());
			
		} catch (Exception e){
			LOGGER.error("BEAM_DELETE Failed {} {} {}",luwId,chunkId,e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}
		
		return line;
	
	}
	
	private JSONObject getValidFor(Object val) {
		JSONObject startDateTime = new JSONObject();
		startDateTime.put("startDateTime", val);
		return startDateTime;
	}
	
	private String getTerminationFormattedDate()
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		formatter.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
		String strDate = formatter.format(new Date());

		LOGGER.debug("Date:   "+strDate);
		Date dateOb = (Date)formatter.parse(strDate);
		
		Calendar c = Calendar.getInstance();
		c.setTime(dateOb);
		c.add(Calendar.MINUTE, 10);
		dateOb = c.getTime();		
		String dateStr = formatter.format(dateOb)
		
		return dateStr;
	}

	private void setSSLSocketFactory()
	{
		LOGGER.debug("Configuring SSL Socket...");
		try
		{
			KeyStore keyStore = KeyStore.getInstance(IOUtils.getBridgingProperty("ssl.keystore.type"));
			FileInputStream fiskt = new FileInputStream(IOUtils.getBridgingProperty("ssl.keystore"));
			keyStore.load(fiskt, IOUtils.getBridgingProperty("ssl.keystore.password").toCharArray());
			
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, IOUtils.getBridgingProperty("ssl.keystore.password").toCharArray());
			KeyManager[] kms = kmf.getKeyManagers();
			
			KeyStore trustStore = KeyStore.getInstance(IOUtils.getBridgingProperty("ssl.truststore.type"));
			FileInputStream fistt = new FileInputStream(IOUtils.getBridgingProperty("ssl.truststore"));
			trustStore.load(fistt, IOUtils.getBridgingProperty("ssl.truststore.password").toCharArray());
			
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trustStore);
			TrustManager[] tms = tmf.getTrustManagers();
			
			SSLContext sslContext = SSLContext.getInstance(IOUtils.getBridgingProperty("ssl.type"));
			sslContext.init(kms, tms, new SecureRandom());
		
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			isSSLSocketConfigured = true;
		}catch (Exception e){
			LOGGER.error("BEAM_DELETE Failed {}",e.printStackTrace());
			e.printStackTrace();
		}
		
	}
}
