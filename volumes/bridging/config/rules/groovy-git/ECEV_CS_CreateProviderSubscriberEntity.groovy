package groovy;

import java.util.stream.Collectors

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ericsson.datamigration.bridging.rocksdb.RocksDBRepository;
import com.ericsson.datamigration.bridging.rocksdb.ProviderConsumerInfo;

/**
 * 
 * @author erganaa
 *
 */
public class ECEV_CS_CreateProviderSubscriberEntity {

	private static final Logger LOGGER = LoggerFactory.getLogger(ECEV_CS_CreateProviderSubscriberEntity.class);


	public void storeInformation(JSONObject inputJson, JSONObject mappingJson) {
		JSONArray subscriberArray = inputJson.optJSONArray("SubscriberOffer");
		if(subscriberArray !=null && !subscriberArray.empty){
			List<ECEV_CS_AProviderSubscriberInformation> list = new ArrayList();
			JSONObject offersInMapping = mappingJson.optJSONObject("offer");
			for(JSONObject subscriber:subscriberArray){
				String offerId = subscriber.getString("offer_id");
				JSONObject offerObjFrmMapping = offersInMapping.optJSONObject(offerId);

				if(offerObjFrmMapping!=null){
					JSONObject offerDefinition = offerObjFrmMapping.get("offer_definition");
					JSONObject offerDefinition1 = offerDefinition.get("offer_definition1");
					String sharingEntity = offerDefinition1.optString("sharing_entity");
					String providerSourceOfferId = offerDefinition1.optString("provider_source_offer_id");
					ECEV_CS_AProviderSubscriberInformation psInfo = new ECEV_CS_AProviderSubscriberInformation();
					psInfo.setExpiryDate(subscriber.optString("expiry_date"));
					psInfo.setExpirySeconds(subscriber.optString("expiry_seconds"));
					psInfo.setMsisdn(subscriber.optString("subscriber_id"));
					psInfo.setOfferId(subscriber.optString("offer_id"));
					psInfo.setProductId(subscriber.optString("product_id"));
					psInfo.setProviderSourceOfferId(providerSourceOfferId);
					psInfo.setSharingEntity(sharingEntity);
					psInfo.setStartDate(subscriber.optString("start_date"));
					psInfo.setStartSeconds(subscriber.optString("start_seconds"));
					list.add(psInfo);
				}
			}
			storeInfo(list, inputJson);
		}
	}

	/**
	 * For provider, the key will be provider_{providermsisdn}_{offerId}
	 * For consumer, the key will be consumer_providermsisdn_providerofferid_consumermsisdn_offerId
	 * @param list
	 * @param inputJson
	 */
	private void storeInfo(List<ECEV_CS_AProviderSubscriberInformation> list, JSONObject inputJson){
		JSONArray subscriberArray = inputJson.optJSONArray("ProviderOffer");

		List providerList = list.stream().filter{x-> x.getSharingEntity().equals("provider")}.collect();

		List consumerList = list.stream().filter{x-> x.getSharingEntity().equals("consumer")}.collect();

		RocksDBRepository rocksDBRepository = RocksDBRepository.getRocksDBRepository()

		for(ECEV_CS_AProviderSubscriberInformation provider:providerList){
			StringBuilder key = new StringBuilder(AtRulesConstant.PROVIDER).append(AtRulesConstant.HASH_SEPARATOR).append(provider.getMsisdn()).append(AtRulesConstant.HASH_SEPARATOR).append(provider.getOfferId());
			ProviderConsumerInfo providerInfo = new ProviderConsumerInfo();
			providerInfo.setStartDate(provider.getStartDate());
			providerInfo.setExpiryDate(provider.getExpiryDate());
			providerInfo.setStartSeconds(provider.getStartSeconds());
			providerInfo.setExpirySeconds(provider.getExpirySeconds());
			providerInfo.setProductId(provider.getProductId());
			providerInfo.setMsisdn(provider.getMsisdn());
			providerInfo.setOfferId(provider.getOfferId());
			rocksDBRepository.save(key.toString(), providerInfo);
			LOGGER.debug ("Provider key: "+key.toString());
		}

		HashMap providerInfo = getProviderInfo(inputJson);


		for(ECEV_CS_AProviderSubscriberInformation consumer:consumerList){
			if(providerInfo.containsKey(consumer.getOfferId())&& (!(consumer.getMsisdn().equals(providerInfo.get(consumer.getOfferId()))))){
				String providerMsisdn = providerInfo.get(consumer.getOfferId());
				StringBuilder key = new StringBuilder(AtRulesConstant.CONSUMER).append(AtRulesConstant.HASH_SEPARATOR).append(providerMsisdn).append(AtRulesConstant.HASH_SEPARATOR).append(consumer.getProviderSourceOfferId()).append(AtRulesConstant.HASH_SEPARATOR).append(consumer.getMsisdn()).append(AtRulesConstant.HASH_SEPARATOR).append(consumer.getOfferId());;
				ProviderConsumerInfo consumerInfo = new ProviderConsumerInfo();
				consumerInfo.setStartDate(consumer.getStartDate());
				consumerInfo.setExpiryDate(consumer.getExpiryDate());
				consumerInfo.setStartSeconds(consumer.getStartSeconds());
				consumerInfo.setExpirySeconds(consumer.getExpirySeconds());
				consumerInfo.setProductId(consumer.getProductId());
				consumerInfo.setOfferId(consumer.getOfferId());
				consumerInfo.setMsisdn(consumer.getMsisdn());
				rocksDBRepository.save(key.toString(), consumerInfo);
				LOGGER.debug ("Consumer key: "+key.toString());
			}
			else{
				LOGGER.error("PROVIDER INFORMATION NOT FOUND");
			}
		}
	}

	private HashMap getProviderInfo(JSONObject inputJson){
		HashMap providerMap = new HashMap();
		JSONArray providers = inputJson.optJSONArray("ProviderOffer");
		for(JSONObject provider:providers){
			String providerId = provider.optString("provider_id");
			String hexa = providerId.substring(2,4);
			int length = Integer.parseInt(hexa,16);
			String providerMsisdn = provider.optString("provider_id").substring(4,length + 4);
			providerMap.put(provider.optString("offer_id"), providerMsisdn);
		}
		return providerMap;
	}
}
