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
	 * Things to consider:
	 * 1.  I had to add setters and getters for MyCampaigns.
	 * 2. I have 5 states representing situations before the bid and 5 after it.
	 * perhaps we should make a transition from each pre-bid state to only 3 post-bid states (instead of all 5)?
	 * 3. Obviously - change the range and prices, maybe even alpha and learningRate
	 * 4. Perhaps we want to pay a bit more wisely on the first 5-6 days? (maybe a strategy we have?)
	 * 5. Still need to calculate the reinforcements
	 */
	double amountPaidYesterday;
	final int stateA = 0;
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
	final double[] prices={0,0.2,0.4,0.6,0.8};
	double alpha=0.1;
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
	double[][] Q = new double[statesCount][possibleActions]; // Q learning
	//static List<Double> amountpaid; // the ith entry represents the amount paid on the ith day.
	//static List<Integer> placeReached; // the ith entry represents the UCS place we got on the ith day.
	private static UCSbidder instance = null;

	protected UCSbidder() {
		// Exists only to defeat instantiation.
	}

	public static UCSbidder getInstance() {

		if(instance == null) {
			instance = new UCSbidder();
		}

		return instance;
	}

	public double getBid(Coordinator co){
		int state=findState(co);
		if (state==stateB) {
			this.action=0;
			this.state=stateB;
			this.amountPaidYesterday=0;
			return 0;
		}
		int action=(int) Math.round((maxQ(state))[1]);
		if (this.action==0) this.action=1;
		this.action=action;
		this.state=state;
		this.amountPaidYesterday=prices[action];
		return prices[action];
	}
	double bid(Coordinator co) {
		/*
         1. Set parameter , and environment reward matrix R 
         2. Initialize matrix Q as zero matrix 
         3. For each episode: Select random initial state 
            Do while not reach goal state o 
                Select one among all possible actions for the current state o 
                Using this possible action, consider to go to the next state o 
                Get maximum Q value of this next state based on all possible actions o 
                Compute o Set the next state as the current state
		 */

		// For each episode
		//for (int i = 0; i < 1000; i++) { // train episodes
		// Select random initial state
		//int state = rand.nextInt(statesCount);
		int state=findState(co);
		int action=(int) Math.round((maxQ(state))[1]);
		if (this.action==0) this.action=1;
		this.action=action;
		this.state=state;
		return prices[action];

	}


	/*
	 * A method to update the bidder for every campaign bidding result , in order to 
	 * store a history.
	 */
	double Q(int s, int a) {
		return this.Q[s][a];
	}
	public void updateUCS(AdNetworkReport anp, Coordinator co) {
		/*
		 */
		if (state==stateB) return;
		double reinforecement=findReinforcement(anp, co, co.day);

		nextstate=findFinalState(reinforecement);

		double q = Q(state, action);
		double maxQ = maxQ(state)[0];
		double value = q + alpha * (reinforecement + gamma * maxQ - q);
		setQ(state, action, value);
		state=nextstate;

	}
	public  double[] maxQ(int s) {
		double[] result=new double[2];
		int[] actionsFromState = actions[s];
		double maxValue = Q[s][0];
		for (int i = 1; i < actionsFromState.length; i++) {
			//int nextState = actionsFromState[i];
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
		double accume=0.0;
		for (Integer i : co.getMyCampaigns().keySet())
			if (co.getMyCampaigns().get(i).getReachImps()!=0)
				accume+=((double)(co.getMyCampaigns().get(i).impsTogo()))/(co.getMyCampaigns().get(i).getReachImps());
		if (accume<range0) return 1;
		if (accume<range1) return 2;
		if (accume<range2) return 3;
		if (accume<range3) return 4;
		return 5;

	}

	double findReinforcement(AdNetworkReport anp, Coordinator co, int day) {
		int misses=0, hits=0;
		boolean hitForCampaign=false;
		for (AdNetworkKey k : anp.keys()) {
			hitForCampaign=false;
			Map<Integer, CampaignData> myCampaigns=co.getMyCampaigns();
			//CampaignData currentCampaign=myCampaigns.get(k.getCampaignId());
			//Set<MarketSegment> currentSegment= currentCampaign.getTargetSegment();
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
			return ((misses+hits)/((((double)(misses))*activeCampagins)*amountPaidYesterday))-0.4;
		return 1.5*hits;
	}

	boolean relevantCampaign(CampaignData cd, int day) {
		if (cd.getReachImps()/cd.getStats().getTargetedImps()<=1.1 && cd.getDayEnd()>=day) return true;
		return false;
	}

	boolean matchSegment(CampaignData ms, AdNetworkKey k) {
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
		this.Q[s][a] = value;
	}


}
