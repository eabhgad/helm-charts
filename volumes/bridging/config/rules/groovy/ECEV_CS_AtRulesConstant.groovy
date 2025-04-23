package groovy;
/**
 * 
 * @author erganaa
 *
 */
public class ECEV_CS_AtRulesConstant{
	
	// Error Constants
	public static final String ERR_INC01 = "INC01";
	public static final String ERR_MSG_INC01 = "subscriber_status is null";
	public static final String ERR_INC02= "INC02";
	public static final String ERR_MSG_INC02 = "missing lifecycle dates";

	public static final String ERR_INC03 = "INC03";
	public static final String ERR_MSG_INC03 = "sup_expiry_date is less than activated";
	public static final String ERR_INC04= "INC04";
	public static final String ERR_MSG_INC04 = "sfee_expiry_date is less than activated";


	public static final String ERR_INC05 = "INC05";
	public static final String ERR_MSG_INC05 = "sup_expiry_date is greather than sfee_expiry_date";
	public static final String ERR_INC06= "INC06";
	public static final String ERR_MSG_INC06 = "offer_id not mapped";


	public static final String ERR_INC07 = "INC07";
	public static final String ERR_MSG_INC07 = "orig_account_class not mapped";
	public static final String ERR_INC08= "INC08";
	public static final String ERR_MSG_INC08 = "offer.start_date is greater than offer.expiry_date";

	public static final String ERR_INC09 = "INC09";
	public static final String ERR_MSG_INC09 = "timer offer dates seconds missing";
	public static final String ERR_INC10= "INC10";
	public static final String ERR_MSG_INC10 = "subscriber already terminated";


	public static final String ERR_INC11 = "INC11";
	public static final String ERR_MSG_INC11 = "source bucket empty or missing";
	
	public static final String ERROR_CODE = "errCode";
	public static final String ERROR_MSG = "errMsg";
	
	public static final int SUCCESS = 0;
	public static final int INVALID_STATUS = 1;
	public static final int AVAILABLE_STATUS = 2;
	public static final int INVALID_EPOCH = 3;
	public static final int INVALID_SUP_EXPIRY = 4;
	public static final int INVALID_SFEE_EXPIRY = 5;
	public static final int INVALID_SUP_STATUS = 6;
	public static final int INVALID_SFEE_STATUS = 7;
	public static final int SUP_LESS_THAN_ACTIVATED = 8;
	public static final int SFEE_LESS_THAN_ACTIVATED = 9;
	public static final int SUP_GREATER_THAN_SFEE = 10;
	public static final int SUB_ALREADY_TERMINATED = 11;
	public static final int INVALID_OFFER_DATES = 12
	public static final int INVALID_START_SECONDS = 13;
	public static final int INVALID_EXPIRY_SECONDS = 14;
	
	// Other constants
	public static final String DATE_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	public static final String TE_ERR_RULE = "TE_ERR_RULE";
	public static final String TE_ERR_RULE_KEY = "logCode";
	public static final String NULL = "NULL";
	
	public static final String UNDERSCORE = "_";
	public static final String HASH_SEPARATOR = "#";
	
	public static final int WARN_LEVEL = 0;
	public static final int ERROR_LEVEL =1;
	
	public static final String CHUNKID = "chunkId";
	public static final String LUWID = "luwId";
	
	public static final String SERVICE_CLASS_IDENTIFIER = "S";
	public static final String OFFER_IDENTIFIER = "O";
	public static final String ZERO = "0";
	
	public static final String OFFER_START_DATE = "1";
	public static final String OFFER_EXP_DATE = "2";
	public static final String OFFER_START_SEC = "3";
	public static final String OFFER_EXP_SEC = "4";
	public static final String OFFER_TYPE = "5";
	public static final String OFFER_DATE = "6";
	public static final String OFFER_BUNDLE_ID = "7";
	public static final String OFFER_ID = "8";
	public static final String OFFER_PRODUCT_ID = "9";
	public static final String OFFER_BASE_OFFER = "10";
	
	public static final String PRODUCT_ENTITY = "product";
	public static final String COMPRISED_OFF_ENTITY = "comprisedOf";
	public static final String BUCKET_ENTITY = "bucket";
	public static final String ERROR = "ERROR";
	
	public static final String EXTERNAL_ID = "externalId";
	
	public static final String PROVIDER = "provider";
	public static final String CONSUMER = "consumer";
	
	public static final byte WORKFLOW_TYPE = 2;	
	public static final byte CREATE_ONLY_FLOW = 1;	
	public static final byte CREATE_UPDATE_BOTH = 2;
	
	public String getExternalId(String prefix, String externalId){
		StringBuilder sb = new StringBuilder(prefix);
		sb.append(UNDERSCORE);
		sb.append(externalId);
		return sb.toString();
	}	
}

