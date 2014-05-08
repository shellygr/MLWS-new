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
	
	// constants defined from the table at Adx specification document
	private static double R_CAMPAIGN_MIN = 0.0001;
	private static double R_CAMPAIGN_MAX = 0.001;
	
	// keep the probabilities for each target (according to 'AdExchangeGameSpecV13')
	private static double AudienceProb[] = { 1, 
		0.4956, // 1:  --M 
		0.5044, // 2:  --F
		0.4589, // 3:  -Y-
		0.2353, // 4:  -YM
		0.2236, // 5:  -YF
		0.5411, // 6:  -O-
		0.2603, // 7:  -OM
		0.2808, // 8:  -OF
		0.8012, // 9:  L--
		0.3631, // 10: L-M
		0.4381, // 11: L-F
		0.3816, // 12: LY-
		0.1836, // 13: LYM
		0.1980, // 14: LYF
		0.4196, // 15: LO-
		0.1795, // 16: LOM
		0.2401, // 17: LOF
		0.1988, // 18: H--
		0.1325, // 19: H-M
		0.0663, // 20: H-F
		0.0773, // 21: HY-
		0.0517, // 22: HYM
		0.0256, // 23: HYF
		0.1215, // 24: HO-
		0.0808, // 25: HOM
		0.0407  // 26: HOF
		 }; 
	
	
	/* Optimization parameters - refer to project report */
	private static final double GAMMA = 0.5;
	private static final double BETA = 959; // Optimize where: 0 <= BETA < 1000
	private static final double ALPHA = (BETA + 9999000) / 10000000; // do NOT change
	
	private static CampaignBidder instance = null; // singleton
	
	private MyCampaigns myCampaigns; // holds the campaigns we won along the game. 
	   
	/* input:
	 *	Reach - higher target-reach means higher costly campaign.
	 *	Duration - longer campaign means cheaper (since we have more time to achieve the goal).
	 *	target audience.
	 */
	protected CampaignBidder(){
	      // Exists only to defeat instantiation.
		myCampaigns = MyCampaigns.getInstance();
	}
	   
	public static CampaignBidder getInstance(){ 
		
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
		 */
		
		long dayStart = pendingCampaign.getDayStart();
		long dayEnd = pendingCampaign.getDayEnd();
		double reach = pendingCampaign.getReachImps();
		double duration = dayEnd - dayStart + 1;
		double targetAudienceScore = getAudienceScore(pendingCampaign.getTargetSegment(), dayStart, dayEnd);
		
		/* minimum/maximum bid: (defined according to Adx Agent Specification) */
		double minBid = (qualityScore == 0 ? 0 : reach * R_CAMPAIGN_MIN / qualityScore);
		double maxBid = reach * R_CAMPAIGN_MAX * qualityScore;
		
		/* base calculation */
		double bid = qualityScore * ( reach / duration ) * ( 1 / targetAudienceScore );
		
		/* Normalization -
		 * This part of the formula normalize the bid base to be between minBid and maxBid
		 * and let us optimize ALPHA parameter in order to determine "how fast we raise our bids 
		 */
		bid = minBid + (maxBid - minBid)* (1 - Math.pow(ALPHA, bid));
		
		log.info("getBid: bid="+bid+" quality="+qualityScore+" minBid="+minBid+" maxbid="+maxBid+" targetAudienceScore="+targetAudienceScore);
		
		return bid;
	}
	
	/*
	 * Scores the measure of commonality of 2 target segments. 
	 * Each common market segment adds 1 to the score.
	 */
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
	
	/*
	 * we score the target segment according to it's general probability related to it's comulative 
	 * score with our active campaigns at each of the days 'dayStart' to 'dayEnd'.   
	 */
	private double getAudienceScore(Set<MarketSegment> targetSegment, long dayStart, long dayEnd) {
		double score = GAMMA;
		/* map target segment into 'index' */
		int index = getTargetIndex(targetSegment);
		for (long day=dayStart; day<=dayEnd; day++) {
			for (CampaignData campaignData : myCampaigns.getActiveCampaigns(day)) {
				score+=compareTargetSegments(campaignData.getTargetSegment(),targetSegment);
			} 
		}
		
		/* 
		 * The score will be the probability of the segment divided by the number of campaigns we already have for segments  
		 */
		return AudienceProb[index] * GAMMA / score;
	}

	/* maps a target segment to a one-to-one index between 0 to 26 */
	private int getTargetIndex(Set<MarketSegment> targetSegment) {
		int index = 0;
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
		
		if (index>26) { // wrong index
			return 0;
		}
		
		return index;
	}
}
