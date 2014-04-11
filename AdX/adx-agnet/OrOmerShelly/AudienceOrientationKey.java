package OrOmerShelly;

import tau.tac.adx.report.adn.MarketSegment;

public class AudienceOrientationKey {
	
	protected String publisherName;
	protected MarketSegment marketSegment;
	
	public AudienceOrientationKey(String publisherName,
			MarketSegment marketSegment) {
		super();
		this.publisherName = publisherName;
		this.marketSegment = marketSegment;
	}
	
	public String toString() {
		return "Name: " + publisherName + "; MarketSegment: " + marketSegment;
	}
	
	public String getPublisherName() {
		return publisherName;
	}
	
	public void setPublisherName(String publisherName) {
		this.publisherName = publisherName;
	}
	
	public MarketSegment getMarketSegment() {
		return marketSegment;
	}
	
	public void setMarketSegment(MarketSegment marketSegment) {
		this.marketSegment = marketSegment;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		AudienceOrientationKey other = (AudienceOrientationKey) obj;
		if (marketSegment != other.marketSegment)
			return false;
		if (publisherName == null) {
			if (other.publisherName != null)
				return false;
		} else if (!publisherName.equals(other.publisherName))
			return false;
		return true;
	}
	
	
}