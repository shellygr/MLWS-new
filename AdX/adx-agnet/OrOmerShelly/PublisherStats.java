package OrOmerShelly;

public class PublisherStats {

	private long popularity;
	private long videoOrientation;
	private long textOrientation;
	
	public PublisherStats(long popularity, long videoOrientation, long textOrientation) {
		this.popularity = popularity;
		this.videoOrientation = videoOrientation;
		this.textOrientation = textOrientation;
	}
	
	public double getAdTypeOrientation() {
		return videoOrientation / textOrientation;
	}
	
	public long getPopularity() {
		return popularity;
	}
	
	public void setPopularity(long popularity) {
		this.popularity = popularity;
	}
	
	public long getVideoOrientation() {
		return videoOrientation;
	}
	
	public void setVideoOrientation(long videoOrientation) {
		this.videoOrientation = videoOrientation;
	}
	
	public long getTextOrientation() {
		return textOrientation;
	}
	
	public void setTextOrientation(long textOrientation) {
		this.textOrientation = textOrientation;
	}	
	
	public String toString() {
		return "Popularity = " + popularity + "; VideoOrientation = " + videoOrientation + "; TextOrientation = " + textOrientation;
	}
}
