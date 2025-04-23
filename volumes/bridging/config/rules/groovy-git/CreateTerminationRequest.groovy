package groovy;

import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import java.util.Optional;
import com.ericsson.datamigration.bridging.commons.core.util.IOUtils;
import com.ericsson.datamigration.bridging.rocksdb.ExtIdInfo;
import com.ericsson.datamigration.bridging.rocksdb.RocksDBRepository;
import com.ericsson.datamigration.log.dto.KafkaMsgIOLogBuilder;

import com.ericsson.datamigration.bridging.commons.core.httputil.HttpCloudSSLContext;
import com.ericsson.datamigration.bridging.commons.core.httputil.TokenUtils;
import com.jsoniter.output.JsonStream;
import com.ericsson.datamigration.log.utils.LogUtil;

import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;
import java.util.Calendar;
import java.util.*;

class CreateTerminationRequest {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateTerminationRequest.class);
	private JSONObject jsonObject = new JSONObject();
	private static boolean isSSLSocketConfigured = false;
	

	def String execute(String input){
		LOGGER.debug("executing groovy scripts: input {} ",input);
		JSONObject inputJson = new JSONObject(input);
		String chunkId = inputJson.optString("chunk_id");
		String luwId = inputJson.optString("luw_id");
		jsonObject.put("chunk_id",chunkId);
		jsonObject.put("luw_id",luwId);
		
		JSONObject party = (JSONObject)inputJson.get("BODY").getJSONArray("stg_beam_party").get(0);
            
		//String cascadeTermination = IOUtils.getBridgingProperty("beam.delete.cascade.termination");
                 String cascadeTermination="YES";
		if(cascadeTermination.equalsIgnoreCase("YES")) 
		{
			retrieveCascadeData(chunkId,luwId,party);
		} else {
			retrieveData(chunkId, luwId);
		}

		LOGGER.debug("Delete output {}", jsonObject);
		return jsonObject.toString();
	}
	
	private void retrieveData(String chunkId, String luwId) throws Exception
	{
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
			//LOGGER.debug("Fetched Token: {} ",token);
		} else if((connprotocol.equalsIgnoreCase("https-K") || connprotocol.equalsIgnoreCase("https")) && !isSSLSocketConfigured) {
			protocol = "https";
			setSSLSocketFactory();
		} else if(connprotocol.equalsIgnoreCase("https-A")) {
			protocol = "https";
		}

		try
		{
		
			RocksDBRepository rocksDBRepository = RocksDBRepository.getRocksDBRepository();
			Optional op = rocksDBRepository.find(chunkId+"_"+luwId);
			if(op.isPresent())
			{
				ExtIdInfo extIdInfo = (ExtIdInfo)op.get();
				prefixes = extIdInfo.getPrefix().split("###");
				extid = extIdInfo.getExtId();
			}
			
			//Delete Contract
			for(int i=0;prefixes!=null && i<prefixes.length;i++)
			{
				if(prefixes[i].startsWith("cont"))
				{
					String contExtID = prefixes[i]+extid;
					String custExtID = "cust_"+extid;
					String uri = IOUtils.getBridgingProperty("beam.contract.delete.uri");
					uri = uri.replaceAll("#1",custExtID);
					uri = uri.replaceAll("#2",contExtID); 
					uri = protocol+"://"+hostport+uri;
					
					executeDelete(uri, "contract", contExtID,  chunkId, luwId, connprotocol, token);
										
				}
				
			}
			
			//Delete Customer
			for(int i=0;prefixes!=null && i<prefixes.length;i++)
			{
				if(prefixes[i].startsWith("cust"))
				{
					String custExtID = prefixes[i]+extid;
					
					String uri = IOUtils.getBridgingProperty("beam.customer.delete.uri");
					uri = uri.replaceAll("#1",custExtID);
					uri = protocol+"://"+hostport+uri;
					
					executeDelete(uri, "customer", custExtID,  chunkId, luwId, connprotocol, token);
					
				}
				
			}
			
			//Delete Party
			for(int i=0;prefixes!=null && i<prefixes.length;i++)
			{
				if(prefixes[i].startsWith("party"))
				{
					String partyExtID = extid;
					
					String uri = IOUtils.getBridgingProperty("beam.party.delete.uri");
					uri = uri.replaceAll("#1",partyExtID);
					uri = protocol+"://"+hostport+uri;
					
					executeDelete(uri, "party", partyExtID, chunkId, luwId, connprotocol, token);
				}
				
			}
			
		}
		catch (Exception e) 
		{
		   LOGGER.error("BEAM_DELETE Failed Exception {}",e.printStackTrace());
           e.printStackTrace();
		   throw new Exception(e);
        }
		
	}
	
	private int executeDelete(String uri, String entityName, String extId, String chunkId, String luwId, String connprotocol, String token) throws Exception
	{
		
		HttpURLConnection connection = null;
		HttpsURLConnection httpsConnection = null;
		JSONObject jsonEle = new JSONObject();
		int line = 0;
		KafkaMsgIOLogBuilder logBuilder = null;
		
		try {
			URL url = new URL(uri);
			
			if(connprotocol.equalsIgnoreCase("https"))
			{
				httpsConnection = (HttpsURLConnection) url.openConnection();
				httpsConnection.setRequestMethod("DELETE");
				httpsConnection.setReadTimeout(5000);
				line = httpsConnection.getResponseCode();	
			} else if(connprotocol.equalsIgnoreCase("https-C")) {
				HttpCloudSSLContext cloudSSLContext = new HttpCloudSSLContext();
				SSLContext sslContext = cloudSSLContext.getSSLContext();

				HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
				isSSLSocketConfigured = true;

				httpsConnection = (HttpsURLConnection) url.openConnection();
				httpsConnection.setRequestProperty("Authorization", token);
				httpsConnection.setRequestProperty("Content-Type", "application/merge-patch+json");
				httpsConnection.setRequestMethod("DELETE");
				httpsConnection.setReadTimeout(5000);
				LOGGER.debug("Https Connection: {} ",httpsConnection);
				line = httpsConnection.getResponseCode();	
			}
			else
			{
				connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("DELETE");
				connection.setReadTimeout(5000);
				line = connection.getResponseCode();
			}
			
			if(line == 200 || line == 204)
			{
				jsonEle.put("status", "delete successful");
				jsonEle.put("responseCode", line);
				jsonEle.put("externalId", extId);
				jsonObject.put(entityName, jsonEle);
				LOGGER.debug("BEAM_DELETE Success {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"SUCCESS","BEAM_DELETE");
			}
			else
			{
				jsonEle.put("status", "delete failed");
				jsonEle.put("responseCode", line);
				jsonEle.put("externalId", extId);
				jsonObject.put(entityName, jsonEle);
				LOGGER.debug("BEAM_DELETE Failed {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"FAILED","BEAM_DELETE");
			}
			
			LogUtil.logMsgJsonStr(LOGGER, JsonStream.serialize(logBuilder), jsonEle.toString());
			
		} catch (Exception e){
			LOGGER.error("BEAM_DELETE Failed {} {} {}",luwId,chunkId,e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}finally{
			if(connection != null)
			{
				connection.disconnect();
			}
			else
			if(httpsConnection != null)
			{
				httpsConnection.disconnect();
			}
		}
		
		return line;
		
	}

	private void retrieveCascadeData(String chunkId, String luwId,JSONObject party) throws Exception {
		
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
					String partyExtID = party.optString("partyexternalid");;
					
					String uri = IOUtils.getBridgingProperty("beam.party.delete.uri");
					uri = uri.replaceAll("#1",partyExtID);
					uri = protocol+"://"+hostport+uri;
				    LOGGER.debug("used  uri: {} ",uri);
					executeCascadeDelete(uri, "party", partyExtID, chunkId, luwId, connprotocol, token);
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
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"SUCCESS","BEAM_DELETE");
			}
			else
			{
				jsonEle.put("status", "Cascade delete failed");
				jsonEle.put("responseCode", line);
				jsonEle.put("externalId", extId);
				jsonObject.put(entityName, jsonEle);
				//LOGGER.debug("BEAM_DELETE Failed {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"FAILED","BEAM_DELETE");
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
