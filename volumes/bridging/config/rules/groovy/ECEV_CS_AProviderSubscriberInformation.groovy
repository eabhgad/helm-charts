package groovy

class ECEV_CS_AProviderSubscriberInformation {

	private String startDate;
	private String expiryDate;
	private String startSeconds;
	private String expirySeconds;
	private String productId;

	private String sharingEntity;
	private String providerSourceOfferId;
	private String offerId;
	private String msisdn;

	public String getSharingEntity() {
		return sharingEntity;
	}
	public void setSharingEntity(String sharingEntity) {
		this.sharingEntity = sharingEntity;
	}
	public String getProviderSourceOfferId() {
		return providerSourceOfferId;
	}
	public void setProviderSourceOfferId(String providerSourceOfferId) {
		this.providerSourceOfferId = providerSourceOfferId;
	}
	public String getOfferId() {
		return offerId;
	}
	public void setOfferId(String offerId) {
		this.offerId = offerId;
	}
	public String getMsisdn() {
		return msisdn;
	}
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}


	public String getStartDate() {
		return startDate;
	}
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	public String getExpiryDate() {
		return expiryDate;
	}
	public void setExpiryDate(String expiryDate) {
		this.expiryDate = expiryDate;
	}
	public String getStartSeconds() {
		return startSeconds;
	}
	public void setStartSeconds(String startSeconds) {
		this.startSeconds = startSeconds;
	}
	public String getExpirySeconds() {
		return expirySeconds;
	}
	public void setExpirySeconds(String expirySeconds) {
		this.expirySeconds = expirySeconds;
	}
	public String getProductId() {
		return productId;
	}
	public void setProductId(String productId) {
		this.productId = productId;
	}
}
