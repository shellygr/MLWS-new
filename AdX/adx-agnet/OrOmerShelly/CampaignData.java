package OrOmerShelly;
import java.util.Set;

import tau.tac.adx.demand.CampaignStats;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.report.demand.CampaignOpportunityMessage;
import tau.tac.adx.report.demand.InitialCampaignMessage;


public class CampaignData {
	/* campaign attributes as set by server */
	Long reachImps;
	long dayStart;
	long dayEnd;
	Set<MarketSegment> targetSegment;
	double videoCoef;
	double mobileCoef;
	int id;
	private AdxQuery[] campaignQueries;//array of queries relvent for the campaign.

	/* campaign info as reported */
	CampaignStats stats;
	double budget;

	public CampaignData(InitialCampaignMessage icm) {
		reachImps = icm.getReachImps();
		dayStart = icm.getDayStart();
		dayEnd = icm.getDayEnd();
		targetSegment = icm.getTargetSegment();
		videoCoef = icm.getVideoCoef();
		mobileCoef = icm.getMobileCoef();
		id = icm.getId();

		stats = new CampaignStats(0, 0, 0);
		budget = 0.0;
	}

	public void setBudget(double d) {
		budget = d;
	}

	public CampaignData(CampaignOpportunityMessage com) {
		dayStart = com.getDayStart();
		dayEnd = com.getDayEnd();
		id = com.getId();
		reachImps = com.getReachImps();
		targetSegment = com.getTargetSegment();
		mobileCoef = com.getMobileCoef();
		videoCoef = com.getVideoCoef();
		stats = new CampaignStats(0, 0, 0);
		budget = 0.0;
	}

	@Override
	public String toString() {
		return "Campaign ID " + id + ": " + "day " + dayStart + " to "
				+ dayEnd + " " + targetSegment + ", reach: " + reachImps
				+ " coefs: (v=" + videoCoef + ", m=" + mobileCoef + ")"
				+ " budget: " + budget
				+ " Stats: " + stats;
	}
	
	public float getCampaignPriority(int dayBiddingFor) throws IllegalArgumentException {
		if (dayBiddingFor > getDayEnd()) {
			throw new IllegalArgumentException("Trying to calculate priority for a campaign after it has ended!");
		}
		
		if (dayBiddingFor == getDayEnd()) {
			System.out.println("Last day reached!");
			return impsTogo() * 3;
		}
		
		return (float)impsTogo() / ((float)(getDayEnd() - dayBiddingFor));
	}

	public int impsTogo() {
		return (int) Math.max(0, reachImps - stats.getTargetedImps());
	}
	
	/* Campaign is defined as active if:
	 * 1. It's not yet ended (days)
	 * 2. It has impsTogo > 0
	 */ 
	public boolean isActive(int relevantDay) { // relevantDay depends on context
		return (impsTogo() > 0
				&& relevantDay < getDayEnd()
				&& relevantDay >= getDayStart());
	}
	
	/* Campaign is defined as not over but below threshold if:
	 * 1. It's not yet ended (days)
	 * 2. stats.getTargetedImps()/reachImps < (1 + extraReachThreshold)
	 */
	public boolean notOverButBelowThreshold(int relevantDay, double extraReachThreshold) {
		if (relevantDay < getDayEnd()
				&& relevantDay >= getDayStart()) {
			if ( (stats.getTargetedImps()/reachImps) < (1 + extraReachThreshold) ) {
				System.out.println(("Campaign " + id + " is below threshold, currently: " + stats.getTargetedImps()/reachImps));
				System.out.println(this + " " + stats);
				return true;
			} else {
				System.out.println(("Campaign " + id + " is above threshold, currently: " + stats.getTargetedImps()/reachImps) + " threshold is " + (1+extraReachThreshold));
			}
		}
		
		System.out.println(this + " " + stats);
		return false;
	}
	
	
	/**
	 	 * Returns all relevant queries as a string.
	 	 * @param relevantQueries
	 	 * @return String of all relevant queries.
	 	 */
	 	public String printQueries() {
	 		if (campaignQueries == null) {
	 			return "null";
	 		}
	 
	 		StringBuilder sb = new StringBuilder("[");
	 		for (int i = 0 ; i < campaignQueries.length; i++) {
	 			sb.append(campaignQueries[i].toString() + ", ");
	 		}
	 
	 		sb.append("]");
	 
	 		return sb.toString();
	 	}

	void setStats(CampaignStats s) {
		stats.setValues(s);
	}

	public AdxQuery[] getCampaignQueries() {
		return campaignQueries;
	}

	public void setCampaignQueries(AdxQuery[] campaignQueries) {
		this.campaignQueries = campaignQueries;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<MarketSegment> getTargetSegment() {
		return targetSegment;
	}

	public void setTargetSegment(Set<MarketSegment> targetSegment) {
		this.targetSegment = targetSegment;
	}

	public CampaignStats getStats() {
		return stats;
	}

	public double getBudget() {
		return budget;
	}

	public Long getReachImps() {
		return reachImps;
	}

	public void setReachImps(Long reachImps) {
		this.reachImps = reachImps;
	}

	public long getDayStart() {
		return dayStart;
	}

	public void setDayStart(long dayStart) {
		this.dayStart = dayStart;
	}

	public long getDayEnd() {
		return dayEnd;
	}

	public void setDayEnd(long dayEnd) {
		this.dayEnd = dayEnd;
	}

	public double getVideoCoef() {
		return videoCoef;
	}

	public void setVideoCoef(double videoCoef) {
		this.videoCoef = videoCoef;
	}

	public double getMobileCoef() {
		return mobileCoef;
	}

	public void setMobileCoef(double mobileCoef) {
		this.mobileCoef = mobileCoef;
	}

	
	
}
