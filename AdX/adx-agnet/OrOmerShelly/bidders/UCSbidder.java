package OrOmerShelly.bidders;

import java.util.Map;
import java.util.Set;

import tau.tac.adx.report.adn.AdNetworkKey;
import tau.tac.adx.report.adn.AdNetworkReport;
import tau.tac.adx.report.adn.MarketSegment;
import OrOmerShelly.CampaignData;
import OrOmerShelly.Coordinator;

public class UCSbidder {
	/*
	*/
	double amountPaidYesterday;
	final int stateA = 0; // the machine's states
	final int stateB = 1;
	final int stateC = 2;
	final int stateD = 3;
	final int stateE = 4;
	final int stateF = 5;
	final int stateG = 6;
	final int stateH = 7;
	final int stateI = 8;
	final int stateJ = 9;
	final int stateK = 10;
	final int statesCount=11;
	final int possibleActions=5;
	final double range0=1; // range for the bid
	final double range1=2;
	final double range2=3;
	final double range3=4;
	final double finalRange0=1; // range for the reinforcement
	final double finalRange1=2;
	final double finalRange2=3;
	final double finalRange3=4;
	final double[] prices={0.1,0.125,0.15,0.175,0.2}; //possible prices
	double alpha=0.1; //Q-learning's parameters
	double gamma=0.9;
	int state;
	int nextstate;
	int action;
	int[] actionsFromA = new int[] { stateB, stateC, stateD, stateE, stateF };
	int[] actionsFromB = new int[] { stateG, stateH, stateI, stateJ, stateK };
	int[] actionsFromC = new int[] { stateG, stateH, stateI, stateJ, stateK };
	int[] actionsFromD = new int[] { stateG, stateH, stateI, stateJ, stateK };
	int[] actionsFromE = new int[] { stateG, stateH, stateI, stateJ, stateK };
	int[] actionsFromF = new int[] { stateG, stateH, stateI, stateJ, stateK };
	int[][] actions = new int[][] { actionsFromA, actionsFromB, actionsFromC,
			actionsFromD, actionsFromE, actionsFromF };
	double[][] Q = new double[statesCount][possibleActions]; // a matrix that holds the Q-values, which are used in the Q-learning algorithm
	private static UCSbidder instance = null;

	protected UCSbidder() {
		// Exists only to defeat instantiation.
	}

	public static UCSbidder getInstance() { // this function initializes the UCSbidder (called once from the coordinator)

		if(instance == null) {
			instance = new UCSbidder();
		}

		return instance;
	}

	public double getBid(Coordinator co){ // getting a bid for the UCS based on our current state and previous rewards
		int state=findState(co); // classify our state based solely on the active campaigns we still have
		if (state==stateB) {
			this.action=0;
			this.state=stateB;
			this.amountPaidYesterday=0;
			return 0;
		}
		int action=(int) Math.round((maxQ(state))[1]); // use Q-learning to find out the best possible action (price)
		//if (this.action==0) this.action=1;
		this.action=action;
		this.state=state;
		this.amountPaidYesterday=prices[action];
		return prices[action]; // return the price, based on the Q-learning action (out of a possible 5 prices)
	}


	double Q(int s, int a) {
		/*
		 * Get the Q value for a state s and an action a
		 */
		return this.Q[s][a];
	}
	public void updateUCS(AdNetworkReport anp, Coordinator co) {
		/*
		 * update the UCS based on its performance yesterday.
		 * 
		 */
		if (state==stateB) return;
		double reinforecement=findReinforcement(anp, co, co.day);

		nextstate=findFinalState(reinforecement);

		double q = Q(state, action);
		double maxQ = maxQ(state)[0];
		double value = q + alpha * (reinforecement + gamma * maxQ - q);
		setQ(state, action, value); // change the state, and update the Q values based on the reinforcement.

	}
	public  double[] maxQ(int s) {
		/*
		 * Find the maxQ (that's used in Q-learning).
		 * maxQ[0] is the value itself of maxQ and
		 * maxQ[1] is the action the maximizes maxQ
		 */
		double[] result=new double[2];
		int[] actionsFromState = actions[s];
		double maxValue = Q[s][0];
		for (int i = 1; i < actionsFromState.length; i++) {
			double value = Q[s][i];

			if (value >= maxValue) {
				result[0]=value;
				result[1]=i;
				maxValue = value;
			}
		}
		return result;
	}
	int findState(Coordinator co) {
		/*
		 * find the state describing how important the UCS is.
		 * Division found by trial & error
		 */
		double accume=0.0;
		for (CampaignData cd : co.impressionBidder.getMyActiveCampaigns()) {
			accume=accume+(((double)(cd.impsTogo()))/(cd.getReachImps()));
		}
		if (accume<range0) return 1;
		if (accume<range1) return 2;
		if (accume<range2) return 3;
		if (accume<range3) return 4;
		return 5;

	}

	double findReinforcement(AdNetworkReport anp, Coordinator co, int day) {
		/*
		 * find the reinforcement to be given to our agent.
		 * More details can be found in the project report
		 */
		int misses=0, hits=0;
		boolean hitForCampaign=false;
		for (AdNetworkKey k : anp.keys()) {
			hitForCampaign=false;
			Map<Integer, CampaignData> myCampaigns=co.getMyCampaigns();
			for (CampaignData cd : myCampaigns.values()) {
				if (relevantCampaign(cd, day) && matchSegment(cd, k)) {
					hits+=anp.getEntry(k).getBidCount();
					hitForCampaign=true;
				}
			}
			if (!hitForCampaign) {
				misses+=anp.getEntry(k).getBidCount();
			}
		}
		int activeCampagins=0;
		for (int i : co.getMyCampaigns().keySet()) {
			if (co.getMyCampaigns().get(i).getDayEnd()>=co.day) activeCampagins++;
		}
		if (misses!=0)
			return ((misses+hits)/((((double)(misses))*activeCampagins)*amountPaidYesterday))-2.75;
		return 1.5*hits;
	}

	boolean relevantCampaign(CampaignData cd, int day) {
		/*
		 * This function determines whether a campaign is relevant or not.
		 * A campaign is relevant if:
		 * 1. Its ending day has not elapsed
		 * 2. The number of impressions reached is lower than 110% impressions needed for the campaign
		 * (The percentage 110% was reached by running the game with a few agents, with the only difference
		 * between them being the abovementioned percentage.  
		 */
		if (cd.getReachImps()/cd.getStats().getTargetedImps()<=1.1 && cd.getDayEnd()>=day) return true;
		return false;
	}

	boolean matchSegment(CampaignData ms, AdNetworkKey k) {
		/*
		 * returns true iff the impressions opportunity's audience
		 * matches the campaign's required audience.
		 */
		Set<MarketSegment> s=ms.getTargetSegment();
		for (MarketSegment m : s) {
			if (m.name().equals("MALE")) {
				if (k.getGender().name().equals("female")) return false;
			}
			if (m.name().equals("FEMALE")) {
				if (k.getGender().name().equals("male")) return false;
			}
			if (m.name().equals("YOUNG")) {
				if ((k.getAge().name().equals("Age_45_54")) || (k.getAge().name().equals("Age_55_64")) || (k.getAge().name().equals("Age_65_PLUS"))) return false;
			}
			if (m.name().equals("OLD")) {
				if ((k.getAge().name().equals("Age_18_24")) || (k.getAge().name().equals("Age_25_34")) || (k.getAge().name().equals("Age_35_44"))) return false;
			}
			if (m.name().equals("LOW_INCOME")) {
				if ((k.getIncome().name().equals("high")) || (k.getIncome().name().equals("very_high"))) return false;
			}
			if (m.name().equals("HIGH_INCOME")) {
				if ((k.getIncome().name().equals("low")) || (k.getIncome().name().equals("medium"))) return false;
			}
		}
		return true;
	}

	int findFinalState(double reinforcement) {
		if (reinforcement<finalRange0) return stateG;
		if (reinforcement<finalRange1) return stateH;
		if (reinforcement<finalRange2) return stateI;
		if (reinforcement<finalRange3) return stateJ;
		return stateJ;
	}

	void setQ(int s, int a, double value) {
		/*
		 * Change the value of a specific cell in the
		 * Q values matrix
		 */
		this.Q[s][a] = value;
	}


}
