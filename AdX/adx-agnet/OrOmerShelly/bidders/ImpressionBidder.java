package OrOmerShelly.bidders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import OrOmerShelly.CampaignData;
import OrOmerShelly.ImpressionParameters;
import OrOmerShelly.ImpressionParamtersDistributionKey;
import OrOmerShelly.PublisherStats;
import OrOmerShelly.bidders.ImpressionClassifier;
import OrOmerShelly.userAnalysis.UserAnalyzer;

import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.props.PublisherCatalogEntry;
import tau.tac.adx.report.adn.AdNetworkKey;
import tau.tac.adx.report.adn.AdNetworkReport;
import tau.tac.adx.report.adn.AdNetworkReportEntry;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.users.AdxUser;
import tau.tac.adx.users.properties.Age;
import tau.tac.adx.users.properties.Gender;
import tau.tac.adx.users.properties.Income;
import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.devices.Device;
import weka.classifiers.Classifier;
import edu.umich.eecs.tac.props.Ad;

public class ImpressionBidder {

	private static ImpressionBidder instance = null;
	
	private final Logger log = Logger
			.getLogger(ImpressionBidder.class.getName());
	

	private PublisherCatalog publisherCatalog;
	private Map<String, PublisherStats> publishersStats = new HashMap<String, PublisherStats>();
	private UserAnalyzer userAnalyzer;
	private ImpressionClassifier concreteClassifier;
	
	private final static double CPM = 1000.0;
	
	private List<CampaignData> myActiveCampaigns; // mutable!!!

	@SuppressWarnings("unused")
	private double bankBalance;
	
	private AdxBidBundle bidBundle;
	private AdxBidBundle previousBidBundle;

	private int dayBiddingFor; // TODO: check exactly which day should be given: current day or day bidding for (that is currentDay + 1)?
	
	public void init(Classifier newClassifier, CampaignData currentCampaign, int day) throws Exception { // Called one time during the game or when a classifier changes
		this.dayBiddingFor = day;
		concreteClassifier.init(newClassifier, currentCampaign);
	}

	// TODO: set daily limits for campaign and overall - do not want to get a minus in bankBalance?

	public AdxBidBundle getDefaultBidBundle(AdxQuery[] queries) { // Based on SampleAdNetwork
		
		bidBundle = new AdxBidBundle();
		
		for (CampaignData campaign : myActiveCampaigns) {

			/* A fixed random bid, for all queries of the campaign */
			/*
			 * Note: bidding per 1000 imps (CPM) - no more than average budget
			 * revenue per imp
			 */

			Random rnd = new Random(); // TODO: Shelly; he we set the bids for impressions
			double avgCmpRevenuePerImp = campaign.getBudget() / campaign.getReachImps();
			double randomDouble = rnd.nextDouble();
			double rbid = CPM * randomDouble * avgCmpRevenuePerImp;
			log.info("Default bid bundle bid for " + campaign + " will be " + rbid + " where our rnd was " + randomDouble);
			
			/*
			 * add bid entries w.r.t. each active campaign with remaining
			 * contracted impressions.
			 * 
			 * for now, a single entry per active campaign is added for queries
			 * of matching target segment.
			 */

			if ((dayBiddingFor >= campaign.getDayStart())
					&& (dayBiddingFor <= campaign.getDayEnd())
					&& (campaign.impsTogo() >= 0)) {

				int entCount = 0;
				for (int i = 0; i < queries.length; i++) {

					Set<MarketSegment> segmentsList = queries[i]
							.getMarketSegments();

					for (@SuppressWarnings("unused") MarketSegment marketSegment : segmentsList) {
						// TODO: this is very different from the git repository!!!
						//if (campaign.getTargetSegment() == marketSegment) {
						/*
						 * among matching entries with the same campaign id,
						 * the AdX randomly chooses an entry according to
						 * the designated weight. by setting a constant
						 * weight 1, we create a uniform probability over
						 * active campaigns
						 */
						++entCount;
						bidBundle.addQuery(queries[i], rbid, new Ad(null),
								campaign.getId(), 1);
						//}
					}

					if (segmentsList.size() == 0) {
						++entCount;
						bidBundle.addQuery(queries[i], rbid, new Ad(null),
								campaign.getId(), 1);
					}
				}
				double impressionLimit = 0.5 * campaign.impsTogo();
				double budgetLimit = 0.5 * Math.max(0, campaign.getBudget()
						- campaign.getStats().getCost());
				bidBundle.setCampaignDailyLimit(campaign.getId(),
						(int) impressionLimit, budgetLimit);
				log.info("Day " + dayBiddingFor + ": Updated " + entCount
						+ " Bid Bundle entries for Campaign id " + campaign.getId());
			}
		}
		
		return bidBundle;
	}
	
	// TODO: Optimisation: sort myActiveCampaigns in Coordinator so prioritizing will be more efficient! 
	public void fillBidBundle() throws Exception {
		
		String rname = "fillBidBundle";

		bidBundle = new AdxBidBundle();
		
		if (myActiveCampaigns == null || myActiveCampaigns.size() == 0) {
			log.info("No reason to fill bid bundle at all - no active campaigns");
			return;
		}
		
		log.info("fill bid bundle for day " + dayBiddingFor);
		
		for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog.getPublishers()) {
			String publisherName = publisherCatalogEntry.getPublisherName();
			//log.info(rname + ": Filling bid bundle for " + publisherName);
			
			List<ImpressionParamtersDistributionKey> impressionDistribution = userAnalyzer.calcImpressionDistribution(publisherName);
			
			Collections.sort(impressionDistribution, 
					new Comparator<ImpressionParamtersDistributionKey>() {
				@Override
				public int compare(ImpressionParamtersDistributionKey o1,
						ImpressionParamtersDistributionKey o2) {
					return o1.getWeight().compareTo(o2.getWeight());
				}
			});

			Map<Integer, Float> campaignWeightVector = new HashMap<Integer, Float>();
			int campaignVectorNormalizationFactor = 0;
			double bid = 0.0;
			for (ImpressionParamtersDistributionKey impressionWithWeight : impressionDistribution) {
				ImpressionParameters impParams = impressionWithWeight.getImpParams();
				
				// Initial bid calculates what does the impression worth
				try {
					bid = initialBid(impParams, publisherName, impressionWithWeight.getWeight());
				} catch (Exception e) {
					log.warning("Failed to get initial bid: " + e + " Stack: " + Arrays.asList(e.getStackTrace()));
					try {
						bid = getLastBidAveraged(impParams, publisherName);
						log.warning(rname + ": Failed to classifiy initial bid. Returning last bid averaged = " + bid);
					} catch (Exception e2) {
						log.warning("Failed to get last bid averaged - maybe there's no last bid: " + Arrays.asList(e2.getStackTrace()));
						throw e2;
					}
				}
				
				// Now we will calculate what does the impression worth *for us*.
				List<CampaignData> relevantCampaigns = filterCampaigns(impParams);
				
				if (exists(relevantCampaigns)) {
				//	log.info(rname + ": Prioritizing over relevant campaigns...");
					prioritizeCampaigns(relevantCampaigns);
					//log.info(rname + ": Priority result is = " + relevantCampaigns);
					
					//log.info(rname + ": Updating campaign weight vector...");
					campaignVectorNormalizationFactor = updateCampaignVector(campaignWeightVector, relevantCampaigns);
					//log.info(rname + ": Updated campaign vector is = " + campaignWeightVector);
					
					log.info(rname + ": Initial bid is: " + bid);
					bid = calcBid(bid, relevantCampaigns, publisherName, impParams.getMarketSegments(), campaignWeightVector);
					log.info(rname + ": New bid is = " + bid);
				}

				addToBidBundle(publisherName, impParams, CPM*bid, campaignWeightVector, campaignVectorNormalizationFactor); // Question: do we bid per impression or per 1000 impressions?
			}
			
			// Unknown market segment imp params...
			for (Device device : Device.values()) {
				for (AdType adType : AdType.values()) {
				//	log.info(rname + ": Market segment is unknown, bidding only for urgent campaigns");
					
					List<CampaignData> urgentCampaigns = getUrgentCampaigns();
					//log.info(rname + ": Urgent campaigns = " + urgentCampaigns);
					if (urgentCampaigns.size() == 0) {
				//		log.info("No urgent campaigns.");
					}
					
					//log.info(rname + ": Updating campaign weight vector...");
					campaignVectorNormalizationFactor = updateCampaignVector(campaignWeightVector, urgentCampaigns);
				//	log.info(rname + ": Updated campaign vector is = " + campaignWeightVector);
					
					bid = calcBidForUnknown(urgentCampaigns, publisherName, device, adType);
					log.info(rname + ": Bid for unknown market segment and urgent campaigns is "  + bid);
					
					addToBidBundle(publisherName, new ImpressionParameters(new HashSet<MarketSegment>(), device, adType), bid, campaignWeightVector, campaignVectorNormalizationFactor);
				}
			}

		}
	}
	
	private int updateCampaignVector(Map<Integer, Float> campaignWeightVector, List<CampaignData> relevantCampaigns) {
		float sumCampaignPriorities = 0;
		for (CampaignData relevantCampaign : relevantCampaigns) {
			sumCampaignPriorities += relevantCampaign.getCampaignPriority(dayBiddingFor);
		}
		
		for (CampaignData relevantCampaign : relevantCampaigns) {
			campaignWeightVector.put(relevantCampaign.getId(), relevantCampaign.getCampaignPriority(dayBiddingFor)/sumCampaignPriorities);
		}
		
		return Math.round(sumCampaignPriorities);
	}
	
	private double initialBid(ImpressionParameters impParams, String publisherName, Double weight) throws Exception {
		concreteClassifier.generateFirstInstance(impParams, publisherName, weight);
		return concreteClassifier.classifyLastInstance();
	}

	private double getLastBidAveraged(ImpressionParameters impParams, String publisherName) {
		Set<MarketSegment> marketSegments = impParams.getMarketSegments();
		Device device = impParams.getDevice();
		AdType adType = impParams.getAdType();
			
		return previousBidBundle.getBid(new AdxQuery(publisherName, marketSegments, device, adType));	
	}

	private void addToBidBundle(String publisherName, ImpressionParameters impParams, double bid, Map<Integer, Float> campaignWeightVector, int normalizationFactor) {
		for (Integer campaignId : campaignWeightVector.keySet()) {
			AdxQuery query = new AdxQuery(publisherName, impParams.getMarketSegments(), impParams.getDevice(), impParams.getAdType());
			bidBundle.addQuery(query, bid, new Ad(null), campaignId, Math.round(campaignWeightVector.get(campaignId)*normalizationFactor));
		}
		
	}

	private double calcBidForUnknown(List<CampaignData> urgentCampaigns, String publisherName, Device device, AdType adType) throws Exception {
		double sumBids = 0;
		//log.info("calcBidForUnknown: UrgentCampaigns = " + urgentCampaigns);
		
		// Minimal should be last...
		float leastPriority = urgentCampaigns.get(urgentCampaigns.size()-1).getCampaignPriority(dayBiddingFor);
		
		for (CampaignData urgentCampaign : urgentCampaigns) {
			concreteClassifier.generateUnknownInstance(publisherName, device, adType, urgentCampaign, leastPriority);
			double bidForCampaign = concreteClassifier.classifyLastInstance();
			sumBids += bidForCampaign;
		}
		
		return sumBids/urgentCampaigns.size();	
	}

	private List<CampaignData> getUrgentCampaigns() {
		prioritizeCampaigns(myActiveCampaigns);
		return myActiveCampaigns;
	}

	// TODO should we consider daily limits?
	private double calcBid(double currentBid, List<CampaignData> relevantCampaigns, String publisherName, Set<MarketSegment> marketSegment, Map<Integer, Float> campaignWeightVector) throws Exception {
		// TODO: Generate a bid for each of the campaign based on the campaign data and the initial bid
		List<Double> bidsForRelevantCampaigns = new ArrayList<Double>(relevantCampaigns.size());
		for (CampaignData relevantCampaign : relevantCampaigns) {
			
			double currentBidForInstance = concreteClassifier.classifyEnriched(relevantCampaign.getBudget(), campaignWeightVector.get(relevantCampaign.getId()));

			bidsForRelevantCampaigns.add(currentBidForInstance);
			
		}
		// TODO: average all bids using weighted average with the priorities OR take the max bid
		return averageBids(bidsForRelevantCampaigns, relevantCampaigns);
	}
	
	private double averageBids(List<Double> bidsForRelevantCampaigns,
			List<CampaignData> relevantCampaigns) throws Exception {
		double sum = 0;
		double sumPriorities = 0;
		if (bidsForRelevantCampaigns.size() != relevantCampaigns.size()) {
			log.severe("The sizes of the list of bids for relevant campaigns and the size of the list of the relevant campaigns should be equal!");
			throw new Exception("Not enough/Too many Bids for relevant campaigns");
		}
		
		for (int i = 0 ; i < bidsForRelevantCampaigns.size() ; i++) {
			double bid = bidsForRelevantCampaigns.get(i);
			double priority = relevantCampaigns.get(i).getCampaignPriority(dayBiddingFor);
			
			sum += bid*priority;
			sumPriorities += priority;
		}
		
		return sum/sumPriorities;
	}



	private void prioritizeCampaigns(List<CampaignData> relevantCampaigns) {
		Collections.sort(relevantCampaigns, new Comparator<CampaignData>() {
			@Override
			public int compare(CampaignData campaign1, CampaignData campaign2) {
				Float priorityCampaign1 = campaign1.getCampaignPriority(dayBiddingFor);
				Float priorityCampaign2 = campaign2.getCampaignPriority(dayBiddingFor);
				return priorityCampaign1.compareTo(priorityCampaign2);
			}
		});		
	}

	private List<CampaignData> filterCampaigns(ImpressionParameters impParams) {
		String rname = "filterCampaigns";
		
		List<CampaignData> filteredCampaigns = new ArrayList<CampaignData>();
		AdType impressionAdType = impParams.getAdType();
		Device impressionDevice =  impParams.getDevice();
		Set<MarketSegment> impressionMarketSegments = impParams.getMarketSegments();
		
		for (CampaignData campaign : myActiveCampaigns) {
			boolean addedCampaign = false;
			
			// Step1: is the campaign fulfilled?
			if (isFulfilled(campaign)) {
				continue;
			}
			
			// Step2: does the campaign fit the impressions characteristics and market segment?
			Set<MarketSegment> marketSegments = campaign.getTargetSegment();
			AdxQuery[] relevantQueries = campaign.getCampaignQueries();
			for (AdxQuery query : relevantQueries) {
				AdType relevantAd = query.getAdType();
				Device relevantDevice = query.getDevice();
				
				if (relevantAd == impressionAdType && relevantDevice == impressionDevice) { // TODO sanity on that... could be probability!!
					// Campaign should contain a common market segment with the impressio
					if (marketSegments.containsAll(impressionMarketSegments)) { 
						//log.info(rname + ": equal! Adding campaign");
						filteredCampaigns.add(campaign);
						addedCampaign = true;
					}
				}
				
				if (addedCampaign) {
					break;
				}
			}
		}
		
		return filteredCampaigns;
	}

	@SuppressWarnings("unused")
	private String printQueries(AdxQuery[] relevantQueries) {
		if (relevantQueries == null) {
			return "null";
		}
		
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0 ; i < relevantQueries.length; i++) {
			sb.append(relevantQueries[i].toString() + ", ");
		}
		
		sb.append("]");
		
		return sb.toString();
	}

	private boolean isFulfilled(CampaignData campaign) {
		return campaign.impsTogo() == 0;
	}

	private boolean exists(List<CampaignData> campaigns) {
		if (campaigns == null || campaigns.size() == 0) {
			return false;
		}

		return true;	
	}
	
	public void updatePublisherStats(Map<String, PublisherStats> publisherDailyStats) {
		for (String publisherName : publisherDailyStats.keySet()) {
			publishersStats.put(publisherName, publisherDailyStats.get(publisherName));
		}
		
		concreteClassifier.setPublishersStats(publishersStats);
	}
	
	public void updateDay(int day) {
		this.dayBiddingFor = day;
		concreteClassifier.setDayBiddingFor(day);
	}

	public void updateByAdNetReport(AdNetworkReport adnetReport) throws Exception {
		boolean globalHasUpdatedAny = false;
		
		for (AdNetworkKey adnetKey : adnetReport.keys()) {
			AdNetworkReportEntry adnetEntry = adnetReport.getAdNetworkReportEntry(adnetKey);
			CampaignData campaign = findInActiveCampaigns(adnetKey.getCampaignId());
			if (campaign == null) {
				log.warning("Could not find campaign ID " + adnetKey.getCampaignId() + " in active campaigns list");
				continue;
			}
			
			int wins = adnetEntry.getWinCount();
			int bids = adnetEntry.getBidCount();
			double cost = adnetEntry.getCost();
			
			if (wins > 0) { 
				/*	Each adNetKey is containing 3 different market segment: Age-Income, Age-Gender, Gender-Income. 
				 *	Pick only those that match the campaign. If none matched, do nothing as it is mistake of UCS.	*/
				double secondPriceBid = cost / wins;
				// Adding a small random (0-0.1) + ratio of losses/bids to avoid the case where opponents also increase bids!
				globalHasUpdatedAny = updateWithDifferentBid(adnetKey, campaign, secondPriceBid*(1+((bids-wins)/bids+0.1*Math.random()))); 
			} else {
				if (bids > 0) {
					// Lost all bids - meaning we should have increased our bid
					if (bidBundle == null) {
						log.warning("Bid bundle is null? Error");
						break;
					}
					
					double lastBid = bidBundle.getBid(new AdxQuery(adnetKey.getPublisher(), campaign.getTargetSegment(), adnetKey.getDevice(), adnetKey.getAdType()));
					globalHasUpdatedAny = updateWithDifferentBid(adnetKey, campaign, lastBid*(2+(Math.random()-0.5))); // increase bid by 1.5 to 2.5 factor
				} // bids==0, wins==0 -> not interesting for us.
			}
		}
		
		if (globalHasUpdatedAny) {
			concreteClassifier.updateClassifier();
		}
		
	}
	
	private CampaignData findInActiveCampaigns(int campaignId) {
		for (CampaignData campaign : myActiveCampaigns) { // Not more than 58-60 campaigns to search
			if (campaign.getId() == campaignId) {
				return campaign;
			}
		}
		
		return null;
	}

	private boolean updateWithDifferentBid(AdNetworkKey adnetKey, CampaignData campaign, double correctedBid) {
		String publisher = adnetKey.getPublisher();

		Age age = adnetKey.getAge();
		Income income = adnetKey.getIncome();
		Gender gender = adnetKey.getGender();
		
		Set<MarketSegment> ms, ms1, ms2, ms3;
		ms = MarketSegment.extractSegment(new AdxUser(age, gender, income, 0 , 0));
		List<MarketSegment> msList = Arrays.asList((ms.toArray(new MarketSegment[3])));
		ms1 = MarketSegment.compundMarketSegment(msList.get(0), msList.get(1));
		ms2 = MarketSegment.compundMarketSegment(msList.get(0), msList.get(2));
		ms3 = MarketSegment.compundMarketSegment(msList.get(1), msList.get(2));
		
		Device device = adnetKey.getDevice();
		AdType adType = adnetKey.getAdType();
		double campaignsLastPriority = campaign.getCampaignPriority(dayBiddingFor); // -1 or not?
		
		Set<MarketSegment> targetSegment = campaign.getTargetSegment();
		boolean hasUpdatedAny = false;
		
		if (targetSegment.containsAll(ms1)) {
			concreteClassifier.updateInstance(publisher, ms1, device, adType, campaignsLastPriority, correctedBid);
			hasUpdatedAny = true;
		}
		
		if (targetSegment.containsAll(ms2)) {
			concreteClassifier.updateInstance(publisher, ms2, device, adType, campaignsLastPriority, correctedBid);
			hasUpdatedAny = true;
		}
		
		if (targetSegment.containsAll(ms3)) {
			concreteClassifier.updateInstance(publisher, ms3, device, adType, campaignsLastPriority, correctedBid);
			hasUpdatedAny = true;
		}
		
		if (hasUpdatedAny) {
			log.info("For publisher "  + publisher + " and market segment " + targetSegment + " we have updated the impression bidder with corrected bid " + correctedBid);
			return true;
		}
		
		log.info("For publisher " + publisher + " and market segment " + ms + " it was a guess, so we did not update anything. UCS - on your watch!");
		return false;
	}
	
	
	/* Infrastructure */
	protected ImpressionBidder() {	
		userAnalyzer = new UserAnalyzer();
		concreteClassifier = new ImpressionClassifier(userAnalyzer);
	}

	public static ImpressionBidder getInstance() {
		if (instance == null) {
			instance = new ImpressionBidder();
		}

		return instance;
	}

	public PublisherCatalog getPublisherCatalog() {
		return publisherCatalog;
	}

	public void setPublisherCatalog(PublisherCatalog publisherCatalog) {
		this.publisherCatalog = publisherCatalog;
		concreteClassifier.setPublisherCatalog(publisherCatalog);
	}

	public List<CampaignData> getMyActiveCampaigns() {
		return myActiveCampaigns;
	}

	public void setMyActiveCampaigns(List<CampaignData> myActiveCampaigns) {
		this.myActiveCampaigns = myActiveCampaigns;
	}

	public AdxBidBundle getBidBundle() {
		return bidBundle;
	}

	public void setBidBundle(AdxBidBundle bidBundle) {
		this.bidBundle = bidBundle;
	}

	public AdxBidBundle getPreviousBidBundle() {
		return previousBidBundle;
	}

	public void setPreviousBidBundle(AdxBidBundle previousBidBundle) {
		this.previousBidBundle = previousBidBundle;
	}


	


}
