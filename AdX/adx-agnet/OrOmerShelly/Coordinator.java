package OrOmerShelly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import OrOmerShelly.bidders.CampaignBidder;
import OrOmerShelly.bidders.ImpressionBidder;
import OrOmerShelly.bidders.UCSbidder;

import edu.umich.eecs.tac.props.BankStatus;

import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.demand.CampaignStats;
import tau.tac.adx.devices.Device;
import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.props.PublisherCatalogEntry;
import tau.tac.adx.report.adn.AdNetworkReport;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.report.demand.AdNetworkDailyNotification;
import tau.tac.adx.report.demand.CampaignOpportunityMessage;
import tau.tac.adx.report.demand.CampaignReport;
import tau.tac.adx.report.demand.CampaignReportKey;
import tau.tac.adx.report.demand.InitialCampaignMessage;
import tau.tac.adx.report.demand.campaign.auction.CampaignAuctionReport;
import tau.tac.adx.report.publisher.AdxPublisherReport;
import tau.tac.adx.report.publisher.AdxPublisherReportEntry;
import weka.classifiers.functions.LinearRegression;

public class Coordinator {
	private final Logger log = Logger
			.getLogger(Coordinator.class.getName());

	private static Coordinator instance = null;

	public ImpressionBidder impressionBidder = ImpressionBidder.getInstance();
	public CampaignBidder campaignBidder = CampaignBidder.getInstance();
	public UCSbidder ucsbidder = UCSbidder.getInstance();

	/**
	 * Messages received:
	 * 
	 * We keep all the {@link CampaignReport campaign reports} 
	 * delivered to the agent. We also keep the initialization 
	 * messages {@link PublisherCatalog} and
	 * {@link InitialCampaignMessage} and the most recent messages and reports
	 * {@link CampaignOpportunityMessage}, {@link CampaignReport}, and
	 * {@link AdNetworkDailyNotification}.
	 */
	private final Queue<CampaignReport> campaignReports = new LinkedList<CampaignReport>();
	private PublisherCatalog publisherCatalog = null;
	private InitialCampaignMessage initialCampaignMessage = null;
	private AdNetworkDailyNotification adNetworkDailyNotification = null;
	private final int FIRST_DAY_OF_BID_BUNDLE_CALCULATION = 0;
	private final int FIRST_DAY_OF_PUBLISHERS_REPORT = 1;
	/*
	 * we maintain a list of queries - each characterized by the web site (the
	 * publisher), the device type, the ad type, and the user market segment
	 */
	private AdxQuery[] queries;

	/**
	 * Information regarding the latest campaign opportunity announced
	 */
	private CampaignData pendingCampaign;

	/**
	 * We maintain a collection (mapped by the campaign id) of the campaigns won
	 * by our agent.
	 */
	private Map<Integer, CampaignData> myCampaigns = new HashMap<Integer, CampaignData>(); // TODO: also campaigns that already ended? YES!!!
	// TODO: add a flag to campaigns - active or not

	/*
	 * the bidBundle to be sent daily to the AdX
	 */
	private AdxBidBundle bidBundle;

	/*
	 * The current bid level for the user classification service
	 */
	double ucsBid;

	/*
	 * The targeted service level for the user classification service
	 */
	double ucsTargetLevel;

	double currentUcsLevel;

	double qualityScore;

	Map<String, PublisherStats> publisherDailyStats = new HashMap<String, PublisherStats>();

	/*
	 * current day of simulation
	 */
	public int day;

	public double bankBalance;

	private Random randomGenerator;

	/**
	 * On day 0, a campaign (the "initial campaign") is allocated to each
	 * competing agent. The campaign starts on day 1. The address of the
	 * server's AdxAgent (to which bid bundles are sent) and DemandAgent (to
	 * which bids regarding campaign opportunities may be sent in subsequent
	 * days) are also reported in the initial campaign message
	 */
	public void handleInitialCampaignMessage(InitialCampaignMessage campaignMessage) {
		
		log.info(campaignMessage.toString());
		day = 0;
		initialCampaignMessage = campaignMessage;

		CampaignData campaignData = new CampaignData(initialCampaignMessage);
		campaignData.setBudget(initialCampaignMessage.getReachImps() / 1000.0); // TODO: Shelly - rethink on this calculation

		/*
		 * The initial campaign is already allocated to our agent so we add it
		 * to our allocated-campaigns list.
		 */
		log.info("Day " + day + ": Allocated campaign - " + campaignData);
		campaignData.setCampaignQueries(getRelevantQueriesForCampaign(campaignData));
		getMyCampaigns().put(initialCampaignMessage.getId(), campaignData);
	}


	/**
	 * Process the reported set of publishers
	 * 
	 * @param publisherCatalog
	 */
	public void handlePublisherCatalog(PublisherCatalog publisherCatalog) {
		this.publisherCatalog = publisherCatalog;
		log.info("Got publisherCatalog: " + printPublisherCatalog(publisherCatalog));
		generateAdxQuerySpace();
	}

	private String printPublisherCatalog(PublisherCatalog publisherCatalog) {
		StringBuilder sb = new StringBuilder();
		for (PublisherCatalogEntry entry : publisherCatalog) {
			sb.append(entry.getPublisherName());
			sb.append(" ; ");
		}

		return sb.toString();
	}


	/**
	 * A user visit to a publisher's web-site results in an impression
	 * opportunity (a query) that is characterized by the the publisher, the
	 * market segment the user may belongs to, the device used (mobile or
	 * desktop) and the ad type (text or video).
	 * 
	 * An array of all possible queries is generated here, based on the
	 * publisher names reported at game initialization in the publishers catalog
	 * message
	 */
	private void generateAdxQuerySpace() {
		if (publisherCatalog != null && queries == null) {
			Set<AdxQuery> querySet = new HashSet<AdxQuery>();

			/*
			 * for each web site (publisher) we generate all possible variations
			 * of device type, ad type, and user market segment
			 */
			for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog) {
				String publishersName = publisherCatalogEntry
						.getPublisherName();
				for (MarketSegment userSegment : MarketSegment.values()) {
					Set<MarketSegment> singleMarketSegment = new HashSet<MarketSegment>();
					singleMarketSegment.add(userSegment);

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.video));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.video));

				}

				/**
				 * An empty segments set is used to indicate the "UNKNOWN" segment
				 * such queries are matched when the UCS fails to recover the user's
				 * segments.
				 */
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.text));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.text));
			}
			queries = new AdxQuery[querySet.size()];
			querySet.toArray(queries);

			log.info("Available queries: " + queries);
		}
	}


	/**
	 * On day n ( > 0) a campaign opportunity is announced to the competing
	 * agents. The campaign starts on day n + 2 or later and the agents may send
	 * (on day n) related bids (attempting to win the campaign). The allocation
	 * (the winner) is announced to the competing agents during day n + 1.
	 */
	public double getBidForCampaign(CampaignOpportunityMessage com) {
		day = com.getDay();

		pendingCampaign = new CampaignData(com);
		log.info("Day " + day + ": Campaign opportunity - " + pendingCampaign);

		/*
		 * The campaign requires com.getReachImps() impressions. The competing
		 * Ad Networks bid for the total campaign Budget (that is, the ad
		 * network that offers the lowest budget gets the campaign allocated).
		 * The advertiser is willing to pay the AdNetwork at most 1$ CPM,
		 * therefore the total number of impressions may be treated as a reserve
		 * (upper bound) price for the auction.
		 */
		//		long cmpBid = 1 + Math.abs((randomGenerator.nextLong())
		//				% (com.getReachImps()));
		//
		//		double cmpBidUnits = cmpBid / 1000.0;

		double cmpBidUnits = 1000 * campaignBidder.getBid(pendingCampaign, qualityScore);  // TODO: Or; here we determine the bid.

		log.info("Day " + day + ": Campaign total budget bid: " + cmpBidUnits);
		
		return cmpBidUnits;
	}
	

	public double getBidForUcs() {
		/*
		 * Adjust ucs bid s.t. target level is achieved. Note: The bid for the
		 * user classification service is piggybacked
		 */

		if (adNetworkDailyNotification != null) {
			double ucsLevel = adNetworkDailyNotification.getServiceLevel();
			double prevUcsBid = ucsBid;

			/* UCS Bid should not exceed 0.2 */
			ucsBid = ucsbidder.getBid(this);

			log.info("Day " + day + ": Adjusting ucs bid: was " + prevUcsBid
					+ " level reported: " + ucsLevel + " target: "
					+ ucsTargetLevel + " adjusted: " + ucsBid);
		} else {
			log.info("Day " + day + ": Initial ucs bid is " + ucsBid);
		}
		
		return ucsBid;
	}

	/** Updates the bank balance **/
	public void handleBankStatus(BankStatus content) {
		log.info("Day " + day + " :" + content.toString());
		bankBalance = content.getAccountBalance();
	}


	/**
	 * On day n ( > 0), the result of the UserClassificationService and Campaign
	 * auctions (for which the competing agents sent bids during day n -1) are
	 * reported. The reported Campaign starts in day n+1 or later and the user
	 * classification service level is applicable starting from day n+1.
	 */
	public void handleAdNetworkDailyNotification(
			AdNetworkDailyNotification notificationMessage) {

//		if (day > FIRST_DAY_OF_AD_NETWORK_DAILY_NOTIFICATION) {
//			adNetworkDailyNotificationsHistory.add(adNetworkDailyNotification); // Add last notification to history
//		}

		adNetworkDailyNotification = notificationMessage;

		campaignBidder.updateCampaignes(notificationMessage.getCampaignId(),
				notificationMessage.getWinner(),
				notificationMessage.getPrice()
				); // [orsa:] collect data for ML

		log.info("Day " + day + ": Daily notification for campaign "
				+ adNetworkDailyNotification.getCampaignId());

		String campaignAllocatedTo = " allocated to "
				+ notificationMessage.getWinner();

		if ((pendingCampaign.getId() == adNetworkDailyNotification.getCampaignId())
				&& (notificationMessage.getCost() != 0)) {

			/* add campaign to list of won campaigns */
			pendingCampaign.setBudget(notificationMessage.getCost());
			pendingCampaign.setCampaignQueries(getRelevantQueriesForCampaign(pendingCampaign));

			getMyCampaigns().put(pendingCampaign.getId(), pendingCampaign);

			campaignAllocatedTo = " WON at cost "
					+ notificationMessage.getCost();

			campaignBidder.updateWonPendingCampaign();
		}

		qualityScore = notificationMessage.getQualityScore();

		currentUcsLevel = notificationMessage.getServiceLevel();

		log.info("Day " + day + ": " + campaignAllocatedTo
				+ ". UCS Level set to " + notificationMessage.getServiceLevel()
				+ " at price " + notificationMessage.getPrice()
				+ " Quality Score is: " + notificationMessage.getQualityScore());
	}


	private AdxQuery[] getRelevantQueriesForCampaign(CampaignData campaign) {
		if (publisherCatalog == null) {
			log.severe("Can't get relevant queries for campaign");
		}

		Set<AdxQuery> querySet = new HashSet<AdxQuery>();

		for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog) {
			String publishersName = publisherCatalogEntry.getPublisherName();

			for (MarketSegment userSegment : MarketSegment.values()) {
				if (campaign.getTargetSegment().contains(userSegment)) {
					Set<MarketSegment> singleMarketSegment = new HashSet<MarketSegment>();
					singleMarketSegment.add(userSegment);
	
					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.text));
	
					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.text));
	
					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.video));
	
					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.video));
				}

			}

			/**
			 * An empty segments set is used to indicate the "UNKNOWN" segment
			 * such queries are matched when the UCS fails to recover the user's
			 * segments.
			 */
			querySet.add(new AdxQuery(publishersName,
					new HashSet<MarketSegment>(), Device.mobile,
					AdType.video));
			querySet.add(new AdxQuery(publishersName,
					new HashSet<MarketSegment>(), Device.mobile,
					AdType.text));
			querySet.add(new AdxQuery(publishersName,
					new HashSet<MarketSegment>(), Device.pc, AdType.video));
			querySet.add(new AdxQuery(publishersName,
					new HashSet<MarketSegment>(), Device.pc, AdType.text));
		}

		return querySet.toArray(new AdxQuery[querySet.size()]);

	}

	public AdxBidBundle calculateBidBundle() {
		boolean hadExceptionInClassifier = false;

		if (day == FIRST_DAY_OF_BID_BUNDLE_CALCULATION) {
			try {
				impressionBidder.setPublisherCatalog(publisherCatalog);
				impressionBidder.init(new LinearRegression(), myCampaigns.entrySet().iterator().next().getValue(), day + 1);
				log.info("Initialized ImpressionBidder");
			} catch (Exception e) {
				hadExceptionInClassifier = true;
				log.severe("Exception in initializing classifier: " + e + " - " + Arrays.asList(e.getStackTrace()));
			}
		}

		impressionBidder.updateDay(day + 1);
		impressionBidder.setMyActiveCampaigns(getMyActiveCampaigns(day + 1)); // relevantDay is day of bid which is day+1

		if (day != FIRST_DAY_OF_BID_BUNDLE_CALCULATION) {
			impressionBidder.setPreviousBidBundle(bidBundle);
		}

		if (day >= FIRST_DAY_OF_PUBLISHERS_REPORT) {
			impressionBidder.updatePublisherStats(publisherDailyStats);		
		} else {
			impressionBidder.updatePublisherStats(new HashMap<String, PublisherStats>());
		}

		try {
			if (!hadExceptionInClassifier) {
				impressionBidder.fillBidBundle();
				bidBundle = impressionBidder.getBidBundle();
			}
		} catch (Exception e) {
			hadExceptionInClassifier = true;
			log.severe("Exception in running classifier: " + e + " - " + Arrays.asList(e.getStackTrace()));
		}

		if (!hadExceptionInClassifier) {
			log.info("Day " + day + ": Sending BidBundle");
		} else {
			log.warning("BidBundle is null! Getting default BidBundle");
			bidBundle = impressionBidder.getDefaultBidBundle(queries);
		}

		
		++day;
		
		return bidBundle;
	}


	private List<CampaignData> getMyActiveCampaigns(int relevantDay) {
		List<CampaignData> activeCampaigns = new ArrayList<CampaignData>();

		log.info("Fetching active campaigns from: " + myCampaigns.values());
		for (CampaignData campaign : myCampaigns.values()) {
			if (campaign.isActive(relevantDay)) { 
				log.info("campaign " + campaign + " is active");
				activeCampaigns.add(campaign);
			}
		}

		return activeCampaigns;
	}


	/**
	 * Campaigns performance w.r.t. each allocated campaign
	 */
	public void handleCampaignReport(CampaignReport campaignReport) {

		campaignReports.add(campaignReport);

		/*
		 * for each campaign, the accumulated statistics from day 1 up to day
		 * n-1 are reported
		 */
		for (CampaignReportKey campaignKey : campaignReport.keys()) {
			int cmpId = campaignKey.getCampaignId();
			CampaignStats cstats = campaignReport.getCampaignReportEntry(
					campaignKey).getCampaignStats();
			getMyCampaigns().get(cmpId).setStats(cstats);

			log.info("Day " + day + ": Updating campaign " + cmpId + " stats: "
					+ cstats.getTargetedImps() + " tgtImps "
					+ cstats.getOtherImps() + " nonTgtImps. Cost of imps is "
					+ cstats.getCost());
		}
	}

	public void handleCampaignAuctionReport(CampaignAuctionReport campaignAuctionReport) {
		log.info("Campaign auction report for day " + day + ": " + campaignAuctionReport.toMyString());
	}

	/**
	 * Users and Publishers statistics: popularity and ad type orientation
	 */
	public void handleAdxPublisherReport(AdxPublisherReport adxPublisherReport) {

		log.info("Publishers Report: ");
		for (PublisherCatalogEntry publisherKey : adxPublisherReport.keys()) {
			AdxPublisherReportEntry entry = adxPublisherReport
					.getEntry(publisherKey);
			log.info(entry.toString());
			PublisherStats publisherStats = fetchPublisherStats(entry);
			if (publisherDailyStats.containsKey(publisherKey.getPublisherName())) {
				log.info("Updating statistics for: " + publisherKey.getPublisherName());
			}

			publisherDailyStats.put(publisherKey.getPublisherName(), publisherStats);
		}
	}

	private PublisherStats fetchPublisherStats(AdxPublisherReportEntry entry) {
		return new PublisherStats(entry.getPopularity(),
				entry.getAdTypeOrientation().get(AdType.video),
				entry.getAdTypeOrientation().get(AdType.text));
	}


	/**
	 * 
	 * @param AdNetworkReport
	 * @throws Exception 
	 */
	public void handleAdNetworkReport(AdNetworkReport adnetReport) throws Exception {
		ucsbidder.updateUCS(adnetReport, this);
		log.info("Day "+ day + " : AdNetworkReport = " + adnetReport);
		
		impressionBidder.updateByAdNetReport(adnetReport);
	}



	public void initSimulation() {
		randomGenerator = new Random();
		day = 0;
		bidBundle = new AdxBidBundle();
		ucsTargetLevel = 0.5 + (randomGenerator.nextInt(5) + 1) / 10.0;

		/* initial bid between 0.1 and 0.2 */
		ucsBid = 0.1 + 0.1*randomGenerator.nextDouble(); //TODO: Omer; check this out - the ucs bit for the first day.

		setMyCampaigns(new HashMap<Integer, CampaignData>());
	}

	public void finiSimulation() {
		campaignReports.clear();
		bidBundle = null;
	}


	/* Infrastructure */	
	public Coordinator() { }

	public static Coordinator getInstance() {
		if (instance == null) {
			instance = new Coordinator();
		}

		return instance;
	}


	public Map<Integer, CampaignData> getMyCampaigns() {
		return myCampaigns;
	}

	public void setMyCampaigns(Map<Integer, CampaignData> myCampaigns) {
		this.myCampaigns = myCampaigns;
	}

	public int getPendingCampaignId() {
		return pendingCampaign.getId();
	}

	public int getDay() {
		return day;
	}


}
