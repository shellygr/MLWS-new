package OrOmerShelly.oos.bidders;

import java.util.Set;

import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.devices.Device;
import tau.tac.adx.report.adn.MarketSegment;

public class InstanceIndexKey {

	private String publisher;
	private Set<MarketSegment> marketSegment;
	private Device device;
	private AdType adType;
	private double priority;
	
	public InstanceIndexKey(String publisher,
			Set<MarketSegment> marketSegment, Device device, AdType adType, double priority) {
		this.publisher = publisher;
		this.marketSegment = marketSegment;
		this.device = device;
		this.adType = adType;
		this.priority = priority;
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

	public double getPriority() {
		return priority;
	}

	public void setPriority(double priority) {
		this.priority = priority;
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
		temp = Double.doubleToLongBits(priority);
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
		if (Double.doubleToLongBits(priority) != Double
				.doubleToLongBits(other.priority))
			return false;
		if (publisher == null) {
			if (other.publisher != null)
				return false;
		} else if (!publisher.equals(other.publisher))
			return false;
		return true;
	}
	
	
}
