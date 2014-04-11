package OrOmerShelly;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;
import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.report.adn.AdNetworkReport;
import tau.tac.adx.report.demand.AdNetBidMessage;
import tau.tac.adx.report.demand.AdNetworkDailyNotification;
import tau.tac.adx.report.demand.CampaignOpportunityMessage;
import tau.tac.adx.report.demand.CampaignReport;
import tau.tac.adx.report.demand.InitialCampaignMessage;
import tau.tac.adx.report.demand.campaign.auction.CampaignAuctionReport;
import tau.tac.adx.report.publisher.AdxPublisherReport;
import edu.umich.eecs.tac.props.BankStatus;

/**
 * 
 * @author Mariano Schain
 * 
 */
public class OOSAgent extends Agent {
		
	private final Logger log = Logger
			.getLogger(OOSAgent.class.getName());
	
	private Coordinator coordinator = Coordinator.getInstance(this);

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
	public String demandAgentAddress;
	public String adxAgentAddress;


	public OOSAgent() {	}

	@Override
	protected void messageReceived(Message message) {
		try {
			Transportable content = message.getContent();
			
			//log.fine(message.getContent().getClass().toString());
			
			if (content instanceof InitialCampaignMessage) {
				coordinator.handleInitialCampaignMessage((InitialCampaignMessage) content); // ownership: Or; who determines the budget?
			} else if (content instanceof CampaignOpportunityMessage) {
				coordinator.handleICampaignOpportunityMessage((CampaignOpportunityMessage) content); // ownership: Or
			} else if (content instanceof CampaignReport) {
				coordinator.handleCampaignReport((CampaignReport) content); 				// ownership: Shelly (possibly Or)
			} else if (content instanceof AdNetworkDailyNotification) {
				coordinator.handleAdNetworkDailyNotification((AdNetworkDailyNotification) content); // will be used by all of us
			} else if (content instanceof AdxPublisherReport) {
				coordinator.handleAdxPublisherReport((AdxPublisherReport) content); 		// ownership: Shelly
			} else if (content instanceof SimulationStatus) {
				coordinator.handleSimulationStatus((SimulationStatus) content);				// might be relevant for performance
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
				log.info("UNKNOWN Message Received: " + content);
			}

		} catch (NullPointerException e) {
			this.log.log(Level.SEVERE,
					"Exception thrown while trying to parse message: " + e + " : " + Arrays.asList(e.getStackTrace()));
			return;
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
	 * Send BidBundle message
	 */
	public void sendBidAndAds(AdxBidBundle bidBundle) {
		if (bidBundle != null) {
			sendMessage(adxAgentAddress, bidBundle);
		}
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
