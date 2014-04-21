package OrOmerShelly.bidders;

import java.util.Set;
import java.util.logging.Logger;

import OrOmerShelly.CampaignData;
import OrOmerShelly.Coordinator;
import OrOmerShelly.MyCampaigns;

import tau.tac.adx.report.adn.MarketSegment;

public class CampaignBidder {
	
	private final Logger log = Logger
			.getLogger(Coordinator.class.getName());
	
	private static double R_CAMPAIGN_MIN = 0.0001;
	private static double R_CAMPAIGN_MAX = 0.001;
	
	// keep the probabilities for each target
	private static double AudienceProb[] = { 0, 0, 0, 0, 
		
		0.248282828, // 4 - young male
		0.23959596,  // 5 - young female
		0,			 // 6
		0.237272727, // 7 - old male
		0.274848485, // 8 - old female
		0,			 // 9 
		0.252121212, // 10- low male
		0.258383838, // 11- low female
		0.256969697, // 12- young low
		0,			 // 13
		0,			 // 14
		0.253535354, // 15- old low
		0,			 // 16
		0,			 // 17
		0,			 // 18
		0.233434343, // 19- high male
		0.256060606, // 20- high female
		0.230909091, // 21- high young
		0,			 // 22
		0,			 // 23		
		0.258585859, // 24- high old
		0,0 }; 
	
	private static final double MAX_BID = 100;

	private static final double BETA = 800; // Optimize where: 0 <= BETA < 1000
	private static final double ALPHA = (BETA + 999000) / 1000000; // do NOT change
	
	private static CampaignBidder instance = null;
	
	private MyCampaigns myCampaigns;
	   
	/* input:
		Reach - higher target-reach means higher costly campaign.
		Duration - longer campaign means cheaper (since we have more time to achieve the goal).
		target audience.
		--
		UCS level (better level make UCS  results weightier)
		predicted impressions per target audience - more impressions means cheaper bid.	
	   ----
	*/
	protected CampaignBidder() {
	      // Exists only to defeat instantiation.
		myCampaigns = MyCampaigns.getInstance();
	}
	   
	public static CampaignBidder getInstance() {
		
		if(instance == null) {
			instance = new CampaignBidder();
	    }
		
		return instance;
	}
	
	public double getBid(OrOmerShelly.CampaignData pendingCampaign, double qualityScore){
	
		/*
		 * here we do the magic.
		 * use a smart assessment algorithm to generate the next bid. 
		 * 
		 * [Note: machine learning algorithm might not fit here, since it takes a lot of time (game days) to
		 * figure out whether a particular bid was payed-off (winning the campaigns NOT the only target) 
		 * 
		 * we shall try:
		 * BID = (alpha)*[(Reach)/(Duration)] * 1/(1 + targetAudienceScore)
		 *  plus some more parameters and normalizations.
		 */
		
		long dayStart = pendingCampaign.getDayStart();
		long dayEnd = pendingCampaign.getDayEnd();
		double reach = pendingCampaign.getReachImps();
		double duration = dayEnd - dayStart +1;
		double targetAudienceScore = getAudienceScore(pendingCampaign.getTargetSegment(), dayStart, dayEnd);
		
		/* minimum/maximum bid: */
		double minBid = (qualityScore ==0 ? MAX_BID: reach * R_CAMPAIGN_MIN / qualityScore);
		double maxBid = reach * R_CAMPAIGN_MAX * qualityScore;
		
		double bid = qualityScore * ( reach / duration ) * ( 1 / targetAudienceScore );
		
		bid = minBid + (maxBid - minBid)* (1 - Math.pow(ALPHA, bid));
		
		
		log.info("getBid: bid="+bid+" quality="+qualityScore+" minBid="+minBid+" maxbid="+maxBid+" targetAudienceScore="+targetAudienceScore);
		
		bid = Math.min(bid, maxBid);
		bid = Math.max(bid, minBid);
			
		return bid;
	}
	
	
	private static int compareTargetSegments(Set<MarketSegment> targetA, Set<MarketSegment> targetB){
		int score = 0;
		
		for (MarketSegment marketSegmentA : targetA) {
			for (MarketSegment marketSegmentB : targetB) {
				if (marketSegmentA.compareTo(marketSegmentB)==0){
					score++;
				}
			}
		}
		
		return score;
	}
	
	private double getAudienceScore(Set<MarketSegment> targetSegment, long dayStart, long dayEnd) {
		int score = 1;
		/* map target segment into 'index' */
		int index = getTargetIndex(targetSegment);
		for (long day=dayStart; day<=dayEnd; day++) {
			for (CampaignData campaignData : myCampaigns.getActiveCampaigns(day)) {
				score+=compareTargetSegments(campaignData.getTargetSegment(),targetSegment);
			} 
		}
		
		/* 
		 * the score will be the probability of the segment divided by the number of campaigns we already have for segments  
		 */
		return AudienceProb[index]/(double)score;
	}

	/*
	 * A method to update the bidder for every campaign bidding result , in order to 
	 * store a history.
	 */
//	public void updateCampaignes(int campaignId, String winner, double price ) {
//		
//	// todo: save the data - maybe also process some calculations.
//		
//		
//	
//	}

//	public void updateNewPendingCampaign(Set<MarketSegment> targetSegment) {
//		
//		pendingCampaignTarget = targetSegment;
//		
//	}
//	
//	public void updateWonPendingCampaign() {
//		
//		/* map target segment into 'index' */
//		int index = getTargetIndex(pendingCampaignTarget);
//		
//		++AudienceBindedCampaigns[index];
//		
//	}
	
	private int getTargetIndex(Set<MarketSegment> targetSegment) {
		int index=0;
		for ( MarketSegment ms : targetSegment) {
			
			switch (ms) {
			case MALE:
				index += 1;
				break;
			case FEMALE:
				index += 2;
				break;
			case YOUNG: // young is up to 44
				index += 3;
				break;
			case OLD:
				index += 6;
				break;
			case LOW_INCOME:
				index += 9;
				break;
			case HIGH_INCOME: // high is 60k+
				index += 18;
				break;
			default:
				break;
			 
			}
		}
		return index;
	}

}
