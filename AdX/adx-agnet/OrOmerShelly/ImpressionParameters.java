package OrOmerShelly;

import java.util.Set;

import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.devices.Device;
import tau.tac.adx.report.adn.MarketSegment;

public class ImpressionParameters {

	private Set<MarketSegment> marketSegments;
	private Device device;
	private AdType adType;
	
	public ImpressionParameters() {	}
	
	public ImpressionParameters(Set<MarketSegment> marketSegments,
			Device device, AdType adType) {
		this.marketSegments = marketSegments;
		this.device = device;
		this.adType = adType;
	}

	public Set<MarketSegment> getMarketSegments() {
		return marketSegments;
	}

	public void setMarketSegments(Set<MarketSegment> marketSegments) {
		this.marketSegments = marketSegments;
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
	
	public String toString() {
		return "MarketSegments=" + marketSegments + "; Device=" + device + "; AdType=" + adType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((adType == null) ? 0 : adType.hashCode());
		result = prime * result + ((device == null) ? 0 : device.hashCode());
		result = prime * result
				+ ((marketSegments == null) ? 0 : marketSegments.hashCode());
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
		ImpressionParameters other = (ImpressionParameters) obj;
		if (adType != other.adType)
			return false;
		if (device != other.device)
			return false;
		if (marketSegments == null) {
			if (other.marketSegments != null)
				return false;
		} else if (!marketSegments.equals(other.marketSegments))
			return false;
		return true;
	}
	
	
	
}
