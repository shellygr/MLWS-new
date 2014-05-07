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

	private static boolean DEBUG = false; // Debug flag
	
	private static ImpressionBidder instance = null; // Singleton pattern

	private final Logger log = Logger.getLogger(ImpressionBidder.class.getName()); // Logger

	private PublisherCatalog publisherCatalog;
	private Map<String, PublisherStats> publishersStats = new HashMap<String, PublisherStats>();
	private UserAnalyzer userAnalyzer;
	private ImpressionClassifier concreteClassifier;

	private final static double CPM = 1000.0;

	private List<CampaignData> myActiveCampaigns;

	@SuppressWarnings("unused")
	private double bankBalance; // May be used to set limits?

	private AdxBidBundle bidBundle;
	private AdxBidBundle previousBidBundle;

	private int dayBiddingFor;
	
	private double ucsCurrentLevel = 0.0;

	public void init(Classifier newClassifier, CampaignData currentCampaign, int day) throws Exception { // Called one time during the game - when beginning
		this.dayBiddingFor = day;
		concreteClassifier.setDayBiddingFor(day);
		newClassifier.setDebug(DEBUG);
		//newClassifier.setOptions(Utils.splitOptions("-N 2")); // For SMOReg
		//newClassifier.setOptions(Utils.splitOptions("-B 500")); // For RBFNetwork
		concreteClassifier.init(newClassifier, currentCampaign, 1.0); // Runs only after initial campaign - so have only one campaign, so relative priority is 1.0
	}

	/**
	 * Based on SampleAdNetwork - will be used if an exception occurs in the real bid bundle calculation.
	 * @param queries
	 * @return bidBundle
	 */
	public AdxBidBundle getDefaultBidBundle(AdxQuery[] queries) { 

		bidBundle = new AdxBidBundle();

		for (CampaignData campaign : myActiveCampaigns) {

			/* A fixed random bid, for all queries of the campaign */
			/*
			 * Note: bidding per 1000 imps (CPM) - no more than average budget
			 * revenue per imp
			 */

			Random rnd = new Random();
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
						++entCount;
						bidBundle.addQuery(queries[i], rbid, new Ad(null),
								campaign.getId(), 1);
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

	/**
	 * Main bid bundle calculation method.
	 * Expects an initialised myActiveCampaigns, publisherCatalog 
	 * @throws Exception
	 */
	public void fillBidBundle() throws Exception {

		bidBundle = new AdxBidBundle();

		if (myActiveCampaigns == null || myActiveCampaigns.size() == 0) {
			log.info("No reason to fill bid bundle at all - no active campaigns");
			return;
		}

		log.info("Fill bid bundle for day " + dayBiddingFor);

		for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog.getPublishers()) {
			String publisherName = publisherCatalogEntry.getPublisherName();

			// Calculate bids for known market segments.
			calcKnownBidBundleRecord(publisherName);

			// Calculate bids for unknown market segment.
			if (ucsCurrentLevel != 1.0) { // Only bidding for unknown if our today's UCS level is less than 1.
				calcUnknownBidBundleRecord(publisherName);
			}
		}
		
		bidBundle.setCampaignDailySpendLimit(sumOfRemainingBudgets());
	}

	/*
	 * Returns sum of remaining budgets (approximated) for the active campaigns.
	 */
	private double sumOfRemainingBudgets() {
		double sum = 0;
		
		for (CampaignData campaign : myActiveCampaigns) {
			sum += campaign.getBudget() - campaign.getStats().getCost();
		}
		
		log.info("Sum of remaining budgets for campaigns (used as a daily spend limit): " + sum);
		return sum;
	}

	/**
	 * Adds to bid bundle all bids for records related to this publisher.
	 * @param publisherName
	 * @throws Exception
	 */
	private void calcKnownBidBundleRecord(String publisherName) throws Exception {
		// Calculate the distribution of the different market segments, device and ad-types for this publisher.
		List<ImpressionParamtersDistributionKey> impressionDistribution = userAnalyzer.calcImpressionDistribution(publisherName);
		
		double bid = 0.0;
		
		for (ImpressionParamtersDistributionKey impressionWithWeight : impressionDistribution) {
			ImpressionParameters impParams = impressionWithWeight.getImpParams();
			List<CampaignData> relevantCampaigns = filterCampaigns(impParams); // Filter campaigns relevant to the market segment.

			Map<Integer, Float> campaignWeightVector = new HashMap<Integer, Float>(); // Will map each campaign ID to its weight in the bid.
			int campaignVectorNormalizationFactor = 1; // Weight given to bid bundle in integers, so we will multiply the probability by the same normalisation factor used to create the weights vector.
			
			if (exists(relevantCampaigns)) {
				// Initial bid calculates what does the impression worth.
				try {
					bid = Math.max(0, initialBid(impressionWithWeight.getWeight()));
				} catch (Exception e) {
					log.warning("Failed to get initial bid: " + e + " Stack: " + Arrays.asList(e.getStackTrace()));
					try {
						bid = getLastBid(impParams, publisherName);
						log.warning("Failed to classifiy initial bid. Returning last bid = " + bid);
					} catch (Exception e2) {
						log.warning("Failed to get last bid averaged - maybe there's no last bid: " + Arrays.asList(e2.getStackTrace()));
						throw e2;
					}
				}

				// Now we will calculate what does the impression worth *for us*.
				//prioritizeCampaigns(relevantCampaigns); // Prioritise campaigns
				campaignVectorNormalizationFactor = updateCampaignVector(campaignWeightVector, relevantCampaigns); // Update campaign weight vector

				if (DEBUG) log.info("Initial bid is: " + bid + " for publisher " + publisherName + " and market segment " + impParams.getMarketSegments());
				bid = Math.max(0, calcBid(bid, relevantCampaigns, publisherName, impParams.getMarketSegments(), campaignWeightVector, impParams.getDevice(), impParams.getAdType()));
				if (DEBUG) log.info("Final bid is = " + bid + " for publisher " + publisherName + " and market segment " + impParams.getMarketSegments());
				if (DEBUG) log.info("Campaign weight vector is: " + campaignWeightVector);
				
				if (bid > 0) { 
					addToBidBundle(publisherName, impParams, bid, campaignWeightVector, campaignVectorNormalizationFactor); // Question: do we bid per impression or per 1000 impressions?
				}
			}
		}
	}

	/**
	 * Adds to bid bundle all bids for records related to this publisher which have an "Unknown" market segment.
	 * @param publisherName
	 * @throws Exception
	 */
	private void calcUnknownBidBundleRecord(String publisherName) throws Exception {
		Map<Integer, Float> campaignWeightVector = new HashMap<Integer, Float>();
		int campaignVectorNormalizationFactor = 1;
		double bid = 0.0;
	
		List<CampaignData> urgentCampaigns = getUrgentCampaigns(); // Only for urgent campaigns -that's all campaigns but prioritised already
		if (urgentCampaigns.size() != 0) { // Probably won't fail here as we have tested that case already, but to be on the safe side, we repeat.
			for (Device device : Device.values()) {
				for (AdType adType : AdType.values()) {
					campaignVectorNormalizationFactor = updateCampaignVector(campaignWeightVector, urgentCampaigns); // Update the campaign weight vector

					bid = Math.max(0, calcBidForUnknown(urgentCampaigns, publisherName, device, adType, campaignWeightVector));
					if (DEBUG) log.info("Bid for unknown market segment and urgent campaigns is " + bid);

					addToBidBundle(publisherName, new ImpressionParameters(new HashSet<MarketSegment>(), device, adType), bid, campaignWeightVector, campaignVectorNormalizationFactor);
				}
			}
		}
	}

	/**
	 * Updates the campaignWeightVector map with weights of all relevantCampaigns as if it was a probability vector.
	 * @param campaignWeightVector
	 * @param relevantCampaigns
	 * @return the normalisation factor used to create the vector.
	 */
	private int updateCampaignVector(Map<Integer, Float> campaignWeightVector, List<CampaignData> relevantCampaigns) {
		float sumCampaignPriorities = 0;
		for (CampaignData relevantCampaign : relevantCampaigns) {
			sumCampaignPriorities += relevantCampaign.getCampaignPriority(dayBiddingFor);
		}
		
		if (DEBUG) log.info("Sum of campaign priorities is " + sumCampaignPriorities);

		for (CampaignData relevantCampaign : relevantCampaigns) {
			if (DEBUG) log.info("Campaign " + relevantCampaign.getId() + " priority is " + relevantCampaign.getCampaignPriority(dayBiddingFor));
			campaignWeightVector.put(relevantCampaign.getId(), relevantCampaign.getCampaignPriority(dayBiddingFor)/sumCampaignPriorities);
		}

		return Math.round(sumCampaignPriorities);
	}

	/**
	 * Get the initial bid from the concrete classifier, based on the impression parameters, the publisher and the weight of these impression parameters.
	 * @param weight
	 * @return Initial bid for the BidBundle.
	 * @throws Exception
	 */
	private double initialBid(Double weight) throws Exception {
		concreteClassifier.generateFirstInstance(weight);
		return concreteClassifier.classifyLastInstance();
	}

	/**
	 * Will return the last bid used for these impression parameters and publisher.
	 * As the bid bundle itself may be persistent (according to the code) this may be redundant.
	 * @param impParams
	 * @param publisherName
	 * @return The previous bid for the same bid bundle record.
	 */
	private double getLastBid(ImpressionParameters impParams, String publisherName) {
		Set<MarketSegment> marketSegments = impParams.getMarketSegments();
		Device device = impParams.getDevice();
		AdType adType = impParams.getAdType();

		return previousBidBundle.getBid(new AdxQuery(publisherName, marketSegments, device, adType));	
	}

	/**
	 * Adds a bid to the relevant BidBundle record, with all campaigns that will participate, and their weights.
	 * @param publisherName
	 * @param impParams
	 * @param bid
	 * @param campaignWeightVector
	 * @param normalizationFactor
	 */
	private void addToBidBundle(String publisherName, ImpressionParameters impParams, double bid, Map<Integer, Float> campaignWeightVector, int normalizationFactor) {
		for (Integer campaignId : campaignWeightVector.keySet()) {
			AdxQuery query = new AdxQuery(publisherName, impParams.getMarketSegments(), impParams.getDevice(), impParams.getAdType());
			bidBundle.addQuery(query, bid, new Ad(null), campaignId, Math.round(campaignWeightVector.get(campaignId)*normalizationFactor));
		}
	}

	/**
	 * Given urgentCampaigns, calculate the bid to be used for all of them in the "Unknown" record of the bid bundle.
	 * This is simply the arithmetic average of all bids to these campaigns for "Unknown" market segment.
	 * @param urgentCampaigns
	 * @param publisherName
	 * @param device
	 * @param adType
	 * @param campaignWeightVector 
	 * @return The bid for all urgent campaigns.
	 * @throws Exception
	 */
	private double calcBidForUnknown(List<CampaignData> urgentCampaigns, String publisherName, Device device, AdType adType, Map<Integer, Float> campaignWeightVector) throws Exception {
		double sumBids = 0;

		// Minimal should be last according to the priority.
		float leastPriority = urgentCampaigns.get(urgentCampaigns.size()-1).getCampaignPriority(dayBiddingFor);

		for (CampaignData urgentCampaign : urgentCampaigns) {
			concreteClassifier.generateUnknownInstance(publisherName, device, adType, urgentCampaign, leastPriority);
			double bidForCampaign = concreteClassifier.classifyLastInstance() * campaignWeightVector.get(urgentCampaign.getId());
			sumBids += bidForCampaign;
		}

		return (sumBids/urgentCampaigns.size()) / 50;	
	}

	/**
	 * @return all campaigns sorted by how urgent they are.
	 */
	private List<CampaignData> getUrgentCampaigns() {
		prioritizeCampaigns(myActiveCampaigns);
		return myActiveCampaigns;
	}
	/**
	 * Calculates the bid for all relevant campaigns, based on the initial bid.
	 * @param currentBid
	 * @param relevantCampaigns
	 * @param publisherName
	 * @param marketSegment
	 * @param campaignWeightVector
	 * @param device
	 * @param adType
	 * @return Final bid for this record - based on all available relevant data - averaged by the priorities of the campaigns - priority serves as a weight. 
	 * @throws Exception
	 */
	private double calcBid(double currentBid, List<CampaignData> relevantCampaigns, String publisherName, Set<MarketSegment> marketSegment, Map<Integer, Float> campaignWeightVector, Device device, AdType adType) throws Exception {
		List<Double> bidsForRelevantCampaigns = new ArrayList<Double>(relevantCampaigns.size());
		if (DEBUG) log.info("Calc bid: relevant campaigns size: " + relevantCampaigns.size());
		
		// Will add the bids in the same order as the campaigns are sorted. This is required for average bids.
		for (CampaignData relevantCampaign : relevantCampaigns) {
			double currentBidForInstance = concreteClassifier.classifyEnriched(campaignWeightVector.get(relevantCampaign.getId()), publisherName, marketSegment, device, adType, relevantCampaign.getId());
			bidsForRelevantCampaigns.add(currentBidForInstance);
		}

		return averageBids(bidsForRelevantCampaigns, relevantCampaigns);
	}

	/**
	 * Calculates a weighted average of the bids for the relevant campaigns with the priorities (= weights) of these campaigns.
	 * Assumption: Lists have equal size and each i-th place in both is a pair match - the campaign and its bid.
	 * @param bidsForRelevantCampaigns
	 * @param relevantCampaigns
	 * @return Weighted average of bids by the campaigns' priorities.
	 * @throws Exception
	 */
	private double averageBids(List<Double> bidsForRelevantCampaigns,
			List<CampaignData> relevantCampaigns) throws Exception {
		double sum = 0.0;
		double sumPriorities = 0.0;
		
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

	/**
	 * Returns the relevant campaigns list sorted by decreasing priority.
	 * @param relevantCampaigns
	 */
	private void prioritizeCampaigns(List<CampaignData> relevantCampaigns) {
		Collections.sort(relevantCampaigns, new Comparator<CampaignData>() {
			@Override
			public int compare(CampaignData campaign1, CampaignData campaign2) {
				Float priorityCampaign1 = campaign1.getCampaignPriority(dayBiddingFor);
				Float priorityCampaign2 = campaign2.getCampaignPriority(dayBiddingFor);
				return priorityCampaign2.compareTo(priorityCampaign1);
			}
		});		
	}

	/**
	 * Filter campaigns relevant to the impression parameters given.
	 * @param impParams
	 * @return List of campaigns that match the impression parameters.
	 */
	private List<CampaignData> filterCampaigns(ImpressionParameters impParams) {
		List<CampaignData> filteredCampaigns = new ArrayList<CampaignData>();
		AdType impressionAdType = impParams.getAdType();
		Device impressionDevice =  impParams.getDevice();
		Set<MarketSegment> impressionMarketSegments = impParams.getMarketSegments();

		for (CampaignData campaign : myActiveCampaigns) {
			boolean addedCampaign = false;

			// Step1: Is the campaign fulfilled? Generally should return yes as myActiveCampaigns is initialised. Safety double-check.
			if (isFulfilled(campaign)) {
				continue;
			}

			// Step2: Does the campaign fit the impressions characteristics and market segment?
			Set<MarketSegment> marketSegments = campaign.getTargetSegment();
			AdxQuery[] relevantQueries = campaign.getCampaignQueries();
			for (AdxQuery query : relevantQueries) {
				AdType relevantAd = query.getAdType();
				Device relevantDevice = query.getDevice();

				if (relevantAd == impressionAdType && relevantDevice == impressionDevice) {
					// Campaign should contain a common market segment with the impression parameters.
					if (impressionMarketSegments.containsAll(marketSegments)) { 
						filteredCampaigns.add(campaign);
						addedCampaign = true;
					}
				}

				if (addedCampaign) { // Already added the campaign.
					break;
				}
			}
		}

		return filteredCampaigns;
	}

	/**
	 * @param campaign
	 * @return True if the campaign is already fulfilled.
	 */
	private boolean isFulfilled(CampaignData campaign) {
		return campaign.impsTogo() == 0; // impsTogo() >= 0.
	}

	/**
	 * @param campaigns
	 * @return True if the list of campaigns is not empty.
	 */
	private boolean exists(List<CampaignData> campaigns) {
		if (campaigns == null || campaigns.size() == 0) {
			return false;
		}

		return true;	
	}

	/**
	 * Updates the publisher statistics received for both the bidder and the classifier.
	 * @param publisherDailyStats
	 */
	public void updatePublisherStats(Map<String, PublisherStats> publisherDailyStats) {
		for (String publisherName : publisherDailyStats.keySet()) {
			publishersStats.put(publisherName, publisherDailyStats.get(publisherName));
		}

		concreteClassifier.setPublishersStats(publishersStats);
		userAnalyzer.setPublisherStats(publishersStats);
	}

	/**
	 * Updates the relevant day for bidding in both the bidder and the classifier.
	 * @param day
	 */
	public void updateDay(int day) {
		this.dayBiddingFor = day;
		concreteClassifier.setDayBiddingFor(day);
	}

	/**
	 * Updates the classifier with corrected bids, in light of the received AdNetworkReport.
	 * Note: In this context, myActiveCampaigns is relevant to the last simulation status
	 * @param adnetReport
	 * @throws Exception
	 */
	public void updateByAdNetReport(AdNetworkReport adnetReport) throws Exception {
		boolean globalHasUpdatedAny = false; // Has updated any instance in the dataset?

		if (DEBUG) log.info("Current instnaces : " + concreteClassifier.printLastInstancesMap());
		if (DEBUG) log.info("AdnetReport: " + adnetReport);
		
		for (AdNetworkKey adnetKey : adnetReport.keys()) {
			AdNetworkReportEntry adnetEntry = adnetReport.getAdNetworkReportEntry(adnetKey);
			CampaignData campaign = findInActiveCampaigns(adnetKey.getCampaignId());
			if (campaign == null) { // Only updated if this campaign appears in the last day's active campaigns list.
				if (DEBUG) log.info("Skipping adnet report");
				continue;
			}

			int wins = adnetEntry.getWinCount();
			int bids = adnetEntry.getBidCount();
			double cost = adnetEntry.getCost();

			if (wins > 0) { 
				double secondPriceBid = cost / wins; // The second price bid we paid is what we paid overall divided by the number of wins we had.
				
				// Adding a small random (0-0.1) + ratio of losses/bids to avoid the case where opponents also increase bids!
				double correctedBid =  secondPriceBid * (1 + ( ((bids - wins) / bids) + (0.1 * Math.random()) )); 
				
				globalHasUpdatedAny |= updateWithDifferentBid(adnetKey, campaign, correctedBid); 
			} else {
				if (bids > 0) {
					// Lost all bids - meaning we should have increased our bid
					if (bidBundle == null) {
						log.severe("Bid bundle is null - Severe error - probably has nothing to update as no last bid will be available");
						return;
					}

					double lastBid = bidBundle.getBid(new AdxQuery(adnetKey.getPublisher(), campaign.getTargetSegment(), adnetKey.getDevice(), adnetKey.getAdType()));
					globalHasUpdatedAny |= updateWithDifferentBid(adnetKey, campaign, lastBid*(2+(Math.random()-0.5))); // increase bid by 1.5 to 2.5 factor
				} // bids==0, wins==0 -> Not interesting - we gave no bids.
			}
		}

		if (globalHasUpdatedAny) {
			log.info("Retraining the classifier with updated instances");
			concreteClassifier.updateClassifier();
		}
	}

	/**
	 * Search for the given campaign ID it's actual data structure in the myActiveCampaigns list.
	 * @param campaignId
	 * @return The CampaignData object of the campaign with id campaignId if it's active and null otherwise.
	 */
	private CampaignData findInActiveCampaigns(int campaignId) {
		for (CampaignData campaign : myActiveCampaigns) { // Not more than 58-60 campaigns to search - so no major performance degradation compared to a map.
			if (campaign.getId() == campaignId) {
				return campaign;
			}
		}

		return null;
	}

	/**
	 * Update one or more instances of our dataset with the corrected bid.
	 * @param adnetKey
	 * @param campaign
	 * @param correctedBid
	 * @return True if any instances were actually updated.
	 */
	/*	Each adNetKey is containing 3 different market segment: Age-Income, Age-Gender, Gender-Income. 
	 *	Pick only those that match the campaign. If none matched, do nothing as it is mistake of UCS - not something our bidder can learn something of.	*/
	private boolean updateWithDifferentBid(AdNetworkKey adnetKey, CampaignData campaign, double correctedBid) {
		String publisher = adnetKey.getPublisher(); // Publisher data

		// User data
		Age age = adnetKey.getAge();
		Income income = adnetKey.getIncome();
		Gender gender = adnetKey.getGender();

		Set<MarketSegment> ms, ms1, ms2, ms3, ms1_2, ms2_2, ms3_2;
		ms = MarketSegment.extractSegment(new AdxUser(age, gender, income, 0 , 0)); // Set containing all the user's market segments.
		
		// ms1, ms2, ms3 = For single market segments
		// ms1_2, ms2_2, ms3_2 = The 3 pairs possible of market segment pairs for the user: Age-Income, Age-Gender, Gender-Income.
		// ms is for the complete market segment
		List<MarketSegment> msList = Arrays.asList((ms.toArray(new MarketSegment[3])));
		ms1 = MarketSegment.compundMarketSegment1(msList.get(0));
		ms2 = MarketSegment.compundMarketSegment1(msList.get(1));
		ms3 = MarketSegment.compundMarketSegment1(msList.get(2));
		ms1_2 = MarketSegment.compundMarketSegment2(msList.get(0), msList.get(1));
		ms2_2 = MarketSegment.compundMarketSegment2(msList.get(0), msList.get(2));
		ms3_2 = MarketSegment.compundMarketSegment2(msList.get(1), msList.get(2));

		Device device = adnetKey.getDevice();
		AdType adType = adnetKey.getAdType();

		Set<MarketSegment> targetSegment = campaign.getTargetSegment();
		boolean hasUpdatedAny = false;

		// Target segment should completely match the market segment given in the key (after splitting it to pairs).
		if (ms1.containsAll(targetSegment)) {
			concreteClassifier.updateInstance(publisher, ms1, device, adType, campaign.getId(), correctedBid);
			hasUpdatedAny = true;
		}
		
		if (ms2.containsAll(targetSegment)) {
			concreteClassifier.updateInstance(publisher, ms2, device, adType, campaign.getId(), correctedBid);
			hasUpdatedAny = true;
		}
		
		if (ms3.containsAll(targetSegment)) {
			concreteClassifier.updateInstance(publisher, ms3, device, adType, campaign.getId(), correctedBid);
			hasUpdatedAny = true;
		}
		
		if (ms1_2.containsAll(targetSegment)) {
			concreteClassifier.updateInstance(publisher, ms1_2, device, adType, campaign.getId(), correctedBid);
			hasUpdatedAny = true;
		}

		if (ms2_2.containsAll(targetSegment)) {
			concreteClassifier.updateInstance(publisher, ms2_2, device, adType, campaign.getId(), correctedBid);
			hasUpdatedAny = true;
		}

		if (ms3_2.containsAll(targetSegment)) {
			concreteClassifier.updateInstance(publisher, ms3_2, device, adType, campaign.getId(), correctedBid);
			hasUpdatedAny = true;
		}
		
		if (ms.containsAll(targetSegment)) {
			concreteClassifier.updateInstance(publisher, ms, device, adType, campaign.getId(), correctedBid);
			hasUpdatedAny = true;
		}

		if (hasUpdatedAny) {
			if (DEBUG) log.info("For publisher "  + publisher + " and market segment " + targetSegment + " we have updated the impression bidder with corrected bid " + correctedBid);
			return true;
		}

		if (DEBUG) log.info("For publisher " + publisher + " and market segment " + ms + " it was a guess, so we did not update anything. UCS - on your watch!");
		return false;
	}
	
	/**
	 * Generate default instances and insert them to the dataset.
	 * @param pendingCampaign
	 */
	public void updateForNewCampaign(CampaignData pendingCampaign) {
		try {
			double campaignRelativePriority = calaculateRelativePriority(pendingCampaign); // Should know the current relative priority of the campaign in order to give a suggested bid.
			concreteClassifier.updateForNewCampaign(pendingCampaign, campaignRelativePriority);
		} catch (Exception e) {
			log.warning("Failed to update concrete classifier for new Campaign" + Arrays.asList(e.getStackTrace()));
		}		
	}

	/**
	 * @param pendingCampaign
	 * @return Relative priority of the pending won campaign, relative to other active campaigns.
	 * Note:	We run in context of previous simulation status, so should purge the campaigns that are not active in the upcoming day.
	 * 			Right now, dayBiddingFor is initialised to the last day when simulation status message was received. So increase by 1.
	 */
	private double calaculateRelativePriority(CampaignData pendingCampaign) {
		List<CampaignData> activeCampaigns = purgeInactive(myActiveCampaigns, dayBiddingFor + 1);
		activeCampaigns.add(pendingCampaign);
		//prioritizeCampaigns(activeCampaigns);
		HashMap<Integer,Float> campaignWeightVector = new HashMap<Integer, Float>();
		updateCampaignVector(campaignWeightVector, activeCampaigns);
		return campaignWeightVector.get(pendingCampaign.getId());
	}

	/**
	 * Filters inactive campaigns from the campaigns list, according to the given day.
	 * @param campaigns
	 * @param day
	 * @return Filtered list of campaigns, containing only campaigns active in day.
	 * Note: Assumes that the impsTogo() for the day are known already, if not, results may not be actually correct when the day comes.
	 */
	private List<CampaignData> purgeInactive(List<CampaignData> campaigns, int day) {
		List<CampaignData> activeCampaigns = new ArrayList<CampaignData>();
		
		for (CampaignData campaign : campaigns) {
			if (campaign.isActive(day)) {
				activeCampaigns.add(campaign);
			}
		}
		
		return activeCampaigns;
	}


	/* Infrastructure */

	/*
	 * Constructor
	 */
	protected ImpressionBidder() {	
		userAnalyzer = new UserAnalyzer();
		concreteClassifier = new ImpressionClassifier(userAnalyzer);
	}

	/*
	 * Singleton pattern
	 */
	public static ImpressionBidder getInstance() {
		if (instance == null) {
			instance = new ImpressionBidder();
		}

		return instance;
	}

	
	/*
	 * Getters / Setters
	 */
	
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

	public void setDatasetAsNull() {
		concreteClassifier.setDatasetAsNull();
	}

	public void setCurrentUcsLevel(double ucsCurrentLevel) {
		this.ucsCurrentLevel = ucsCurrentLevel;
	}

}
