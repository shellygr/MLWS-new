package OrOmerShelly.oos.bidders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import OrOmerShelly.CampaignData;
import OrOmerShelly.ImpressionParameters;
import OrOmerShelly.ImpressionParamtersDistributionKey;
import OrOmerShelly.PublisherStats;
import OrOmerShelly.UserAnalyzer;

import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.props.PublisherCatalogEntry;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.devices.Device;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.SparseInstance;
import weka.core.Instances;
import edu.umich.eecs.tac.props.Ad;

public class ImpressionBidder {

	private static ImpressionBidder instance = null;

	private final Logger log = Logger
			.getLogger(ImpressionBidder.class.getName());

	private final static double CPM = 1000.0;

	private static final String INSTANCES_NAME = "IMPRESSION_BIDDER_INSTANCES";
	
	private static final int PUBLISHER_POPULARITY_ATTR_INDEX = 0;
	private static final int DEVICE_ATTR_INDEX = 1;
	private static final int AD_TYPE_ATTR_INDEX = 2;
	private static final int AD_TYPE_ORIENTATION_ATTR_INDEX = 3;
	private static final int WEIGHT_OF_MARKET_SEGMENT_ATTR_INDEX = 4;
	private static final int REMAINING_BUDGET_ATTR_INDEX = 5;
	private static final int PRIORITY_ATTR_INDEX = 6;
	private final static int BID_ATTR_CLASS_VALUE_INDEX = 7;
	private final static int IS_UPDATED_INDEX = 8;
	
	private final static int CAPACITY_OF_INSTANCES = 10000;

	private UserAnalyzer userAnalyzer = new UserAnalyzer();
	private PublisherCatalog publisherCatalog;
	private Map<String, PublisherStats> publishersStats = new HashMap<String, PublisherStats>();
	private List<CampaignData> myActiveCampaigns; // mutable!!!
	@SuppressWarnings("unused")
	private double bankBalance;
	
	private AdxBidBundle bidBundle;
	private AdxBidBundle previousBidBundle;
	
	// Map publisher, market segment, device, ad type to Instance index
	private HashMap<InstanceIndexKey, Integer> lastInstancesIndicesMap = new HashMap<InstanceIndexKey, Integer>();
	private Instance lastInstance = null; // Doesn't contain campaign information and bid
	private Instances dataset = null; // On the first day this is a example dataset which is based on budget/reach impressions.
	private Classifier classifier; // Must be an updateable classifier
	
	private int dayBiddingFor; // TODO: check exactly which day should be given: current day or day bidding for (that is currentDay + 1)?
	
	public void init(Classifier newClassifier, CampaignData currentCampaign, int day) throws Exception { // Called one time during the game or when a classifier changes
		this.dayBiddingFor = day;
		classifier = newClassifier;
		dataset = getDefaultDataset(currentCampaign);
		trainClassifier();
	}
	
	public void updateForNewCampaign(CampaignData currentCampaign) throws Exception {
		Instances newInstances = getDefaultDataset(currentCampaign);
		@SuppressWarnings("unchecked")
		Enumeration<Instance> enumerationNewInstances = (Enumeration<Instance>) newInstances.enumerateInstances();
		
		while (enumerationNewInstances.hasMoreElements()) {
			dataset.add(enumerationNewInstances.nextElement());
		}
		
		trainClassifier();
	}
	
	// TODO: set daily limits for campaign and overall - do not want to get a minus in bankBalance?
	private static final double PC = 1.0;
	private static final double MOBILE = 2.0;
	private static final double TEXT = 1.0;
	private static final double VIDEO = 2.0;
	
	private static final int NOT_UPDATED = 0;
	private static final int UPDATED = 1;
	
	// Default dataset is not fixed, only concrete instances
	private Instances getDefaultDataset(CampaignData currentCampaign) {
		String rname = "getDefaultDataset";
		
		FastVector attributeNames = new FastVector();
		attributeNames.insertElementAt(new Attribute("popularity"), PUBLISHER_POPULARITY_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("device"), DEVICE_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("adType"), AD_TYPE_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("adTypeOrientation"), AD_TYPE_ORIENTATION_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("weightMarketSegment"), WEIGHT_OF_MARKET_SEGMENT_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("remainingBudget"), REMAINING_BUDGET_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("priority"), PRIORITY_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("bid"), BID_ATTR_CLASS_VALUE_INDEX);
	//	attributeNames.insertElementAt(new Attribute("isUpdated"), IS_UPDATED_INDEX);
		
		Instances defaultInstances = new Instances(INSTANCES_NAME, 
				attributeNames,
				CAPACITY_OF_INSTANCES); // Test this... Weka3.7 is more intuitive using a list and not a FastVector
		
		
		/* Adding default instances - base bid is like in sample ad network, but want to incorporate market segment matching and weight consideration */
		double budget = currentCampaign.getBudget();
		double priority = getCampaignPriority(currentCampaign);

		int countInstances = (dataset == null ? 0 : dataset.numInstances()-1);
		// Video / Mobile - are worth more according to mobileCoeff, videoCoeff. So take them into account.
		for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog.getPublishers()) {
			String publisherName = publisherCatalogEntry.getPublisherName();
			log.info(rname + ": Generating default instances for publisher - " + publisherName);

			double median = userAnalyzer.calcMedianOfMarketSegmentsWeights(publisherName);
			double publishersMarketSegmentWeight = userAnalyzer.getMarketSegmentWeight(currentCampaign.getTargetSegment(), publisherName);
			double avgCmpRevenuePerImp = budget / currentCampaign.getReachImps();
			
			PublisherStats publisherStats = publishersStats.get(publisherName);
			if (publisherStats == null) {
				log.info(rname + ": Getting averaged publisher stats as there is no record for publisher " + publisherName);
				publisherStats = userAnalyzer.getAveragedPublisherStatsForPublisher(publisherName);
			}
						
			double adTypeOrientation = publisherStats.getVideoOrientation() / publisherStats.getTextOrientation();
			
			for (Device device : Device.values()) {
				for (AdType adType : AdType.values()) {
					double[] attributes = new double[7];
					attributes[0] = deviceSerialize(device);
					attributes[1] = adTypeSerialize(adType);
					attributes[2] = adTypeOrientation; 
					attributes[3] = publishersMarketSegmentWeight;
					attributes[4] = budget;
					attributes[5] = priority;
					attributes[6] = avgCmpRevenuePerImp
										*priority
										*(publishersMarketSegmentWeight/median)
										*(device == Device.mobile ? currentCampaign.getMobileCoef() : 1.0)
										*(adType == AdType.text ? 1.0 : adTypeOrientation*currentCampaign.getVideoCoef());
					//attributes[7] = NOT_UPDATED;
					
					
					int[] indicesToFill = new int[7];
					indicesToFill[0] = DEVICE_ATTR_INDEX;
					indicesToFill[1] = AD_TYPE_ATTR_INDEX;
					indicesToFill[2] = AD_TYPE_ORIENTATION_ATTR_INDEX;
					indicesToFill[3] = WEIGHT_OF_MARKET_SEGMENT_ATTR_INDEX;
					indicesToFill[4] = REMAINING_BUDGET_ATTR_INDEX;
					indicesToFill[5] = PRIORITY_ATTR_INDEX;
					indicesToFill[6] = BID_ATTR_CLASS_VALUE_INDEX;
					//indicesToFill[7] = IS_UPDATED_INDEX;
					
					log.info(rname + ": Adding instance - " + Arrays.toString(attributes));
					
					Instance defaultInstance = new SparseInstance(1, attributes, indicesToFill, 1); 
					defaultInstances.add(defaultInstance);
					lastInstancesIndicesMap.put(new InstanceIndexKey(publisherName, currentCampaign.getTargetSegment(), device, adType, priority), countInstances); // -1 or no -1?
					++countInstances;
				}
			}
		}
		
		// TODO: add unknown market segment case - device, adType known, market segment weight not.
		
		defaultInstances.setClassIndex(BID_ATTR_CLASS_VALUE_INDEX);
		return defaultInstances;
		
	}

	private double deviceSerialize(Device device) {
		return (device == Device.mobile ? MOBILE : PC);
	}
	
	private double adTypeSerialize(AdType adType) {
		return (adType == AdType.text ? TEXT : VIDEO);
	}


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

		for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog.getPublishers()) {
			String publisherName = publisherCatalogEntry.getPublisherName();
			log.info(rname + ": Filling bid bundle for " + publisherName);
			
			List<ImpressionParamtersDistributionKey> impressionDistribution = userAnalyzer.calcImpressionDistribution(publisherName);
			
			Collections.sort(impressionDistribution, 
					new Comparator<ImpressionParamtersDistributionKey>() {
				@Override
				public int compare(ImpressionParamtersDistributionKey o1,
						ImpressionParamtersDistributionKey o2) {
					return o1.getWeight().compareTo(o2.getWeight());
				}
			});

			Map<Integer,Integer> campaignWeightVector = new HashMap<Integer,Integer>();
			double bid = 0.0;
			for (ImpressionParamtersDistributionKey impressionWithWeight : impressionDistribution) {
				ImpressionParameters impParams = impressionWithWeight.getImpParams();
				log.info(rname + ": Working on impParams = " + impParams);
				
				// Initial bid calculates what does the impression worth
				try {
					bid = initialBid(impParams, publisherName, impressionWithWeight.getWeight());
					log.info(rname + ": Initial bid is: " + bid);
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
				log.info(rname + ": Relevant campaigns found are = " + relevantCampaigns);
				
				if (exists(relevantCampaigns)) {
					log.info(rname + ": Prioritizing over relevant campaigns...");
					prioritizeCampaigns(relevantCampaigns);
					log.info(rname + ": Priority result is = " + relevantCampaigns);
					
					log.info(rname + ": Updating campaign weight vector...");
					updateCampaignVector(campaignWeightVector, relevantCampaigns);
					log.info(rname + ": Updated campaign vector is = " + campaignWeightVector);
					
					bid = calcBid(bid, relevantCampaigns, publisherName, impParams.getMarketSegments());
					log.info(rname + ": New bid is = " + bid);
				} else {
					if (isUnknown(impParams)) {
						log.info(rname + ": Market segment is unknown, bidding only for urgent campaigns");
						
						List<CampaignData> urgentCampaigns = getUrgentCampaigns();
						log.info(rname + ": Urgent campaigns = " + urgentCampaigns);
						
						log.info(rname + ": Updating campaign weight vector...");
						updateCampaignVector(campaignWeightVector, urgentCampaigns);
						log.info(rname + ": Updated campaign vector is = " + campaignWeightVector);
						
						bid = calcBidForUnknown(urgentCampaigns, publisherName, impParams.getDevice(), impParams.getAdType());
						log.info(rname + ": Bid for unknown market segment and urgent campaigns is "  + bid);
					}
				}

				addToBidBundle(publisherName, impParams, CPM*bid, campaignWeightVector); // Question: do we bid per impression or per 1000 impressions?
			}

		}
	}
	
	private void updateCampaignVector(Map<Integer, Integer> campaignWeightVector, List<CampaignData> relevantCampaigns) {
		for (CampaignData relevantCampaign : relevantCampaigns) {
			campaignWeightVector.put(relevantCampaign.getId(), Math.round((getCampaignPriority(relevantCampaign))));
		}
	}

	private void generateUnknownInstance(String publisherName, Device device, AdType adType, CampaignData urgentCampaign, float leastPriority) {
		PublisherStats publisherStats = publishersStats.get(publisherName);
		double adTypeOrientation = getAdTypeOrientation(publisherStats);
		float thisCampaignPriority = getCampaignPriority(urgentCampaign);
		
		double[] attributes = new double[6];
		attributes[0] = deviceSerialize(device);
		attributes[1] = adTypeSerialize(adType);
		attributes[2] = adTypeOrientation;
		attributes[3] = userAnalyzer.getMarketSegmentWeight(urgentCampaign.getTargetSegment(), publisherName);
		attributes[4] = urgentCampaign.getBudget();
		attributes[5] = thisCampaignPriority * thisCampaignPriority/leastPriority; // Increase priority by how much more urgent it is than the least urgent campaign.
		//attributes[6] = NOT_UPDATED;
		
		int[] indicesToFill = new int[6];
		indicesToFill[0] = DEVICE_ATTR_INDEX;
		indicesToFill[1] = AD_TYPE_ATTR_INDEX;
		indicesToFill[2] = AD_TYPE_ORIENTATION_ATTR_INDEX;
		indicesToFill[3] = WEIGHT_OF_MARKET_SEGMENT_ATTR_INDEX;
		indicesToFill[4] = REMAINING_BUDGET_ATTR_INDEX;
		indicesToFill[5] = PRIORITY_ATTR_INDEX;
		//indicesToFill[6] = IS_UPDATED_INDEX;
		
		lastInstance = new SparseInstance(1, attributes, indicesToFill, 1);
		dataset.add(lastInstance);
		lastInstancesIndicesMap.put(new InstanceIndexKey(publisherName, urgentCampaign.getTargetSegment(), device, adType, thisCampaignPriority * thisCampaignPriority/leastPriority), dataset.numInstances()-1);
	}
	
	private void generateFirstInstance(ImpressionParameters impParams,
			String publisherName, Double weight) {
		PublisherStats publisherStats = publishersStats.get(publisherName);
		double adTypeOrientation = getAdTypeOrientation(publisherStats);
		
		double[] attributes = new double[4];
		attributes[0] = deviceSerialize(impParams.getDevice());
		attributes[1] = adTypeSerialize(impParams.getAdType());
		attributes[2] = adTypeOrientation;
		attributes[3] = weight;
		//attributes[4] = NOT_UPDATED;
		
		int[] indicesToFill = new int[4];
		indicesToFill[0] = DEVICE_ATTR_INDEX;
		indicesToFill[1] = AD_TYPE_ATTR_INDEX;
		indicesToFill[2] = AD_TYPE_ORIENTATION_ATTR_INDEX;
		indicesToFill[3] = WEIGHT_OF_MARKET_SEGMENT_ATTR_INDEX;
	//	indicesToFill[4] = IS_UPDATED_INDEX;
		
		lastInstance = new SparseInstance(1, attributes, indicesToFill, 1);
		dataset.add(lastInstance);
		lastInstancesIndicesMap.put(new InstanceIndexKey(publisherName, impParams.getMarketSegments(), impParams.getDevice(), impParams.getAdType(), -1), dataset.numInstances()-1);
	}

	private double initialBid(ImpressionParameters impParams, String publisherName, Double weight) throws Exception {
		// Define an instance for classification - filter relevant data according to stage
		generateFirstInstance(impParams, publisherName, weight);
		return (double)classifier.classifyInstance(lastInstance);
	}



	private double getLastBidAveraged(ImpressionParameters impParams, String publisherName) {
		Set<MarketSegment> marketSegments = impParams.getMarketSegments();
		Device device = impParams.getDevice();
		AdType adType = impParams.getAdType();
			
		return previousBidBundle.getBid(new AdxQuery(publisherName, marketSegments, device, adType));	
	}

	private double getAdTypeOrientation(PublisherStats publisherStats) {
		return (publisherStats == null ? 1.0 : publisherStats.getVideoOrientation() / publisherStats.getTextOrientation());
	}

	private void addToBidBundle(String publisherName, ImpressionParameters impParams, double bid, Map<Integer, Integer> campaignWeightVector) {
		for (Integer campaignId : campaignWeightVector.keySet()) {
			AdxQuery query = new AdxQuery(publisherName, impParams.getMarketSegments(), impParams.getDevice(), impParams.getAdType());
			bidBundle.addQuery(query, bid, new Ad(null), campaignId, campaignWeightVector.get(campaignId));
		}
		
	}

	private double calcBidForUnknown(List<CampaignData> urgentCampaigns, String publisherName, Device device, AdType adType) throws Exception {
		double sumBids = 0;
		log.info("calcBidForUnknown: UrgentCampaigns = " + urgentCampaigns);
		
		// Minimal should be last...
		float leastPriority = getCampaignPriority(urgentCampaigns.get(urgentCampaigns.size()-1));
		
		for (CampaignData urgentCampaign : urgentCampaigns) {
			generateUnknownInstance(publisherName, device, adType, urgentCampaign, leastPriority);

			double bidForCampaign = classifier.classifyInstance(lastInstance);
			sumBids += bidForCampaign;
		}
		
		return sumBids/urgentCampaigns.size();	
	}

	private List<CampaignData> getUrgentCampaigns() {
		prioritizeCampaigns(myActiveCampaigns);
		return myActiveCampaigns;
	}

	private boolean isUnknown(ImpressionParameters impParams) {
		Set<MarketSegment> marketSegments = impParams.getMarketSegments();
		return marketSegments == null || marketSegments.isEmpty();	
	}

	// TODO should we consider daily limits?
	private double calcBid(double currentBid, List<CampaignData> relevantCampaigns, String publisherName, Set<MarketSegment> marketSegment) throws Exception {
		// TODO: Generate a bid for each of the campaign based on the campaign data and the initial bid
		List<Double> bidsForRelevantCampaigns = new ArrayList<Double>(relevantCampaigns.size());
		for (CampaignData relevantCampaign : relevantCampaigns) {
			Instance campaignInstnace = enrichInstance(relevantCampaign.getBudget(), // TODO Remaining budget, not whole budget
					getCampaignPriority(relevantCampaign));
			
			dataset.add(campaignInstnace);			
			double currentBidForInstance = classifier.classifyInstance(campaignInstnace); // TODO why 0?
			bidsForRelevantCampaigns.add(currentBidForInstance);
			campaignInstnace.setClassValue(currentBidForInstance);
		}
		// TODO: average all bids using weighted average with the priorities OR take the max bid
		return averageBids(bidsForRelevantCampaigns, relevantCampaigns);
	}
	
	private double averageBids(List<Double> bidsForRelevantCampaigns,
			List<CampaignData> relevantCampaigns) throws Exception {
		int sum = 0;
		int sumPriorities = 0;
		if (bidsForRelevantCampaigns.size() != relevantCampaigns.size()) {
			log.severe("The sizes of the list of bids for relevant campaigns and the size of the list of the relevant campaigns should be equal!");
			throw new Exception("Not enough/Too many Bids for relevant campaigns");
		}
		
		for (int i = 0 ; i < bidsForRelevantCampaigns.size() ; i++) {
			double bid = bidsForRelevantCampaigns.get(i);
			double priority = getCampaignPriority(relevantCampaigns.get(i));
			
			sum += bid*priority;
			sumPriorities += priority;
		}
		
		return sum/sumPriorities;
	}

	private Instance enrichInstance(double remainingBudget, float priority) {
		Instance enriched = new SparseInstance(lastInstance); 		
		enriched.setValue(REMAINING_BUDGET_ATTR_INDEX, remainingBudget);
		enriched.setValue(PRIORITY_ATTR_INDEX, priority);
		return enriched;
	}

	// TODO move as a method of CampaignData
	public float getCampaignPriority(CampaignData campaign) { // TODO: check that can't divide by zero
		return (float)campaign.impsTogo() / ((float)(campaign.getDayEnd() - dayBiddingFor));
	}

	private void prioritizeCampaigns(List<CampaignData> relevantCampaigns) {
		Collections.sort(relevantCampaigns, new Comparator<CampaignData>() {
			@Override
			public int compare(CampaignData campaign1, CampaignData campaign2) {
				Float priorityCampaign1 = getCampaignPriority(campaign1);
				Float priorityCampaign2 = getCampaignPriority(campaign2);
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
						log.info(rname + ": equal! Adding campaign");
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
	
	
	public void trainClassifier() throws Exception { // TODO: train on all instances given, update with the "winning" bid
		if (dataset == null) {
			log.severe("Can't train classifier if dataset wasn't loaded. Make sure to init the ImpressionBidder properly");
		}
				
		classifier.buildClassifier(dataset);
	}
	
	public void updateInstance(String publisher,
			Set<MarketSegment> marketSegment, Device device, AdType adType,
			double campaignsLastPriority, double secondPriceBid) {
		Integer instanceIndex = lastInstancesIndicesMap.get(new InstanceIndexKey(publisher, marketSegment, device, adType, campaignsLastPriority));
		if (instanceIndex == null) {
			log.warning("ERROR - could not find the matching instance for the query <" + publisher + "," + marketSegment + "," + device + "," + adType + ">");
			return;
		}
		dataset.instance(instanceIndex).setClassValue(secondPriceBid);
	}
	
	public void updateClassifier() throws Exception {
		
		// TODO use adNetReports to update the dataset with the real bids we wanted.
		@SuppressWarnings("unchecked")
		Enumeration<Instance> instances = dataset.enumerateInstances();
		while (instances.hasMoreElements()) {
			Instance instance = (Instance) instances.nextElement();
			if (instance.value(IS_UPDATED_INDEX) == NOT_UPDATED) {
				instance.setValue(IS_UPDATED_INDEX, UPDATED);
				// TODO instance.setClassValue(value);
				// TODO update instance
			}
		}
		
		classifier.buildClassifier(dataset);
	}
	
	/* Infrastructure */
	protected ImpressionBidder() {	}

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
	
	public void updatePublisherStats(Map<String, PublisherStats> publisherDailyStats) {
		for (String publisherName : publisherDailyStats.keySet()) {
			publishersStats.put(publisherName, publisherDailyStats.get(publisherName));
		}
	}
	
	public void updateDay(int day) {
		this.dayBiddingFor = day;
	}


}
