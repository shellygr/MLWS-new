package OrOmerShelly;

import tau.tac.adx.report.adn.MarketSegment;

public class AudienceOrientationRecord extends AudienceOrientationKey {

	private double orientation;
		
	public AudienceOrientationRecord(String publisherName,
			MarketSegment marketSegment, double orientation) {
		super(publisherName, marketSegment);
		this.orientation = orientation;
	}
	
	public String toString() {
		return super.toString() + " ; Orientation = " + orientation;
	}
		
	public AudienceOrientationKey getKey(){ 
		return new AudienceOrientationKey(publisherName, marketSegment);
	}
	
	public double getOrientation() {
		return orientation;
	}
	
	public void setOrientation(double orientation) {
		this.orientation = orientation;
	}
	
}
