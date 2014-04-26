package OrOmerShelly.bidders;

import java.util.Set;

import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.devices.Device;
import tau.tac.adx.report.adn.MarketSegment;

public class InstanceIndexKey {

	private String publisher;
	private Set<MarketSegment> marketSegment;
	private Device device;
	private AdType adType;
	private int campaignId;
	
	public InstanceIndexKey(String publisher,
			Set<MarketSegment> marketSegment, Device device, AdType adType, int campaignId) {
		this.publisher = publisher;
		this.marketSegment = marketSegment;
		this.device = device;
		this.adType = adType;
		this.campaignId = campaignId;
	}
	
	public String toString() {
		return "\nPublisher = " + publisher + "; MarketSegment = " + marketSegment + "; Device = " + device + "; AdType = " + adType + "; CampaignId = " + campaignId + "\t";
	}

	public String getPublisher() {
		return publisher;
	}
	
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
		
	public Set<MarketSegment> getMarketSegment() {
		return marketSegment;
	}

	public void setMarketSegment(Set<MarketSegment> marketSegment) {
		this.marketSegment = marketSegment;
	}

	public Device getDevice() {
		return device;
	}
	
	public void setDevice(Device device) {
		this.device = device;
	}
	
	public AdType getAdType() {
		return adType;
	}
	
	public void setAdType(AdType adType) {
		this.adType = adType;
	}

	public int getPriority() {
		return campaignId;
	}

	public void setPriority(int campaignId) {
		this.campaignId = campaignId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((adType == null) ? 0 : adType.hashCode());
		result = prime * result + ((device == null) ? 0 : device.hashCode());
		result = prime * result
				+ ((marketSegment == null) ? 0 : marketSegment.hashCode());
		long temp;
		temp = Double.doubleToLongBits(campaignId);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((publisher == null) ? 0 : publisher.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstanceIndexKey other = (InstanceIndexKey) obj;
		if (adType != other.adType)
			return false;
		if (device != other.device)
			return false;
		if (marketSegment == null) {
			if (other.marketSegment != null)
				return false;
		} else if (!marketSegment.equals(other.marketSegment))
			return false;
		if (Double.doubleToLongBits(campaignId) != Double
				.doubleToLongBits(other.campaignId))
			return false;
		if (publisher == null) {
			if (other.publisher != null)
				return false;
		} else if (!publisher.equals(other.publisher))
			return false;
		return true;
	}


	
	
}
