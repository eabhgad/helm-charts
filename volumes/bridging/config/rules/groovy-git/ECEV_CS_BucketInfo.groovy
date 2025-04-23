package groovy;

import org.json.JSONArray

public class ECEV_CS_BucketInfo {

	private String msisdn;
	private String product_id;
	private String account_type;
	private String account_id;
	private String po_external_id;
	private String pb_spec_external_id;
	private String uom;
	private int decimal_places;
	private String action;
	private long units;
	
	private String pb_current_triggerTime;
	private String pb_needs_validFor;
	private String pb_reset_amount;
	private String validFor_startDate;
	private String validFor_endDate;
	
	
	private JSONArray characteristics; 

	public JSONArray getCharacteristics() {
		return characteristics;
	}

	public void setCharacteristics(JSONArray characteristics) {
		this.characteristics = characteristics;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public String getProduct_id() {
		return product_id;
	}

	public void setProduct_id(String product_id) {
		this.product_id = product_id;
	}

	public String getAccount_type() {
		return account_type;
	}

	public void setAccount_type(String account_type) {
		this.account_type = account_type;
	}

	public String getAccount_id() {
		return account_id;
	}

	public void setAccount_id(String account_id) {
		this.account_id = account_id;
	}

	public String getPo_external_id() {
		return po_external_id;
	}

	public void setPo_external_id(String po_external_id) {
		this.po_external_id = po_external_id;
	}

	public String getPb_spec_external_id() {
		return pb_spec_external_id;
	}

	public void setPb_spec_external_id(String pb_spec_external_id) {
		this.pb_spec_external_id = pb_spec_external_id;
	}

	public String getUom() {
		return uom;
	}

	public void setUom(String uom) {
		this.uom = uom;
	}

	public int getDecimal_places() {
		return decimal_places;
	}

	public void setDecimal_places(int decimal_places) {
		this.decimal_places = decimal_places;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public long getUnits() {
		return units;
	}

	public void setUnits(long units) {
		this.units = units;
	}

	public String getPb_current_triggerTime() {
		return pb_current_triggerTime;
	}

	public void setPb_current_triggerTime(String pb_current_triggerTime) {
		this.pb_current_triggerTime = pb_current_triggerTime;
	}

	public String getPb_needs_validFor() {
		return pb_needs_validFor;
	}

	public void setPb_needs_validFor(String pb_needs_validFor) {
		this.pb_needs_validFor = pb_needs_validFor;
	}

	public String getPb_reset_amount() {
		return pb_reset_amount;
	}

	public void setPb_reset_amount(String pb_reset_amount) {
		this.pb_reset_amount = pb_reset_amount;
	}

	public String getValidFor_startDate() {
		return validFor_startDate;
	}

	public void setValidFor_startDate(String validFor_startDate) {
		this.validFor_startDate = validFor_startDate;
	}

	public String getValidFor_endDate() {
		return validFor_endDate;
	}

	public void setValidFor_endDate(String validFor_endDate) {
		this.validFor_endDate = validFor_endDate;
	}
}
