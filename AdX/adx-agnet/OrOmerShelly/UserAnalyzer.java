package OrOmerShelly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math.analysis.NewtonSolver;

import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.devices.Device;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.report.adn.MarketSegment;

public class UserAnalyzer {

	private final static int NUMBER_OF_MARKET_SEGMENTS = 12;
	private final static int USER_POPULATION_SIZE = 10000;
	
	private final static String YAHOO = "yahoo";
	private final static String CNN = "cnn";
	private final static String NYTIMES = "nyt";
	private final static String HFNGTN = "hfn";
	private final static String MSN = "msn";
	private final static String FOX = "fox";
	private final static String AMAZON = "amazon";
	private final static String EBAY = "ebay";
	private final static String WALMART = "wallmart";
	private final static String TARGET = "target";
	private final static String BESTBUY = "bestbuy";
	private final static String SEARS = "sears";
	private final static String WEBMD = "webmd";
	private final static String EHOW = "ehow";
	private final static String ASK = "ask";
	private final static String TRIPADVISOR = "tripadvisor";
	private final static String CNET = "cnet";
	private final static String WEATHER = "weather";
	
	
	
	private Map<String, PublisherStats> publishersStats;
		
	// Private static member - Map publisher name + audience orientation to probabilities
	private static HashMap<AudienceOrientationKey, Double> audienceOrientationMap = getAudienceOrientation();
	private static HashMap<DeviceOrientationKey, Double> deviceOrientationMap = getDeviceOrientation();
	private static HashMap<String, Double> popularityMap = getPopularity();
	
	
	private static HashMap<DeviceOrientationKey, Double> getDeviceOrientation() {
		HashMap<DeviceOrientationKey, Double> map = new HashMap<DeviceOrientationKey, Double>();
		
		map.put(new DeviceOrientationKey(YAHOO, Device.mobile), 26.0);
		map.put(new DeviceOrientationKey(YAHOO, Device.pc), 100 - 26.0);
		
		map.put(new DeviceOrientationKey(CNN, Device.mobile), 24.0);
		map.put(new DeviceOrientationKey(CNN, Device.pc), 100 - 24.0);
		
		map.put(new DeviceOrientationKey(NYTIMES, Device.mobile), 23.0);
		map.put(new DeviceOrientationKey(NYTIMES, Device.pc), 100 - 23.0);
		
		map.put(new DeviceOrientationKey(HFNGTN, Device.mobile), 22.0);
		map.put(new DeviceOrientationKey(HFNGTN, Device.pc), 100 - 22.0);
		
		map.put(new DeviceOrientationKey(MSN, Device.mobile), 25.0);
		map.put(new DeviceOrientationKey(MSN, Device.pc), 100 - 25.0);
		
		map.put(new DeviceOrientationKey(FOX, Device.mobile), 24.0);
		map.put(new DeviceOrientationKey(FOX, Device.pc), 100 - 24.0);
		
		map.put(new DeviceOrientationKey(AMAZON, Device.mobile), 21.0);
		map.put(new DeviceOrientationKey(AMAZON, Device.pc), 100 - 21.0);
		
		map.put(new DeviceOrientationKey(EBAY, Device.mobile), 22.0);
		map.put(new DeviceOrientationKey(EBAY, Device.pc), 100 - 22.0);
		
		map.put(new DeviceOrientationKey(WALMART, Device.mobile), 18.0);
		map.put(new DeviceOrientationKey(WALMART, Device.pc), 100 - 18.0);
		
		map.put(new DeviceOrientationKey(TARGET, Device.mobile), 19.0);
		map.put(new DeviceOrientationKey(TARGET, Device.pc), 100 - 19.0);
		
		map.put(new DeviceOrientationKey(BESTBUY, Device.mobile), 20.0);
		map.put(new DeviceOrientationKey(BESTBUY, Device.pc), 100 - 20.0);
		
		map.put(new DeviceOrientationKey(SEARS, Device.mobile), 19.0);
		map.put(new DeviceOrientationKey(SEARS, Device.pc), 100 - 19.0);
		
		map.put(new DeviceOrientationKey(WEBMD, Device.mobile), 24.0);
		map.put(new DeviceOrientationKey(WEBMD, Device.pc), 100 - 24.0);
		
		map.put(new DeviceOrientationKey(EHOW, Device.mobile), 28.0);
		map.put(new DeviceOrientationKey(EHOW, Device.pc), 100 - 28.0);
		
		map.put(new DeviceOrientationKey(ASK, Device.mobile), 28.0);
		map.put(new DeviceOrientationKey(ASK, Device.pc), 100 - 28.0);
		
		map.put(new DeviceOrientationKey(TRIPADVISOR, Device.mobile), 30.0);
		map.put(new DeviceOrientationKey(TRIPADVISOR, Device.pc), 100 - 30.0);
		
		map.put(new DeviceOrientationKey(CNET, Device.mobile), 27.0);
		map.put(new DeviceOrientationKey(CNET, Device.pc), 100 - 27.0);
		
		map.put(new DeviceOrientationKey(WEATHER, Device.mobile), 31.0);
		map.put(new DeviceOrientationKey(WEATHER, Device.pc), 100 - 31.0);
				
		return map;
	}

	private static HashMap<String, Double> getPopularity() {
		HashMap<String, Double> map = new HashMap<String, Double>();
		map.put(YAHOO, 16.0);
		map.put(CNN, 2.2);
		map.put(NYTIMES, 3.1);
		map.put(HFNGTN, 8.1);
		map.put(MSN, 18.2);
		map.put(FOX, 3.1);
		map.put(AMAZON, 12.8);
		map.put(EBAY, 8.5);
		map.put(WALMART, 3.8);
		map.put(TARGET, 2.0);
		map.put(BESTBUY, 1.6);
		map.put(SEARS, 1.6);
		map.put(WEBMD, 2.5);
		map.put(EHOW, 2.5);
		map.put(ASK, 5.0);
		map.put(TRIPADVISOR, 1.6);
		map.put(CNET, 1.7);
		map.put(WEATHER, 5.8);
		return map;
	}

	private static HashMap<AudienceOrientationKey, Double> getAudienceOrientation() {
		List<AudienceOrientationRecord> orientationsCollection = new LinkedList<AudienceOrientationRecord>();
		// Assuming no below age 18 users, as they do not appear in User Popoulation Probabilities table. Therefore remaining part is given to "OLD" market segment.
		
		// Note: Calculations are "not pretty" to allow easy verification with the table.
		
		// Yahoo
		orientationsCollection.add(new AudienceOrientationRecord(YAHOO, MarketSegment.FEMALE, 50.4));
		orientationsCollection.add(new AudienceOrientationRecord(YAHOO, MarketSegment.MALE, 49.6));
		orientationsCollection.add(new AudienceOrientationRecord(YAHOO, MarketSegment.YOUNG, 12.2 + 17.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord(YAHOO, MarketSegment.OLD, 18.4 + 16.4 + (100 - (12.2 + 17.1 + 16.7) - (18.4 + 16.4))));
		orientationsCollection.add(new AudienceOrientationRecord(YAHOO, MarketSegment.LOW_INCOME, 53 + 27));
		orientationsCollection.add(new AudienceOrientationRecord(YAHOO, MarketSegment.HIGH_INCOME, 13 + (100 - 53 - 27 - 13)));
		
		// CNN
		orientationsCollection.add(new AudienceOrientationRecord(CNN, MarketSegment.FEMALE, 51.4));
		orientationsCollection.add(new AudienceOrientationRecord(CNN, MarketSegment.MALE, 48.6));
		orientationsCollection.add(new AudienceOrientationRecord(CNN, MarketSegment.YOUNG, 10.2 + 16.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord(CNN, MarketSegment.OLD, 19.4 + 17.4 + (100 - (10.2 + 16.1 + 16.7) - (19.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord(CNN, MarketSegment.LOW_INCOME, 48 + 27));
		orientationsCollection.add(new AudienceOrientationRecord(CNN, MarketSegment.HIGH_INCOME, 16 + (100 - 48 - 27 - 16)));		
		
		// NY Times
		orientationsCollection.add(new AudienceOrientationRecord(NYTIMES, MarketSegment.FEMALE, 52.4));
		orientationsCollection.add(new AudienceOrientationRecord(NYTIMES, MarketSegment.MALE, 47.6));
		orientationsCollection.add(new AudienceOrientationRecord(NYTIMES, MarketSegment.YOUNG, 9.2 + 15.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord(NYTIMES, MarketSegment.OLD, 19.4 + 17.4 + (100 - (9.2 + 15.1 + 16.7) - (19.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord(NYTIMES, MarketSegment.LOW_INCOME, 47 + 26));
		orientationsCollection.add(new AudienceOrientationRecord(NYTIMES, MarketSegment.HIGH_INCOME, 17 + (100 - 47 - 26 - 17)));		
		
		// Hfngtn
		orientationsCollection.add(new AudienceOrientationRecord(HFNGTN, MarketSegment.FEMALE, 53.4));
		orientationsCollection.add(new AudienceOrientationRecord(HFNGTN, MarketSegment.MALE, 46.6));
		orientationsCollection.add(new AudienceOrientationRecord(HFNGTN, MarketSegment.YOUNG, 10.2 + 16.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord(HFNGTN, MarketSegment.OLD, 19.4 + 17.4 + (100 - (10.2 + 16.1 + 16.7) - (19.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord(HFNGTN, MarketSegment.LOW_INCOME, 47 + 27));
		orientationsCollection.add(new AudienceOrientationRecord(HFNGTN, MarketSegment.HIGH_INCOME, 17 + (100 - 47 - 27 - 17)));		
		
		
		// MSN
		orientationsCollection.add(new AudienceOrientationRecord(MSN, MarketSegment.FEMALE, 52.4));
		orientationsCollection.add(new AudienceOrientationRecord(MSN, MarketSegment.MALE, 47.6));
		orientationsCollection.add(new AudienceOrientationRecord(MSN, MarketSegment.YOUNG, 10.2 + 16.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord(MSN, MarketSegment.OLD, 19.4 + 17.4 + (100 - (10.2 + 16.1 + 16.7) - (19.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord(MSN, MarketSegment.LOW_INCOME, 49 + 27));
		orientationsCollection.add(new AudienceOrientationRecord(MSN, MarketSegment.HIGH_INCOME, 16 + (100 - 49 - 27 - 16)));		
		
		// Fox
		orientationsCollection.add(new AudienceOrientationRecord(FOX, MarketSegment.FEMALE, 51.4));
		orientationsCollection.add(new AudienceOrientationRecord(FOX, MarketSegment.MALE, 48.6));
		orientationsCollection.add(new AudienceOrientationRecord(FOX, MarketSegment.YOUNG, 9.2 + 15.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord(FOX, MarketSegment.OLD, 19.4 + 18.4 + (100 - (9.2 + 15.1 + 16.7) - (19.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord(FOX, MarketSegment.LOW_INCOME, 46 + 26));
		orientationsCollection.add(new AudienceOrientationRecord(FOX, MarketSegment.HIGH_INCOME, 18 + (100 - 46 - 26 - 18)));		
		
		// Amazon
		orientationsCollection.add(new AudienceOrientationRecord(AMAZON, MarketSegment.FEMALE, 52.4));
		orientationsCollection.add(new AudienceOrientationRecord(AMAZON, MarketSegment.MALE, 47.6));
		orientationsCollection.add(new AudienceOrientationRecord(AMAZON, MarketSegment.YOUNG, 9.2 + 15.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord(AMAZON, MarketSegment.OLD, 19.4 + 18.4 + (100 - (9.2 + 15.1 + 16.7) - (19.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord(AMAZON, MarketSegment.LOW_INCOME, 50 + 27));
		orientationsCollection.add(new AudienceOrientationRecord(AMAZON, MarketSegment.HIGH_INCOME, 15 + (100 - 50 - 27 - 15)));		
		
		// Ebay
		orientationsCollection.add(new AudienceOrientationRecord(EBAY, MarketSegment.FEMALE, 51.4));
		orientationsCollection.add(new AudienceOrientationRecord(EBAY, MarketSegment.MALE, 48.6));
		orientationsCollection.add(new AudienceOrientationRecord(EBAY, MarketSegment.YOUNG, 9.2 + 16.1 + 15.7));
		orientationsCollection.add(new AudienceOrientationRecord(EBAY, MarketSegment.OLD, 19.4 + 17.4 + (100 - (9.2 + 16.1 + 15.7) - (19.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord(EBAY, MarketSegment.LOW_INCOME, 50 + 27));
		orientationsCollection.add(new AudienceOrientationRecord(EBAY, MarketSegment.HIGH_INCOME, 15 + (100 - 50 - 27 - 15)));		
		
		// Wal-Mart
		orientationsCollection.add(new AudienceOrientationRecord(WALMART, MarketSegment.FEMALE, 54.4));
		orientationsCollection.add(new AudienceOrientationRecord(WALMART, MarketSegment.MALE, 45.6));
		orientationsCollection.add(new AudienceOrientationRecord(WALMART, MarketSegment.YOUNG, 7.2 + 15.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord(WALMART, MarketSegment.OLD, 20.4 + 18.4 + (100 - (7.2 + 15.1 + 16.7) - (20.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord(WALMART, MarketSegment.LOW_INCOME, 50 + 27));
		orientationsCollection.add(new AudienceOrientationRecord(WALMART, MarketSegment.HIGH_INCOME, 15 + (100 - 50 - 27 - 15)));		
		
		// Target
		orientationsCollection.add(new AudienceOrientationRecord(TARGET, MarketSegment.FEMALE, 54.4));
		orientationsCollection.add(new AudienceOrientationRecord(TARGET, MarketSegment.MALE, 45.6));
		orientationsCollection.add(new AudienceOrientationRecord(TARGET, MarketSegment.YOUNG, 9.2 + 17.1 + 17.7));
		orientationsCollection.add(new AudienceOrientationRecord(TARGET, MarketSegment.OLD, 18.4 + 17.4 + (100 - (9.2 + 17.1 + 17.7) - (18.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord(TARGET, MarketSegment.LOW_INCOME, 45 + 27));
		orientationsCollection.add(new AudienceOrientationRecord(TARGET, MarketSegment.HIGH_INCOME, 19 + (100 - 45 - 27 - 19)));		
		
		// BestBuy
		orientationsCollection.add(new AudienceOrientationRecord(BESTBUY, MarketSegment.FEMALE, 52.4));
		orientationsCollection.add(new AudienceOrientationRecord(BESTBUY, MarketSegment.MALE, 47.6));
		orientationsCollection.add(new AudienceOrientationRecord(BESTBUY, MarketSegment.YOUNG, 10.2 + 14.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord(BESTBUY, MarketSegment.OLD, 20.4 + 17.4 + (100 - (10.2 + 14.1 + 16.7) - (20.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord(BESTBUY, MarketSegment.LOW_INCOME, 46.5 + 26));
		orientationsCollection.add(new AudienceOrientationRecord(BESTBUY, MarketSegment.HIGH_INCOME, 18 + (100 - 46.5 - 26 - 18)));		
		
		// Sears
		orientationsCollection.add(new AudienceOrientationRecord(SEARS, MarketSegment.FEMALE, 53.4));
		orientationsCollection.add(new AudienceOrientationRecord(SEARS, MarketSegment.MALE, 46.6));
		orientationsCollection.add(new AudienceOrientationRecord(SEARS, MarketSegment.YOUNG, 9.2 + 12.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord(SEARS, MarketSegment.OLD, 20.4 + 18.4 + (100 - (9.2 + 12.1 + 16.7) - (20.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord(SEARS, MarketSegment.LOW_INCOME, 45 + 25));
		orientationsCollection.add(new AudienceOrientationRecord(SEARS, MarketSegment.HIGH_INCOME, 20 + (100 - 45 - 25 - 20)));		
		
		// WebMD
		orientationsCollection.add(new AudienceOrientationRecord(WEBMD, MarketSegment.FEMALE, 54.4));
		orientationsCollection.add(new AudienceOrientationRecord(WEBMD, MarketSegment.MALE, 45.6));
		orientationsCollection.add(new AudienceOrientationRecord(WEBMD, MarketSegment.YOUNG, 9.2 + 15.1 + 15.7));
		orientationsCollection.add(new AudienceOrientationRecord(WEBMD, MarketSegment.OLD, 19.4 + 18.4 + (100 - (9.2 + 15.1 + 15.7) - (19.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord(WEBMD, MarketSegment.LOW_INCOME, 46 + 26.5));
		orientationsCollection.add(new AudienceOrientationRecord(WEBMD, MarketSegment.HIGH_INCOME, 18.5 + (100 - 46 - 26.5 - 18.5)));		
		
		// EHow
		orientationsCollection.add(new AudienceOrientationRecord(EHOW, MarketSegment.FEMALE, 52.4));
		orientationsCollection.add(new AudienceOrientationRecord(EHOW, MarketSegment.MALE, 47.6));
		orientationsCollection.add(new AudienceOrientationRecord(EHOW, MarketSegment.YOUNG, 10.2 + 15.1 + 15.7));
		orientationsCollection.add(new AudienceOrientationRecord(EHOW, MarketSegment.OLD, 19.4 + 17.4 + (100 - (10.2 + 15.1 + 15.7) - (19.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord(EHOW, MarketSegment.LOW_INCOME, 50 + 27));
		orientationsCollection.add(new AudienceOrientationRecord(EHOW, MarketSegment.HIGH_INCOME, 15 + (100 - 50 - 27 - 15)));		
		
		// Ask
		orientationsCollection.add(new AudienceOrientationRecord(ASK, MarketSegment.FEMALE, 51.4));
		orientationsCollection.add(new AudienceOrientationRecord(ASK, MarketSegment.MALE, 48.6));
		orientationsCollection.add(new AudienceOrientationRecord(ASK, MarketSegment.YOUNG, 10.2 + 13.1 + 15.7));
		orientationsCollection.add(new AudienceOrientationRecord(ASK, MarketSegment.OLD, 20.4 + 18.4 + (100 - (10.2 + 13.1 + 15.7) - (20.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord(ASK, MarketSegment.LOW_INCOME, 50 + 28));
		orientationsCollection.add(new AudienceOrientationRecord(ASK, MarketSegment.HIGH_INCOME, 15 + (100 - 50 - 28 - 15)));		
		
		// TripAdvisor
		orientationsCollection.add(new AudienceOrientationRecord(TRIPADVISOR, MarketSegment.FEMALE, 53.4));
		orientationsCollection.add(new AudienceOrientationRecord(TRIPADVISOR, MarketSegment.MALE, 46.6));
		orientationsCollection.add(new AudienceOrientationRecord(TRIPADVISOR, MarketSegment.YOUNG, 8.2 + 16.1 + 17.7));
		orientationsCollection.add(new AudienceOrientationRecord(TRIPADVISOR, MarketSegment.OLD, 20.4 + 17.4 + (100 - (8.2 + 16.1 + 17.7) - (20.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord(TRIPADVISOR, MarketSegment.LOW_INCOME, 46.5 + 26));
		orientationsCollection.add(new AudienceOrientationRecord(TRIPADVISOR, MarketSegment.HIGH_INCOME, 17.5 + (100 - 46.5 - 26 - 17.5)));		
		
		// CNet
		orientationsCollection.add(new AudienceOrientationRecord(CNET, MarketSegment.FEMALE, 49.4));
		orientationsCollection.add(new AudienceOrientationRecord(CNET, MarketSegment.MALE, 50.6));
		orientationsCollection.add(new AudienceOrientationRecord(CNET, MarketSegment.YOUNG, 12.2 + 15.1 + 15.7));
		orientationsCollection.add(new AudienceOrientationRecord(CNET, MarketSegment.OLD, 18.4 + 17.4 + (100 - (12.2 + 15.1 + 15.7) - (18.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord(CNET, MarketSegment.LOW_INCOME, 48 + 26.5));
		orientationsCollection.add(new AudienceOrientationRecord(CNET, MarketSegment.HIGH_INCOME, 16.5 + (100 - 48 - 26.5 - 16.5)));	
		
		// Weather
		orientationsCollection.add(new AudienceOrientationRecord(WEATHER, MarketSegment.FEMALE, 52.4));
		orientationsCollection.add(new AudienceOrientationRecord(WEATHER, MarketSegment.MALE, 47.6));
		orientationsCollection.add(new AudienceOrientationRecord(WEATHER, MarketSegment.YOUNG, 9.2 + 15.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord(WEATHER, MarketSegment.OLD, 20.4 + 18.4 + (100 - (9.2 + 15.1 + 16.7) - (20.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord(WEATHER, MarketSegment.LOW_INCOME, 45.5 + 26.5));
		orientationsCollection.add(new AudienceOrientationRecord(WEATHER, MarketSegment.HIGH_INCOME, 18.5 + (100 - 45.5 - 26.5 - 18.5)));		
		
		HashMap<AudienceOrientationKey, Double> map = new HashMap<AudienceOrientationKey, Double>();
		for (AudienceOrientationRecord orientationRecord : orientationsCollection) {
			map.put(orientationRecord.getKey(), orientationRecord.getOrientation());
		}
		
		return map;
	}

	public List<ImpressionParamtersDistributionKey> calcImpressionDistribution(String publisherName) {
		List<ImpressionParamtersDistributionKey> weights = new ArrayList<ImpressionParamtersDistributionKey>();
		PublisherStats publisherStats = (publishersStats == null ? null : publishersStats.get(publisherName));
		
		// Ad type orientation may be unknown. Therefore, if not stats are available for the publisher, average on what you already have.
		if (publisherStats == null) {
			publisherStats = getAveragedPublisherStatsForPublisher(publisherName);
		}
		
		// Calculate for all 12 market segments and include device and ad-type
		List<Set<MarketSegment>> allMarketSegments = MarketSegment.compundMarketSegments();
		for (Set<MarketSegment> marketSegment : allMarketSegments) {
			for (Device device : Device.values()) {
				for (AdType adType : AdType.values()) {
					ImpressionParameters impParams = new ImpressionParameters(marketSegment, device, adType);
					Double weight = 1.0;
					String logString = "";
					
					for (MarketSegment partialMarketSegmnet : marketSegment) {
						logString += "(" + audienceOrientationMap.get(new AudienceOrientationKey(publisherName, partialMarketSegmnet)) / 100  + "/100) *";
						weight *= audienceOrientationMap.get(new AudienceOrientationKey(publisherName, partialMarketSegmnet)) / 100; 
					}
					
					weight *= deviceOrientationMap.get(new DeviceOrientationKey(publisherName, device))  / 100;
					logString += "(" +  deviceOrientationMap.get(new DeviceOrientationKey(publisherName, device))  / 100 + "/100) *";
					weight *= ((double)(adType == AdType.text ? publisherStats.getTextOrientation() : publisherStats.getVideoOrientation()) / publisherStats.getPopularity());
					logString += "(" + (adType == AdType.text ? publisherStats.getTextOrientation() : publisherStats.getVideoOrientation()) + " / " + publisherStats.getPopularity() + ") *";
					weight *= popularityMap.get(publisherName) / 100;
					logString += "(" + popularityMap.get(publisherName) / 100 + "/100)";
					
					System.out.println("PUBLISHER_STATS = " + publisherStats + "; ADTYPE = " + adType + "; WEIGHT = " + logString);
					
					ImpressionParamtersDistributionKey key = new ImpressionParamtersDistributionKey(impParams, weight);
					weights.add(key);					
				}
			}
		}
		
		return weights;
	}

	public PublisherStats getAveragedPublisherStatsForPublisher(String publisherName) {
		double sumText = 0.0;
		double sumVideo = 0.0;
		double avgText = 0.0;
		double avgVideo = 0.0;
		double popularity = USER_POPULATION_SIZE * popularityMap.get(publisherName) / 100;
		
		int count = (publishersStats == null ? 0 : publishersStats.values().size());
		
		if (count == 0) {
			System.out.println("Returning default publisher stats = " + USER_POPULATION_SIZE + "*" + popularityMap.get(publisherName) + "/100 ; Math.round(popularity/2)=" + Math.round(popularity/2));
			return new PublisherStats(Math.round(popularity), Math.round(popularity/2), Math.round(popularity/2));
		}
		
		for (PublisherStats publisherStats : publishersStats.values()) {
			long publisherPopularity = publisherStats.getPopularity();
			sumText += (double)publisherStats.getTextOrientation() / publisherPopularity;
			sumVideo += (double)publisherStats.getVideoOrientation() / publisherPopularity;
		}
		
		avgText = sumText / count;
		avgVideo = sumVideo / count;
				
		return new PublisherStats(Math.round(popularity), Math.round(avgVideo * popularity), Math.round(avgText * popularity));
	}

	public double calcMedianOfMarketSegmentsWeights(String publisherName) {
		List<Double> weights = new ArrayList<Double>(NUMBER_OF_MARKET_SEGMENTS);
		
		List<Set<MarketSegment>> allMarketSegments = MarketSegment.compundMarketSegments();
		for (Set<MarketSegment> marketSegment : allMarketSegments) {
			double weight = 1.0;
			
			for (MarketSegment partialMarketSegment : marketSegment) {
				weight *= audienceOrientationMap.get(new AudienceOrientationKey(publisherName, partialMarketSegment)) / 100; 
			}
			
			weights.add(weight);
		}
		
		return weights.get(NUMBER_OF_MARKET_SEGMENTS/2 - 1);		
	}

	public double getMarketSegmentWeight(Set<MarketSegment> targetSegment, String publisherName) {
		double weight = 1.0;
		
		for (MarketSegment partialMarketSegmnet : targetSegment) {
			weight *= audienceOrientationMap.get(new AudienceOrientationKey(publisherName, partialMarketSegmnet)) / 100; 
		}
		
		return weight;
	}
	
}
