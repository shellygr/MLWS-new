package OrOmerShelly;

import java.util.ArrayList;

public class MyCampaigns {

	private ArrayList<CampaignData> data; // contains all the campaigns we won.
	
	private static MyCampaigns instance = null; // our singleton
	
	private MyCampaigns() {
		data = new ArrayList<CampaignData>();
	}
	
	public static MyCampaigns getInstance(){		

		if (MyCampaigns.instance==null){
			MyCampaigns.instance = new MyCampaigns();
		}
		
		return MyCampaigns.instance;
	}

	// each time we win we need to call that externally
	public void addCampaign(CampaignData c){
	
		data.add(c);
	
	}
	
	/*
	 * Returns a List containing all campaigns we win and is/was/will be active at particular day.
	 */
	public ArrayList<CampaignData> getActiveCampaigns(long day){
		
		ArrayList<CampaignData> newArr = new ArrayList<CampaignData>();
		for (CampaignData c : data) {
			if (c.dayStart <= day && c.dayEnd >= day){
				newArr.add(c);
			}
		}
		
		return newArr;
	}
	
}
