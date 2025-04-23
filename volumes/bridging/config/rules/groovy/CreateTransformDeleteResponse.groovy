package groovy;

import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.net.URL;
import java.net.URI;
import java.net.HttpURLConnection;
import java.util.Optional;

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


class CreateTransformDeleteResponse {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateTransformDeleteResponse.class);
	private JSONObject jsonObject = new JSONObject();
	
	private AtRulesConstant atrules = new AtRulesConstant();
	

	def String execute(String input){
		LOGGER.debug("executing groovy scripts: input {} ",input);
		JSONObject inputJson = new JSONObject(input);
		String chunkId = inputJson.optString("chunk_id");
		String luwId = inputJson.optString("luw_id");
		jsonObject.put("chunk_id",chunkId);
		jsonObject.put("luw_id",luwId);
		//String cascadeTermination = IOUtils.getBridgingProperty("beam.delete.cascade.termination");
		String cascadeTermination = atrules.BEAM_DELETE_CASCADE_TERMINATION;
		
		if(cascadeTermination.equalsIgnoreCase("YES"))
		{
			cascadeTerminationData(chunkId, luwId, inputJson);
		}
		else
		{
			deleteData(chunkId, luwId, inputJson);
		}
		
		LOGGER.debug("Delete output {}", jsonObject);
		return jsonObject.toString();
	}
	
	private void deleteData(String chunkId, String luwId, JSONObject inputJson) throws Exception
	{
		inputJson = inputJson.getJSONObject("BODY");
		JSONObject party = (JSONObject)inputJson.getJSONArray("stg_beam_party").get(0);
		JSONObject customer = (JSONObject)inputJson.getJSONArray("stg_beam_customer").get(0);
		JSONObject contract = (JSONObject)inputJson.getJSONArray("stg_beam_contract").get(0);
		
		try
		{
			//String hostport = IOUtils.getBridgingProperty("beam.hostport");
			String hostport = atrules.BEAM_HOSTPORT;
			String beam_protocol = "http";

			if(atrules.BEAM_PROTOCOL.equalsIgnoreCase("https-K") || atrules.BEAM_PROTOCOL.equalsIgnoreCase("https-A")) {
				beam_protocol = "https";
			}

			if(contract != null)
			{
				String contExtID = contract.get("contractexternalid");
				String custExtID = customer.get("customerexternalid");
				
				//String uri = IOUtils.getBridgingProperty("beam.contract.delete.uri");
				String uri = atrules.BEAM_CONTRACT_DELETE_URI;
				uri = uri.replaceAll("#1",custExtID);
				uri = uri.replaceAll("#2",contExtID);
				uri = beam_protocol+"://"+hostport+uri;

				executeDelete(uri, "contract", contExtID,  chunkId, luwId);
			}
			
			if(customer != null)
			{
				String custExtID = customer.get("customerexternalid");
				
				//String uri = IOUtils.getBridgingProperty("beam.customer.delete.uri");
				String uri = atrules.BEAM_CUSTOMER_DELETE_URI;
				uri = uri.replaceAll("#1",custExtID);
				uri = beam_protocol+"://"+hostport+uri;
					
				executeDelete(uri, "customer", custExtID,  chunkId, luwId);
			}
			if(party != null)
			{
				String partyExtID = party.optString("partyexternalid");
				
				//String uri = IOUtils.getBridgingProperty("beam.party.delete.uri");
				String uri = atrules.BEAM_PARTY_DELETE_URI;
				uri = uri.replaceAll("#1",partyExtID);
				uri = beam_protocol+"://"+hostport+uri;
					
				executeDelete(uri, "party", partyExtID, chunkId, luwId);
			}
		
		}
		catch (Exception e) 
		{
		   LOGGER.error("BEAM_DELETE Failed Exception {}",e.printStackTrace());
           e.printStackTrace();
		   throw new Exception(e);
        }
	}
		
	private int executeDelete(String uri, String entityName, String extId, String chunkId, String luwId) throws Exception
	{
		
		HttpURLConnection connection = null;
		JSONObject jsonEle = new JSONObject();
		int line = 0;
		BufferedReader br = null;
		StringBuilder message = new StringBuilder();
		String currentLine;
		KafkaMsgIOLogBuilder logBuilder = null;
		
		try {
			URL url = new URL(uri);
			
			if(atrules.BEAM_PROTOCOL.equalsIgnoreCase("https-A")) {
				URI uriObj = new URI(uri);
				SSLContext sslContext = createInsecureSSLContext();
				InsecureClientHttpRequestFactory insecureFactory = new InsecureClientHttpRequestFactory(sslContext);
				insecureFactory.createRequest(uriObj,HttpMethod.DELETE);
				connection = (HttpsURLConnection) insecureFactory.getConnection();
			} else {
				connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("DELETE");
			}
			//connection.setReadTimeout(5000);
			connection.setReadTimeout(10000);//Timeout increased for slow BEAM response
			line = connection.getResponseCode();
					
			if(line == 200 || line == 204)
			{
				jsonEle.put("status", "delete successful");
				jsonEle.put("responseCode", line);
				jsonEle.put("externalId", extId);

 				br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    			while ((currentLine = br.readLine()) != null) 
        			message.append(currentLine);
				br.close();
				jsonEle.put("response", message);

				jsonObject.put(entityName, jsonEle);
				//LOGGER.debug("BEAM_DELETE Success {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"SUCCESS","BEAM_DELETE");
			}
			else
			{
				jsonEle.put("status", "delete failed");
				jsonEle.put("responseCode", line);
				jsonEle.put("externalId", extId);
		
 				br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
    			while ((currentLine = br.readLine()) != null) 
        			message.append(currentLine);
				br.close();
				jsonEle.put("response", message);

				jsonObject.put(entityName, jsonEle);
				//LOGGER.debug("BEAM_DELETE Failed {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"FAILED","BEAM_DELETE");
			}

			LogUtil.logMsgJsonStr(LOGGER, JsonStream.serialize(logBuilder), jsonEle.toString());
			
		} catch (Exception e){
			LOGGER.error("BEAM_DELETE Failed {} {} {}",luwId,chunkId,e.printStackTrace());
			e.printStackTrace();
			throw new Exception(e);
		}finally{
			if(!connection.equals(null)) {
				connection.disconnect();
			}
		}
		
		return line;
		
	}
	
	private void cascadeTerminationData(String chunkId, String luwId, JSONObject inputJson) throws Exception
	{
		inputJson = inputJson.getJSONObject("BODY");
		JSONObject party = (JSONObject)inputJson.getJSONArray("stg_beam_party").get(0);
		try
		{
			//String hostport = IOUtils.getBridgingProperty("beam.hostport");
			String hostport = atrules.BEAM_HOSTPORT;
			
			if(party != null)
			{
				String partyExtID = party.optString("partyexternalid");
				
				//String uri = IOUtils.getBridgingProperty("beam.party.delete.uri");
				String uri = atrules.BEAM_PARTY_DELETE_URI;
				
				uri = uri.replaceAll("#1",partyExtID);
				uri = "http://"+hostport+uri;
					
				executeCascadeTermination(uri, "party", partyExtID, chunkId, luwId);
			}
		}
		catch (Exception e) 
		{
		   LOGGER.error("BEAM_DELETE Failed Exception {}",e.printStackTrace());
           e.printStackTrace();
		   throw new Exception(e);
        }
	}
	
	
	
	private int executeCascadeTermination (String uri, String entityName, String extId, String chunkId, String luwId) throws Exception
	{
		KafkaMsgIOLogBuilder logBuilder = null;
		
		String contentType = "application/merge-patch+json;profile=\"http://ericsson.com/bss.bssfIndividualPartyManagement.individualParty.updateRequest.2.json#\"";
		String accept = "application/json;profile=\"http://ericsson.com/bss.bssfIndividualPartyManagement.individualParty.updateResponse.2.json#\";profile=\"http://ericsson.com/bss.errorMessage.response.2.json#\"";
		
		//------Request status Start -----------------//
		JSONObject req = new JSONObject();
		JSONObject validFor = getValidFor(getTerminationFormattedDate());
		JSONObject status = new JSONObject();
		status.put("status","PartyTerminated");
		status.put("validFor",validFor);
		JSONArray statusArr = new JSONArray();
		statusArr.put(status);
		req.put("status",statusArr);
		LOGGER.debug("Request Payload-----: "+req);
		//------Request status End -----------------//
		
		int respCode = 0;
		JSONObject jsonEle = new JSONObject();
		try{
			HttpPatch pm = new HttpPatch(uri);
			pm.setHeader("Accept", accept);
			pm.setHeader("Content-Type", contentType);
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
				jsonEle.put("status", "Cascade Termination successful");
				jsonEle.put("responseCode", respCode);
				jsonEle.put("externalId", extId);
				jsonObject.put(entityName, jsonEle);
				//LOGGER.debug("BEAM_DELETE Success {} {} Entity Name:{}{}",luwId,chunkId,entityName,jsonEle);
				logBuilder = new KafkaMsgIOLogBuilder(chunkId,luwId,"SUCCESS","BEAM_DELETE");
			}
			else
			{
				jsonEle.put("status", "Cascade Termination failed");
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

		LOGGER.debug("Date:----"+strDate);
		Date dateObj = (Date)formatter.parse(strDate);
		
		Calendar c = Calendar.getInstance();
		c.setTime(dateObj);
		c.add(Calendar.MINUTE, 10);
		dateObj = c.getTime();		
		String dateStr = formatter.format(dateObj)
		
		return dateStr;
	
	}

	private static SSLContext createInsecureSSLContext() {
    	try {
            // Create a trust manager that trusts all certificates
            TrustManager[] trustAllCerts = [
                new X509TrustManager() {
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
