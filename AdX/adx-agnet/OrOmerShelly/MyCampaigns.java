package OrOmerShelly;

import java.util.ArrayList;
import java.util.Set;

import tau.tac.adx.report.adn.MarketSegment;
import OrOmerShelly.oos.bidders.CampaignBidder;

public class MyCampaigns {

	private ArrayList<CampaignData> data;
	
	private static MyCampaigns instance = null;
	
	private MyCampaigns() {
		data = new ArrayList<CampaignData>();
	}
	
	public static MyCampaigns getInstance(){		

		if (MyCampaigns.instance==null){
			MyCampaigns.instance = new MyCampaigns();
		}
		
		return MyCampaigns.instance;
	}

	public void addCampaign(CampaignData c){
	
		data.add(c);
	
	}
	
	public ArrayList<CampaignData> getActiveCampaigns(int day){
		
		ArrayList<CampaignData> newArr = new ArrayList<CampaignData>();
		for (CampaignData c : data) {
			if (c.dayStart <= day && c.dayEnd >= day){
				newArr.add(c);
			}
		}
		
		return newArr;
	}
	
}
