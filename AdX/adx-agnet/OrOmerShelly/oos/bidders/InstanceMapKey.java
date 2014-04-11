package OrOmerShelly.oos.bidders;

import java.util.Set;

import tau.tac.adx.report.adn.MarketSegment;

public class InstanceMapKey {
	String publisherName;
	Set<MarketSegment> marketSegment;
	int campaignId;
	
	public InstanceMapKey(String publisherName, Set<MarketSegment> marketSegment,
			int campaignId) {
		this.publisherName = publisherName;
		this.marketSegment = marketSegment;
		this.campaignId = campaignId;
	}

	public String toString() {
		return "Publisher: " + publisherName + " ; MarketSegment: " + marketSegment +" ; Campaign: " + campaignId;
	}
	
	public String getPublisherName() {
		return publisherName;
	}
	
	public void setPublisherName(String publisherName) {
		this.publisherName = publisherName;
	}
	
	public Set<MarketSegment> getMarketSegment() {
		return marketSegment;
	}
	
	public void setMarketSegment(Set<MarketSegment> marketSegment) {
		this.marketSegment = marketSegment;
	}
	
	public int getCampaignId() {
		return campaignId;
	}
	
	public void setCampaignId(int campaignId) {
		this.campaignId = campaignId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + campaignId;
		result = prime * result
				+ ((marketSegment == null) ? 0 : marketSegment.hashCode());
		result = prime * result
				+ ((publisherName == null) ? 0 : publisherName.hashCode());
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
		InstanceMapKey other = (InstanceMapKey) obj;
		if (campaignId != other.campaignId)
			return false;
		if (marketSegment == null) {
			if (other.marketSegment != null)
				return false;
		} else if (!marketSegment.equals(other.marketSegment))
			return false;
		if (publisherName == null) {
			if (other.publisherName != null)
				return false;
		} else if (!publisherName.equals(other.publisherName))
			return false;
		return true;
	}
}
