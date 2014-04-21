package OrOmerShelly;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;
import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.demand.CampaignStats;
import tau.tac.adx.devices.Device;
import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.props.PublisherCatalogEntry;
import tau.tac.adx.report.adn.AdNetworkReport;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.report.demand.AdNetBidMessage;
import tau.tac.adx.report.demand.AdNetworkDailyNotification;
import tau.tac.adx.report.demand.CampaignOpportunityMessage;
import tau.tac.adx.report.demand.CampaignReport;
import tau.tac.adx.report.demand.CampaignReportKey;
import tau.tac.adx.report.demand.InitialCampaignMessage;
import tau.tac.adx.report.demand.campaign.auction.CampaignAuctionReport;
import tau.tac.adx.report.publisher.AdxPublisherReport;
import tau.tac.adx.report.publisher.AdxPublisherReportEntry;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BankStatus;

/**
 * 
 * @author Mariano Schain
 * Test plug-in
 * 
 */
public class OOSAgent extends Agent {
		
	private final Logger log = Logger
			.getLogger(OOSAgent.class.getName());
	
	private Coordinator coordinator = Coordinator.getInstance();

	/*
	 * Basic simulation information. An agent should receive the {@link
	 * StartInfo} at the beginning of the game or during recovery.
	 */
	@SuppressWarnings("unused")
	private StartInfo startInfo;

	/*
	 * The addresses of server entities to which the agent should send the daily
	 * bids data
	 */
	private String demandAgentAddress;
	private String adxAgentAddress;


	/*
	 * The current bid level for the user classification service
	 */
	double ucsBid;

	/*
	 * The targeted service level for the user classification service
	 */
	double ucsTargetLevel;


	public OOSAgent() {	

	}

	@Override
	protected void messageReceived(Message message) {
		try {
			Transportable content = message.getContent();
			
			//log.fine(message.getContent().getClass().toString());
			
			if (content instanceof InitialCampaignMessage) {
				handleInitialCampaignMessage((InitialCampaignMessage) content); // ownership: Or; who determines the budget?
			} else if (content instanceof CampaignOpportunityMessage) {
				handleICampaignOpportunityMessage((CampaignOpportunityMessage) content); // ownership: Or
			} else if (content instanceof CampaignReport) {
				coordinator.handleCampaignReport((CampaignReport) content); 				// ownership: Shelly (possibly Or)
			} else if (content instanceof AdNetworkDailyNotification) {
				coordinator.handleAdNetworkDailyNotification((AdNetworkDailyNotification) content); // will be used by all of us
			} else if (content instanceof AdxPublisherReport) {
				coordinator.handleAdxPublisherReport((AdxPublisherReport) content); 		// ownership: Shelly
			} else if (content instanceof SimulationStatus) {
				handleSimulationStatus((SimulationStatus) content);				// might be relevant for performance
			} else if (content instanceof PublisherCatalog) {
				coordinator.handlePublisherCatalog((PublisherCatalog) content);				// ownership: Shelly
			} else if (content instanceof AdNetworkReport) {
				coordinator.handleAdNetworkReport((AdNetworkReport) content);				// ownership: Shelly (possibly Or)
			} else if (content instanceof StartInfo) {
				handleStartInfo((StartInfo) content);
			} else if (content instanceof BankStatus) {
				coordinator.handleBankStatus((BankStatus) content);
			} else if (content instanceof CampaignAuctionReport) {
				coordinator.handleCampaignAuctionReport((CampaignAuctionReport) content);
			} else {
				System.out.println("UNKNOWN Message Received: " + content);
			}

		} catch (NullPointerException e) {
			this.log.log(Level.SEVERE,
					"Exception thrown while trying to parse message: " + e + " : " + Arrays.asList(e.getStackTrace()));
			return;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * On day 0, a campaign (the "initial campaign") is allocated to each
	 * competing agent. The campaign starts on day 1. The address of the
	 * server's AdxAgent (to which bid bundles are sent) and DemandAgent (to
	 * which bids regarding campaign opportunities may be sent in subsequent
	 * days) are also reported in the initial campaign message
	 */
	protected void handleInitialCampaignMessage(
			InitialCampaignMessage campaignMessage) {
		demandAgentAddress = campaignMessage.getDemandAgentAddress();
		adxAgentAddress = campaignMessage.getAdxAgentAddress();
		
		coordinator.handleInitialCampaignMessage(campaignMessage);
	}
	
	/**
	 * On day n ( > 0) a campaign opportunity is announced to the competing
	 * agents. The campaign starts on day n + 2 or later and the agents may send
	 * (on day n) related bids (attempting to win the campaign). The allocation
	 * (the winner) is announced to the competing agents during day n + 1.
	 */
	protected void handleICampaignOpportunityMessage(CampaignOpportunityMessage com) {
		double bidForCampaign = coordinator.getBidForCampaign(com);
		int campaignBiddingForId = coordinator.getPendingCampaignId();
		double ucsBid = coordinator.getBidForUcs();
		
		AdNetBidMessage bids = new AdNetBidMessage(ucsBid, campaignBiddingForId,  // here we send a bid for campaign oppor. and ucs combined.
				Math.round(bidForCampaign));
		sendMessageOnBidsAndUcs(bids);
	}
	
	/**
	 * The SimulationStatus message received on day n indicates that the
	 * calculation time is up and the agent is requested to send its bid bundle
	 * to the AdX.
	 */
	public void handleSimulationStatus(SimulationStatus simulationStatus) {
		log.info("Day " + coordinator.getDay() + " : Simulation Status Received");
		AdxBidBundle bidBundle = coordinator.calculateBidBundle();
		log.info("Day " + (coordinator.getDay() - 1) + " ended. Started next day");
		
		if (bidBundle != null) {
			sendMessage(adxAgentAddress, bidBundle);
		}
	}

	
	/**
	 * Processes the start information.
	 * 
	 * @param startInfo
	 *            the start information.
	 */
	protected void handleStartInfo(StartInfo startInfo) {
		this.startInfo = startInfo;
	}

	/**
	 * Send bids on campaigns and UCS. 
	 */
	public void sendMessageOnBidsAndUcs(AdNetBidMessage bids) {
		sendMessage(demandAgentAddress, bids);		
	}

	
	/** Simulation start / finish */
	@Override
	protected void simulationSetup() {
		coordinator.initSimulation();
		log.fine("AdNet " + getName() + " simulationSetup");
	}

	@Override
	protected void simulationFinished() {
		coordinator.finiSimulation();
	}

}
