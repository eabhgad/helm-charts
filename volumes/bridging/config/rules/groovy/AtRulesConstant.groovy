package groovy;
/**
 * 
 * @author erganaa
 *
 */
public class AtRulesConstant{
	
	// Error Constants
	public static final int WARN_LEVEL = 0;
	public static final int ERROR_LEVEL =1;
	public static final String ERROR_CODE = "errCode";
	public static final String ERROR_MSG = "errMsg";
	public static final String ERR_INC01 = "INC01";
	public static final String ERR_MSG_INC01 = "Consumer Contracts are updated along with Provider, so individual Consumer Contracts are ignored";
	public static final String TE_ERR_RULE_KEY = "logCode";
	public static final String TE_ERR_RULE = "TE_ERR_RULE";
	
	public static final String CHUNKID = "chunkId";
	public static final String LUWID = "luwId";
	
	// Config param for delete workflow
	//public static final String BEAM_HOSTPORT="bae-ingress.ecev-demo-eks-trf.beam";
	public static final String IOT_BEAM_HOSTPORT="10.61.49.83:13080";
	public static final String BEAM_HOSTPORT="localhost:8080";
	public static final String BEAM_PARTY_DELETE_URI="/bae/bssfIndividualPartyManagement/v1/individualPartyExternalId/#1";
	public static final String BEAM_CUSTOMER_DELETE_URI="/bae/bssfCustomerManagement/v1/customerExternalId/#1";
	public static final String BEAM_CONTRACT_DELETE_URI="/bae/bssfSubscriptionManagement/v1/customerExternalId/#1/contractExternalId/#2";
    public static final String BEAM_MASS_RESOURCE_DELETE_URI="/cpm/business/v1/updateMassResourceExtId/extId/customer/#1/contract/#2";
    //public static final String BEAM_BILLING_ACCOUNT_DELETE_URI="/bae/bssfCustomerManagement/v1/customerExternalId/cust_ext_0004650"
    public static final String BEAM_PARTY_ROLE_TERMINATE_URI="/bae/bssfPartyRoleManagement/v1/partyRoleExternalId/#1";
    public static final String BEAM_CUSTOMER_TERMINATE_URI="/bae/bssfCustomerManagement/v1/customerExternalId/#1";
    public static final String BEAM_ORGANIZATION_PARTY_TERMINATE_URI="/bae/bssfOrganizationPartyManagement/v1/organizationPartyExternalId/#1";


	public static final String BEAM_DELETE_CASCADE_TERMINATION="YES";
	public static final String AUTOMATIC_ROLLBACK_SYSTEM_WIDE_DELETE="YES";

	// Config param for SSL verification
	//public static final String BEAM_PROTOCOL="https-A";
      public static final String BEAM_PROTOCOL="https";
	  public static final String IOT_BEAM_PROTOCOL="http";

	//Configuration for TE validation

	//Error Codes
	public static final String PARTY_GENDER_ERROR_CODE="TF2000";
	public static final String PARTY_MARITALSTATUS_ERROR_CODE="TF2001";
	public static final String PARTY_NATIONALITY_ERROR_CODE="TF2002";
	public static final String PARTY_LANGUAGE_ERROR_CODE="TF2003";
	public static final String PARTY_TITLE_ERROR_CODE="TF2004";
	public static final String PARTY_STATUS_ERROR_CODE="TF2005";
	public static final String PARTY_INDIVIDUALSPECIFICATION_ERROR_CODE="TF2006";
	public static final String CUSTOMER_CUSTOMERSPECEXTERNALID_ERROR_CODE="TF2007";
	public static final String CUSTOMER_STATUS_ERROR_CODE="TF2008";
	public static final String CONTRACT_PRODUCTOFFERINGEXTERNALID_ERROR_CODE="TF2009";

	public static final String ERROR_TOPIC_NAME="TE_REJECTION";
	public static final String VALIDATION_LOGCODE="SD_TE_REJECTION";
	public static final String ERROR_GENERATION_POINT="Transformation_Validation";
	public static final String VALIDATION_ACTION="LUW Rejection";
    public static final String DETAILS_MSG="The TABLE_INFO SOURCE_VALUE is not in the source to target mapping."

}