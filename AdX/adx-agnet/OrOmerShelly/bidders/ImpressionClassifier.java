package OrOmerShelly.bidders;

import java.util.Arrays;
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
import OrOmerShelly.ImpressionParameters;
import OrOmerShelly.PublisherStats;
import OrOmerShelly.userAnalysis.UserAnalyzer;

public class ImpressionClassifier {
	
	private final Logger log = Logger
			.getLogger(ImpressionClassifier.class.getName());

	private static final String INSTANCES_NAME = "IMPRESSION_BIDDER_INSTANCES";
	
	private static final int PUBLISHER_POPULARITY_ATTR_INDEX = 0;
	private static final int DEVICE_ATTR_INDEX = 1;
	private static final int AD_TYPE_ATTR_INDEX = 2;
	private static final int AD_TYPE_ORIENTATION_ATTR_INDEX = 3;
	private static final int WEIGHT_OF_MARKET_SEGMENT_DEVICE_ATTR_INDEX = 4;
	private static final int REMAINING_BUDGET_ATTR_INDEX = 5;
	private static final int PRIORITY_ATTR_INDEX = 6;
	private final static int BID_ATTR_CLASS_VALUE_INDEX = 7;
	
	private final static int CAPACITY_OF_INSTANCES = 10000;
	
	private static final double PC = 1.0;
	private static final double MOBILE = 2.0;
	private static final double TEXT = 1.0;
	private static final double VIDEO = 2.0;
	
	
	// Map publisher, market segment, device, ad type to Instance index
	private HashMap<InstanceIndexKey, Integer> lastInstancesIndicesMap = new HashMap<InstanceIndexKey, Integer>();
	private Instance lastInstance = null; // Doesn't contain campaign information and bid
	private Instances dataset = null; // On the first day this is a example dataset which is based on budget/reach impressions.
	private Classifier classifier;
	private int dayBiddingFor;
	
	private PublisherCatalog publisherCatalog;
	private Map<String, PublisherStats> publishersStats = new HashMap<String, PublisherStats>();
	private UserAnalyzer userAnalyzer;
	
	public ImpressionClassifier(UserAnalyzer userAnalyzer) {
		this.userAnalyzer = userAnalyzer;
	}

	public void init(Classifier newClassifier, CampaignData currentCampaign) throws Exception {
		classifier = newClassifier;
		dataset = getDefaultDataset(currentCampaign);
		trainClassifier();
	}
	
	// Default dataset is not fixed, only concrete instances
	public Instances getDefaultDataset(CampaignData currentCampaign) {
		String rname = "getDefaultDataset";
		
		FastVector attributeNames = new FastVector();
		attributeNames.insertElementAt(new Attribute("popularity"), PUBLISHER_POPULARITY_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("device"), DEVICE_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("adType"), AD_TYPE_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("adTypeOrientation"), AD_TYPE_ORIENTATION_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("weightDeviceMarketSegment"), WEIGHT_OF_MARKET_SEGMENT_DEVICE_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("remainingBudget"), REMAINING_BUDGET_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("priority"), PRIORITY_ATTR_INDEX);
		attributeNames.insertElementAt(new Attribute("bid"), BID_ATTR_CLASS_VALUE_INDEX);
		
		Instances defaultInstances = new Instances(INSTANCES_NAME, 
				attributeNames,
				CAPACITY_OF_INSTANCES); // Test this... Weka3.7 is more intuitive using a list and not a FastVector
		
		
		/* Adding default instances - base bid is like in sample ad network, but want to incorporate market segment matching and weight consideration */
		double budget = currentCampaign.getBudget();
		
		int countInstances = (dataset == null ? 0 : dataset.numInstances()-1);
		// Video / Mobile - are worth more according to mobileCoeff, videoCoeff. So take them into account.
		for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog.getPublishers()) {
			String publisherName = publisherCatalogEntry.getPublisherName();
			log.info(rname + ": Generating default instances for publisher - " + publisherName);

			double median = userAnalyzer.calcMedianOfMarketSegmentsWeights(publisherName);
			double publishersMarketSegmentWeight = userAnalyzer.getMarketSegmentWeight(currentCampaign.getTargetSegment(), publisherName);
			double priority = currentCampaign.getCampaignPriority(dayBiddingFor);
			double avgCmpRevenuePerImp = budget / currentCampaign.getReachImps();
			
			PublisherStats publisherStats = publishersStats.get(publisherName);
			if (publisherStats == null) {
				log.info(rname + ": Getting averaged publisher stats as there is no record for publisher " + publisherName);
				publisherStats = userAnalyzer.getAveragedPublisherStatsForPublisher(publisherName);
			}
						
			double adTypeOrientation = publisherStats.getVideoOrientation() / publisherStats.getTextOrientation();
			
			for (Device device : Device.values()) {
				double publishersDeviceWeight = userAnalyzer.getDeviceWeight(publisherName, device);
				
				for (AdType adType : AdType.values()) {
					double[] attributes = new double[7];
					attributes[0] = deviceSerialize(device);
					attributes[1] = adTypeSerialize(adType);
					attributes[2] = adTypeOrientation; 
					attributes[3] = publishersMarketSegmentWeight * publishersDeviceWeight;
					attributes[4] = budget;
					attributes[5] = 1.0;
					attributes[6] = avgCmpRevenuePerImp
										*priority
										*(publishersMarketSegmentWeight/median)
										*(device == Device.mobile ? currentCampaign.getMobileCoef() : 1.0)
										*(adType == AdType.text ? 1.0 : adTypeOrientation*currentCampaign.getVideoCoef());			// TODO can be < 0 somehow??	
					
					int[] indicesToFill = new int[7];
					indicesToFill[0] = DEVICE_ATTR_INDEX;
					indicesToFill[1] = AD_TYPE_ATTR_INDEX;
					indicesToFill[2] = AD_TYPE_ORIENTATION_ATTR_INDEX;
					indicesToFill[3] = WEIGHT_OF_MARKET_SEGMENT_DEVICE_ATTR_INDEX;
					indicesToFill[4] = REMAINING_BUDGET_ATTR_INDEX;
					indicesToFill[5] = PRIORITY_ATTR_INDEX;
					indicesToFill[6] = BID_ATTR_CLASS_VALUE_INDEX;
					
					log.info(rname + ": Adding instance - " + Arrays.toString(attributes));
					
					Instance defaultInstance = new SparseInstance(1, attributes, indicesToFill, 1); 
					defaultInstances.add(defaultInstance);
					lastInstancesIndicesMap.put(new InstanceIndexKey(publisherName, currentCampaign.getTargetSegment(), device, adType, 1.0), countInstances); // -1 or no -1?
					++countInstances;
				}
			}
		}
		
		// TODO: add unknown market segment case - device, adType known, market segment weight not.
		
		defaultInstances.setClassIndex(BID_ATTR_CLASS_VALUE_INDEX);
		return defaultInstances;
		
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
	
	
	public void generateUnknownInstance(String publisherName, Device device, AdType adType, CampaignData urgentCampaign, float leastPriority) {
		PublisherStats publisherStats = publishersStats.get(publisherName);
		double adTypeOrientation = (publisherStats == null ? 1.0 : publisherStats.getAdTypeOrientation());
		float thisCampaignPriority = urgentCampaign.getCampaignPriority(dayBiddingFor);
		
		double[] attributes = new double[6];
		attributes[0] = deviceSerialize(device);
		attributes[1] = adTypeSerialize(adType);
		attributes[2] = adTypeOrientation;
		attributes[3] = userAnalyzer.getMarketSegmentWeight(urgentCampaign.getTargetSegment(), publisherName);
		attributes[4] = urgentCampaign.getBudget();
		attributes[5] = thisCampaignPriority * thisCampaignPriority/leastPriority; // Increase priority by how much more urgent it is than the least urgent campaign.

		int[] indicesToFill = new int[6];
		indicesToFill[0] = DEVICE_ATTR_INDEX;
		indicesToFill[1] = AD_TYPE_ATTR_INDEX;
		indicesToFill[2] = AD_TYPE_ORIENTATION_ATTR_INDEX;
		indicesToFill[3] = WEIGHT_OF_MARKET_SEGMENT_DEVICE_ATTR_INDEX;
		indicesToFill[4] = REMAINING_BUDGET_ATTR_INDEX;
		indicesToFill[5] = PRIORITY_ATTR_INDEX;
		
		lastInstance = new SparseInstance(1, attributes, indicesToFill, 1);
		dataset.add(lastInstance);
		lastInstancesIndicesMap.put(new InstanceIndexKey(publisherName, urgentCampaign.getTargetSegment(), device, adType, thisCampaignPriority * thisCampaignPriority/leastPriority), dataset.numInstances()-1);
	}
	
	public void generateFirstInstance(ImpressionParameters impParams,
			String publisherName, Double weight) {
		PublisherStats publisherStats = publishersStats.get(publisherName);
		double adTypeOrientation = (publisherStats == null ? 1.0 : publisherStats.getAdTypeOrientation());
		
		double[] attributes = new double[4];
		attributes[0] = deviceSerialize(impParams.getDevice());
		attributes[1] = adTypeSerialize(impParams.getAdType());
		attributes[2] = adTypeOrientation;
		attributes[3] = weight;

		int[] indicesToFill = new int[4];
		indicesToFill[0] = DEVICE_ATTR_INDEX;
		indicesToFill[1] = AD_TYPE_ATTR_INDEX;
		indicesToFill[2] = AD_TYPE_ORIENTATION_ATTR_INDEX;
		indicesToFill[3] = WEIGHT_OF_MARKET_SEGMENT_DEVICE_ATTR_INDEX;
		
		lastInstance = new SparseInstance(1, attributes, indicesToFill, 1);
		dataset.add(lastInstance);
		lastInstancesIndicesMap.put(new InstanceIndexKey(publisherName, impParams.getMarketSegments(), impParams.getDevice(), impParams.getAdType(), -1), dataset.numInstances()-1);
	}

	public Instance enrichInstance(double remainingBudget, float priority) {
		Instance enriched = new SparseInstance(lastInstance); 		
		enriched.setValue(REMAINING_BUDGET_ATTR_INDEX, remainingBudget);
		enriched.setValue(PRIORITY_ATTR_INDEX, priority);
		return enriched;
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
		classifier.buildClassifier(dataset);
	}

	public double classifyLastInstance() throws Exception {
		double classificationResult = classifier.classifyInstance(lastInstance);
		lastInstance.setClassValue(classificationResult);
		return classificationResult;
	}
	
	private double deviceSerialize(Device device) {
		return (device == Device.mobile ? MOBILE : PC);
	}
	
	private double adTypeSerialize(AdType adType) {
		return (adType == AdType.text ? TEXT : VIDEO);
	}

	public Instance getLastInstance() {
		return lastInstance;
	}

	public double classifyEnriched(double budget, float priority) throws Exception {
		log.info("First instance: " + lastInstance.toString() + " Class value = " + lastInstance.classValue());
		Instance campaignInstnace = enrichInstance(budget, // TODO Remaining budget, not whole budget
				priority);
		
		log.info("Enriched instance: " + campaignInstnace.toString());
		dataset.add(campaignInstnace);	
		
		double enrichedClassificationResult = classifier.classifyInstance(campaignInstnace); // TODO why 0?
		campaignInstnace.setClassValue(enrichedClassificationResult);
		log.info("Classified instance: " + campaignInstnace.toString() + " Class value = " + campaignInstnace.classValue());
		
		return enrichedClassificationResult;
	}

	public void setDayBiddingFor(int day) {
		this.dayBiddingFor = day;		
	}

	public void setPublishersStats(Map<String, PublisherStats> publishersStats) {
		this.publishersStats = publishersStats;		
	}

	public void setPublisherCatalog(PublisherCatalog publisherCatalog) {
		this.publisherCatalog = publisherCatalog;
	}
		
	
}
