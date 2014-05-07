package OrOmerShelly.bidders;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.devices.Device;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.props.PublisherCatalogEntry;
import tau.tac.adx.report.adn.MarketSegment;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import OrOmerShelly.CampaignData;
import OrOmerShelly.PublisherStats;
import OrOmerShelly.userAnalysis.UserAnalyzer;

public class ImpressionClassifier {
	
	private static boolean MAIN_DEBUG = true;
	private static boolean DEBUG = false;
	
	private final Logger log = Logger.getLogger(ImpressionClassifier.class.getName());

	private static final String INSTANCES_NAME = "IMPRESSION_BIDDER_INSTANCES";
	
	private static final int WEIGHT_OF_MARKET_SEGMENT_DEVICE_ATTR_INDEX = 0;
	private static final int PRIORITY_ATTR_INDEX = 1;
	private final static int BID_ATTR_CLASS_VALUE_INDEX = 2;
	
	private final static int CAPACITY_OF_INSTANCES = 10000;
	
	private final double BID_INCREASE_FACTOR = 2000.0; // > 0 as we discovered very slow start. If too high, will be fixed by the learning from the AdnetReport.
	private final double INITIAL_CAMPAIGN_FACTOR = 1550.0;
	
	// Map publisher, market segment, device, ad type and campaign to Instance index
	private HashMap<InstanceIndexKey, Integer> lastInstancesIndicesMap = new HashMap<InstanceIndexKey, Integer>();
	
	private Instance lastInstance = null; // last instance created.
	private Instances dataset = null; // Classifier's dataset, updated each day with new instances, and upon AdNetReport is changed.
	private Classifier classifier; // Concrete classifier implementation.
	private int dayBiddingFor;
	
	private PublisherCatalog publisherCatalog;
	private Map<String, PublisherStats> publishersStats = new HashMap<String, PublisherStats>();
	private UserAnalyzer userAnalyzer;

	/**
	 * Called on the 1st day with the initial campaign, initialised classifier for bidding on that campaign.
	 * @param newClassifier
	 * @param currentCampaign
	 * @param campaignRelativePriority
	 * @throws Exception
	 */
	public void init(Classifier newClassifier, CampaignData currentCampaign,  double campaignRelativePriority) throws Exception {
		classifier = newClassifier;
		dataset = getDefaultDataset(currentCampaign, campaignRelativePriority, true);
		trainClassifier();
	}
	
	/**
	 * Generates a default dataset with instances matching the current campaign and its current relative priority
	 * If called for the initial campaign, will double the BID_INCREASE_FACTOR
	 * @param currentCampaign
	 * @param campaignRelativePriority
	 * @param isInitial
	 * @return
	 */
	public Instances getDefaultDataset(CampaignData currentCampaign, double campaignRelativePriority, boolean isInitial) {
		String rname = "getDefaultDataset";
		
		/* Init attributes */
		FastVector attributeNames = new FastVector();
		attributeNames.insertElementAt(new Attribute("weightDeviceMarketSegment"), WEIGHT_OF_MARKET_SEGMENT_DEVICE_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("priority"), PRIORITY_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("bid"), BID_ATTR_CLASS_VALUE_INDEX);
		
		Instances defaultInstances = new Instances(INSTANCES_NAME, attributeNames, CAPACITY_OF_INSTANCES);
		
		double budget = currentCampaign.getBudget();
				
		/* New instances are mapped, so need to keep an ID of the instances.
		 * In Weka development release, there's a more natural way of counting the instances in the dataset.
		 * However, we use the stable 3.6 version.
		 */
		int countInstances = (dataset == null ? 0 : dataset.numInstances()-1);
		
		// Video / Mobile - are worth more according to mobileCoeff, videoCoeff. So take them into account.
		for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog.getPublishers()) {
			String publisherName = publisherCatalogEntry.getPublisherName();
			if (DEBUG) log.info(rname + ": Generating default instances for publisher - " + publisherName);

			// We will compare the target segment weight in the publisher to the median of all market segments weights.
			double median = userAnalyzer.calcMedianOfMarketSegmentsWeights(publisherName);
			double publishersMarketSegmentWeight = userAnalyzer.getMarketSegmentWeight(currentCampaign.getTargetSegment(), publisherName);
			
			// A campaign with a 3 target segments is harder to satisfy than a campaign with a single target segment.
			double targetSegmentSizeFactor = 1.0;
			switch (currentCampaign.getTargetSegment().size()) {
				case 1: targetSegmentSizeFactor = 1.0; break;
				case 2: targetSegmentSizeFactor = 2.0; break;
				case 3: targetSegmentSizeFactor = 6.0; break;
				default: targetSegmentSizeFactor = 1.0; break;
			}
			
			// Taken from the SampleAdNetwork and serves as a basis for the bid here as well.
			double avgCmpRevenuePerImp = budget / currentCampaign.getReachImps();
			
			// We do not necessarily have stats for all the publishers. If we still don't have these, we average on previous data.
			PublisherStats publisherStats = publishersStats.get(publisherName);
			if (publisherStats == null) {
				if (DEBUG) log.info(rname + ": Getting averaged publisher stats as there is no record for publisher " + publisherName);
				publisherStats = userAnalyzer.getAveragedPublisherStatsForPublisher(publisherName);
			}
						
			// Video ads worth more than text ads, so if we get a video ad, we multiply by the video coefficient.
			double adTypeOrientation = publisherStats.getVideoOrientation() / publisherStats.getTextOrientation();
			
			for (Device device : Device.values()) {
				// The weight of the Device in the publisher.
				double publishersDeviceWeight = userAnalyzer.getDeviceWeight(publisherName, device);
				
				for (AdType adType : AdType.values()) {
					double[] attributes = new double[3];
					attributes[0] = publishersMarketSegmentWeight * publishersDeviceWeight;
					attributes[1] = campaignRelativePriority;
					attributes[2] = Math.max(0, avgCmpRevenuePerImp
										*(1+campaignRelativePriority)
										*(publishersMarketSegmentWeight/median) * targetSegmentSizeFactor
										*(device == Device.mobile ? currentCampaign.getMobileCoef() : 1.0)
										*(adType == AdType.text ? 1.0 : adTypeOrientation*currentCampaign.getVideoCoef()))
										* BID_INCREASE_FACTOR
										* (isInitial ? INITIAL_CAMPAIGN_FACTOR : 1);	
					
					int[] indicesToFill = new int[3];
					indicesToFill[0] = WEIGHT_OF_MARKET_SEGMENT_DEVICE_ATTR_INDEX;
					indicesToFill[1] = PRIORITY_ATTR_INDEX;
					indicesToFill[2] = BID_ATTR_CLASS_VALUE_INDEX;
					
					Instance defaultInstance = new SparseInstance(1, attributes, indicesToFill, 1); 
					defaultInstances.add(defaultInstance);
					
					if (MAIN_DEBUG) log.info(rname + ": Adding instance - " + defaultInstance + " with class value = " + attributes[2] + "; numbered " + countInstances);
					
					// Keeping new instance in the map
					lastInstancesIndicesMap.put(new InstanceIndexKey(publisherName, currentCampaign.getTargetSegment(), device, adType, currentCampaign.getId()), countInstances);
					++countInstances;
				}
			}
		}
				
		defaultInstances.setClassIndex(BID_ATTR_CLASS_VALUE_INDEX);
		return defaultInstances;
	}
	
	/**
	 * When a new campaign is won, we generate its initial instances, with classifications, and train the classifier.
	 * @param currentCampaign
	 * @param campaignRelativePriority
	 * @throws Exception
	 */
	public void updateForNewCampaign(CampaignData currentCampaign, double campaignRelativePriority) throws Exception {
		if (MAIN_DEBUG) log.info("Updating classifier for new campaign: " + currentCampaign);
		Instances newInstances = getDefaultDataset(currentCampaign, campaignRelativePriority, false);
		@SuppressWarnings("unchecked")
		Enumeration<Instance> enumerationNewInstances = (Enumeration<Instance>) newInstances.enumerateInstances();
		
		while (enumerationNewInstances.hasMoreElements()) {
			dataset.add(enumerationNewInstances.nextElement());
		}
				
		trainClassifier();
	}
	
	/**
	 * Generate an instance of unknown market segment for classification.
	 * @param publisherName
	 * @param device
	 * @param adType
	 * @param urgentCampaign
	 * @param leastPriority
	 */
	public void generateUnknownInstance(String publisherName, Device device, AdType adType, CampaignData urgentCampaign, float leastPriority) {
		float thisCampaignPriority = urgentCampaign.getCampaignPriority(dayBiddingFor);
		double publishersDeviceWeight = userAnalyzer.getDeviceWeight(publisherName, device);
		
		double[] attributes = new double[2];
		attributes[0] = userAnalyzer.getMarketSegmentWeight(urgentCampaign.getTargetSegment(), publisherName) * publishersDeviceWeight;
		attributes[1] = thisCampaignPriority * thisCampaignPriority/leastPriority; // Increase priority by how much more urgent it is than the least urgent campaign.

		int[] indicesToFill = new int[2];
		indicesToFill[0] = WEIGHT_OF_MARKET_SEGMENT_DEVICE_ATTR_INDEX;
		indicesToFill[1] = PRIORITY_ATTR_INDEX;
		
		lastInstance = new SparseInstance(1, attributes, indicesToFill, 1);
		dataset.add(lastInstance);
		
		// Keep this also in the instances map - so if we hit correctly, we will update the next instances matching the target market segment.
		lastInstancesIndicesMap.put(new InstanceIndexKey(publisherName, urgentCampaign.getTargetSegment(), device, adType, urgentCampaign.getId()), dataset.numInstances()-1);
	}
	
	/**
	 * Generates an instance based only on the impression parameters' weight.
	 * @param weight
	 */
	public void generateFirstInstance(Double weight) {
		double[] attributes = new double[1];
		attributes[0] = weight;

		int[] indicesToFill = new int[1];
		indicesToFill[0] = WEIGHT_OF_MARKET_SEGMENT_DEVICE_ATTR_INDEX;
		
		lastInstance = new SparseInstance(1, attributes, indicesToFill, 1);
		dataset.add(lastInstance);
	}

	/**
	 * Return an instance containing the priority of the campaign as well.
	 * @param remainingBudget
	 * @param priority
	 * @return "enriched" instance, including an updated/new value of the priority attribute.
	 */
	public Instance enrichInstance(float priority) { 
		Instance enriched = new SparseInstance(lastInstance);
		enriched.setValue(PRIORITY_ATTR_INDEX, priority);
		return enriched;
	}
	
	/**
	 * Build the concrete classifier using the current dataset.
	 * @throws Exception
	 */
	private void trainClassifier() throws Exception {
		if (dataset == null) {
			log.severe("Can't train classifier if dataset wasn't loaded. Make sure to init the ImpressionBidder properly");
		}
				
		classifier.buildClassifier(dataset);
	}
	
	/**
	 * Updates the relevant instance (using the key whose elements are provided) with the corrected bid.
	 * @param publisher
	 * @param marketSegment
	 * @param device
	 * @param adType
	 * @param campaignId
	 * @param correctedBid
	 */
	public void updateInstance(String publisher, Set<MarketSegment> marketSegment, Device device, AdType adType, int campaignId, double correctedBid) {
		Integer instanceIndex = lastInstancesIndicesMap.get(new InstanceIndexKey(publisher, marketSegment, device, adType, campaignId));
		if (instanceIndex == null) {
			log.warning("ERROR - could not find the matching instance for the query <" + publisher + "," + marketSegment + "," + device + "," + adType + "," + campaignId + ">");
			return;
		}
		
		dataset.instance(instanceIndex).setClassValue(correctedBid);
		if (DEBUG) log.info("SUCCESS - found the matching instance for the query <" + publisher + "," + marketSegment + "," + device + "," + adType + "," + campaignId + ">");
	}
	
	/**
	 * Public wrapper of trainClassifier.
	 * @throws Exception
	 */
	public void updateClassifier() throws Exception {		
		trainClassifier();
	}
		
	/**
	 * Classifies the lastInstance member.
	 * @return The classification result of lastInstance.
	 * @throws Exception
	 */
	public double classifyLastInstance() throws Exception {
		double classificationResult = classifier.classifyInstance(lastInstance);
		lastInstance.setDataset(dataset);
		lastInstance.setClassValue(classificationResult);
		if (DEBUG) log.info("Classified: " + lastInstance);
		return classificationResult;
	}

	/**
	 * Creates and classifies an enriched instance based on the lastInstance member.
	 * @param priority
	 * @param publisherName
	 * @param marketSegment
	 * @param device
	 * @param adType
	 * @param campaignId
	 * @return The classification result of the enriched instance.
	 * @throws Exception
	 */
	public double classifyEnriched(float priority, String publisherName, Set<MarketSegment> marketSegment, Device device, AdType adType, int campaignId) throws Exception {
		if (DEBUG) log.info("First instance: " + lastInstance.toString() + "; Class value = " + lastInstance.classValue());
		Instance campaignInstance = enrichInstance(priority);
		
		dataset.add(campaignInstance);
		lastInstancesIndicesMap.put(new InstanceIndexKey(publisherName, marketSegment, device, adType, campaignId), dataset.numInstances()-1);
		
		double enrichedClassificationResult = classifier.classifyInstance(campaignInstance);
		campaignInstance.setDataset(dataset);
		campaignInstance.setClassValue(enrichedClassificationResult);
		if (DEBUG) log.info("Classified enriched instance: " + campaignInstance.toString() + "; Class value = " + campaignInstance.classValue());
		
		return enrichedClassificationResult;
	}
	
	/* Infrastructure */

	/* Constructor */
	public ImpressionClassifier(UserAnalyzer userAnalyzer) {
		this.userAnalyzer = userAnalyzer;
	}
	
	/* For debug prints */
	public String printLastInstancesMap() {
		return lastInstancesIndicesMap.toString();
	}
	
	/* Getters / Setters */
	public void setDayBiddingFor(int day) {
		this.dayBiddingFor = day;		
	}

	public void setPublishersStats(Map<String, PublisherStats> publishersStats) {
		this.publishersStats = publishersStats;		
		this.userAnalyzer.setPublisherStats(publishersStats);
	}

	public void setPublisherCatalog(PublisherCatalog publisherCatalog) {
		this.publisherCatalog = publisherCatalog;
	}

	public void setDatasetAsNull() {
		dataset = null;
	}
}
