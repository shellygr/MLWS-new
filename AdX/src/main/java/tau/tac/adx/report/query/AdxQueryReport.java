/*
 * QueryReport.java
 *
 * COPYRIGHT  2008
 * THE REGENTS OF THE UNIVERSITY OF MICHIGAN
 * ALL RIGHTS RESERVED
 *
 * PERMISSION IS GRANTED TO USE, COPY, CREATE DERIVATIVE WORKS AND REDISTRIBUTE THIS
 * SOFTWARE AND SUCH DERIVATIVE WORKS FOR NONCOMMERCIAL EDUCATION AND RESEARCH
 * PURPOSES, SO LONG AS NO FEE IS CHARGED, AND SO LONG AS THE COPYRIGHT NOTICE
 * ABOVE, THIS GRANT OF PERMISSION, AND THE DISCLAIMER BELOW APPEAR IN ALL COPIES
 * MADE; AND SO LONG AS THE NAME OF THE UNIVERSITY OF MICHIGAN IS NOT USED IN ANY
 * ADVERTISING OR PUBLICITY PERTAINING TO THE USE OR DISTRIBUTION OF THIS SOFTWARE
 * WITHOUT SPECIFIC, WRITTEN PRIOR AUTHORIZATION.
 *
 * THIS SOFTWARE IS PROVIDED AS IS, WITHOUT REPRESENTATION FROM THE UNIVERSITY OF
 * MICHIGAN AS TO ITS FITNESS FOR ANY PURPOSE, AND WITHOUT WARRANTY BY THE
 * UNIVERSITY OF MICHIGAN OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT
 * LIMITATION THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE REGENTS OF THE UNIVERSITY OF MICHIGAN SHALL NOT BE LIABLE FOR ANY
 * DAMAGES, INCLUDING SPECIAL, INDIRECT, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, WITH
 * RESPECT TO ANY CLAIM ARISING OUT OF OR IN CONNECTION WITH THE USE OF THE SOFTWARE,
 * EVEN IF IT HAS BEEN OR IS HEREAFTER ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */
package tau.tac.adx.report.query;


/**
 * Query report contains impressions, clicks, cost, average position, and ad
 * displayed by the advertiser for each query class during the period as well as
 * the positions and displayed ads of all advertisers during the period for each
 * query class.
 * 
 * @author Ben Cassell, Patrick Jordan, Lee Callender
 */
//public class AdxQueryReport extends
//		AbstractQueryKeyedReportTransportable<AdxQueryReportEntry> {
//
// /**
// * The serial version id.
// */
// private static final long serialVersionUID = -7957495904471250085L;
//
// /**
// * Creates a {@link AdxQueryReportEntry} with the given {@link Query
// query}
// * as the key.
// *
// * @param query
// * the query key
// * @return a {@link AdxQueryReportEntry} with the given {@link Query
// query}
// * as the key.
// */
// @Override
// protected final AdxQueryReportEntry createEntry(final Query query) {
// AdxQueryReportEntry entry = new AdxQueryReportEntry();
// entry.setQuery(query);
// return entry;
// }
//
// /**
// * Returns the {@link AdxQueryReportEntry} class.
// *
// * @return the {@link AdxQueryReportEntry} class.
// */
// @Override
// protected final Class entryClass() {
// return AdxQueryReportEntry.class;
// }
//
// /**
// * Creates a new query report.
// */
// public AdxQueryReport() {
// }
//
// /**
// * Adds a {@link AdxQueryReportEntry} keyed with the specificed query and
// * the associated viewing statistics.
// *
// * @param query
// * the query key.
// * @param impressions
// * the number of regular impressions.
// * @param promotedImpressions
// * the number of promoted impressions.
// * @param clicks
// * the number of clicks.
// * @param cost
// * the cost of the clicks.
// * @param positionSum
// * the sum of the positions over all impressions.
// */
// public final void addQuery(final Query query, final int impressions,
// final int promotedImpressions, final int clicks, final double cost,
// final double positionSum) {
// lockCheck();
//
// int index = addQuery(query);
// AdxQueryReportEntry entry = getEntry(index);
// entry.setImpressions(impressions, promotedImpressions);
// entry.setClicks(clicks);
// entry.setCost(cost);
// entry.setPositionSum(positionSum);
// }
//
// /**
// * Sets the sum of the positions over all the impressions for a query.
// *
// * @param query
// * the query
// * @param positionSum
// * the sum of the positions over all the impressions for a query.
// */
// public final void setPositionSum(final Query query, final double
// positionSum) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// index = addQuery(query);
// }
//
// setPositionSum(index, positionSum);
//
// }
//
// /**
// * Sets the sum of the positions over all the impressions for a query.
// *
// * @param index
// * the index of the query
// * @param positionSum
// * the sum of the positions over all the impressions for a query.
// */
// public final void setPositionSum(final int index, final double
// positionSum) {
// lockCheck();
// getEntry(index).setPositionSum(positionSum);
// }
//
// /**
// * Sets the cost associated with the query.
// *
// * @param query
// * the query
// * @param cost
// * the cost associated with the query.
// */
// public final void setCost(final Query query, final double cost) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// index = addQuery(query);
// }
//
// setCost(index, cost);
//
// }
//
// /**
// * Sets the cost associated with the query.
// *
// * @param index
// * the query index
// * @param cost
// * the cost associated with the query.
// */
// public final void setCost(final int index, final double cost) {
// lockCheck();
// getEntry(index).setCost(cost);
// }
//
// /**
// * Sets the impressions associated with the query.
// *
// * @param query
// * the query
// * @param regularImpressions
// * the regular impressions
// * @param promotedImpressions
// * the promoted impressions
// */
// public final void setImpressions(final Query query,
// final int regularImpressions, final int promotedImpressions) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// index = addQuery(query);
// }
//
// setImpressions(index, regularImpressions, promotedImpressions);
//
// }
//
// /**
// * Sets the impressions associated with the query.
// *
// * @param query
// * the query
// * @param regularImpressions
// * the regular impressions
// * @param promotedImpressions
// * the promoted impressions
// * @param ad
// * the ad shown
// * @param positionSum
// * the sum of positions over all impressions
// */
// public final void setImpressions(final Query query,
// final int regularImpressions, final int promotedImpressions,
// final Ad ad, final double positionSum) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// index = addQuery(query);
// }
//
// setImpressions(index, regularImpressions, promotedImpressions, ad,
// positionSum);
// }
//
// /**
// * Adds the impressions associated with the query.
// *
// * @param query
// * the query
// * @param regular
// * the reqular impressions
// * @param promoted
// * the promoted impressions
// */
// public final void addImpressions(final Query query, final int regular,
// final int promoted) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// setImpressions(query, regular, promoted);
// } else {
// addImpressions(index, regular, promoted);
// }
// }
//
// /**
// * Adds the impressions associated with the query.
// *
// * @param query
// * the query
// * @param regular
// * the reqular impressions
// * @param promoted
// * the promoted impressions
// * @param ad
// * the ad shown
// * @param positionSum
// * the sum of positions over all impressions
// */
// public final void addImpressions(final Query query, final int regular,
// final int promoted, final Ad ad, final double positionSum) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// setImpressions(query, regular, promoted, ad, positionSum);
// } else {
// addImpressions(index, regular, promoted, ad, positionSum);
// }
// }
//
// /**
// * Adds the impressions associated with the query.
// *
// * @param index
// * the query index
// * @param regular
// * the reqular impressions
// * @param promoted
// * the promoted impressions
// */
// public final void addImpressions(final int index, final int regular,
// final int promoted) {
// lockCheck();
//
// getEntry(index).addImpressions(regular, promoted);
//
// }
//
// /**
// * Adds the impressions associated with the query.
// *
// * @param index
// * the query index
// * @param regular
// * the reqular impressions
// * @param promoted
// * the promoted impressions
// * @param ad
// * the ad shown
// * @param positionSum
// * the sum of positions over all impressions
// */
// public final void addImpressions(final int index, final int regular,
// final int promoted, final Ad ad, final double positionSum) {
// lockCheck();
//
// getEntry(index).addImpressions(regular, promoted);
// getEntry(index).setAd(ad);
// getEntry(index).addPosition(positionSum);
// }
//
// /**
// * Sets the impressions associated with the query.
// *
// * @param index
// * the query index
// * @param regularImpressions
// * the regular impressions
// * @param promotedImpressions
// * the promoted impressions
// */
// public final void setImpressions(final int index,
// final int regularImpressions, final int promotedImpressions) {
// lockCheck();
// getEntry(index).setImpressions(regularImpressions, promotedImpressions);
// }
//
// /**
// * Sets the impressions associated with the query.
// *
// * @param index
// * the query index
// * @param regular
// * the regular impressions
// * @param promoted
// * the promoted impressions
// * @param ad
// * the ad shown
// * @param positionSum
// * the sum of positions over all impressions
// */
// public final void setImpressions(final int index, final int regular,
// final int promoted, final Ad ad, final double positionSum) {
// lockCheck();
// getEntry(index).setImpressions(regular, promoted);
// getEntry(index).setPositionSum(positionSum);
// getEntry(index).setAd(ad);
// }
//
// /**
// * Sets the clicks for associated query.
// *
// * @param query
// * the query
// * @param clicks
// * the clicks
// */
// public final void setClicks(final Query query, final int clicks) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// index = addQuery(query);
// }
//
// setClicks(index, clicks);
//
// }
//
// /**
// * Sets the clicks for associated query.
// *
// * @param query
// * the query
// * @param clicks
// * the clicks
// * @param cost
// * the cost
// */
// public final void setClicks(final Query query, final int clicks,
// final double cost) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// index = addQuery(query);
// }
//
// setClicks(index, clicks, cost);
//
// }
//
// /**
// * Sets the clicks for associated query.
// *
// * @param index
// * the query index
// * @param clicks
// * the clicks
// */
// public final void setClicks(final int index, final int clicks) {
// lockCheck();
// getEntry(index).setClicks(clicks);
// }
//
// /**
// * Sets the clicks for associated query.
// *
// * @param index
// * the query index
// * @param clicks
// * the clicks
// * @param cost
// * the cost
// */
// public final void setClicks(final int index, final int clicks,
// final double cost) {
// lockCheck();
// getEntry(index).setClicks(clicks);
// getEntry(index).setCost(cost);
// }
//
// /**
// * Adds the clicks to the associated query.
// *
// * @param query
// * the query
// * @param clicks
// * the clicks
// */
// public final void addClicks(final Query query, final int clicks) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// setClicks(query, clicks);
// } else {
// addClicks(index, clicks);
// }
// }
//
// /**
// * Adds the clicks to the associated query.
// *
// * @param query
// * the query
// * @param clicks
// * the clicks
// * @param cost
// * the cost
// */
// public final void addClicks(final Query query, final int clicks,
// final double cost) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// setClicks(query, clicks, cost);
// } else {
// addClicks(index, clicks, cost);
// }
// }
//
// /**
// * Adds the clicks to the associated query.
// *
// * @param index
// * the query index
// * @param clicks
// * the clicks
// */
// public final void addClicks(final int index, final int clicks) {
// lockCheck();
// getEntry(index).addClicks(clicks);
// }
//
// /**
// * Adds the clicks to the associated query.
// *
// * @param index
// * the query index
// * @param clicks
// * the clicks
// * @param cost
// * the cost
// */
// public final void addClicks(final int index, final int clicks,
// final double cost) {
// lockCheck();
// getEntry(index).addClicks(clicks);
// getEntry(index).addCost(cost);
// }
//
// /**
// * Adds the cost to the associated query.
// *
// * @param query
// * the query
// * @param cost
// * the cost
// */
// public final void addCost(final Query query, final double cost) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// setCost(query, cost);
// } else {
// addCost(index, cost);
// }
// }
//
// /**
// * Adds the cost to the associated query.
// *
// * @param index
// * the query index
// * @param cost
// * the cost
// */
// public final void addCost(final int index, final double cost) {
// lockCheck();
// getEntry(index).addCost(cost);
// }
//
// /**
// * Returns the average position for the associated query.
// *
// * @param query
// * the query
// * @return the average position for the associated query.
// */
// public final double getPosition(final Query query) {
// int index = indexForEntry(query);
//
// return index < 0 ? Double.NaN : getPosition(index);
// }
//
// /**
// * Returns the average position for the associated query.
// *
// * @param index
// * the query index
// * @return the average position for the associated query.
// */
// public final double getPosition(final int index) {
// return getEntry(index).getPosition();
// }
//
// /**
// * Returns the average CPC for the associated query.
// *
// * @param query
// * the query
// * @return the average CPC for the associated query.
// */
// public final double getCPC(final Query query) {
// int index = indexForEntry(query);
//
// return index < 0 ? Double.NaN : getCPC(index);
// }
//
// /**
// * Returns the average CPC for the associated query.
// *
// * @param index
// * the query index
// * @return the average CPC for the associated query.
// */
// public final double getCPC(final int index) {
// return getEntry(index).getCPC();
// }
//
// /**
// * Returns the total number of impressions for the associated query.
// *
// * @param query
// * the query
// * @return the total number of impressions for the associated query.
// */
// public final int getImpressions(final Query query) {
// int index = indexForEntry(query);
//
// return index < 0 ? 0 : getImpressions(index);
// }
//
// /**
// * Returns the total number of impressions for the associated query.
// *
// * @param index
// * the query index
// * @return the total number of impressions for the associated query.
// */
// public final int getImpressions(final int index) {
// return getEntry(index).getImpressions();
// }
//
// /**
// * Returns the total number of regular impressions for the associated
// query.
// *
// * @param query
// * the query
// * @return the total number of regular impressions for the associated
// query.
// */
// public final int getRegularImpressions(final Query query) {
// int index = indexForEntry(query);
//
// return index < 0 ? 0 : getRegularImpressions(index);
// }
//
// /**
// * Returns the total number of regular impressions for the associated
// query.
// *
// * @param index
// * the query index
// * @return the total number of regular impressions for the associated
// query.
// */
// public final int getRegularImpressions(final int index) {
// return getEntry(index).getRegularImpressions();
// }
//
// /**
// * Returns the total number of promoted impressions for the associated
// * query.
// *
// * @param query
// * the query
// * @return the total number of promoted impressions for the associated
// * query.
// */
// public final int getPromotedImpressions(final Query query) {
// int index = indexForEntry(query);
//
// return index < 0 ? 0 : getPromotedImpressions(index);
// }
//
// /**
// * Returns the total number of promoted impressions for the associated
// * query.
// *
// * @param index
// * the query index
// * @return the total number of promoted impressions for the associated
// * query.
// */
// public final int getPromotedImpressions(final int index) {
// return getEntry(index).getPromotedImpressions();
// }
//
// /**
// * Returns the total number of clicks for the associated query.
// *
// * @param query
// * the query
// * @return the total number of clicks for the associated query.
// */
// public final int getClicks(final Query query) {
// int index = indexForEntry(query);
//
// return index < 0 ? 0 : getClicks(index);
// }
//
// /**
// * Returns the total number of clicks for the associated query.
// *
// * @param index
// * the query index
// * @return the total number of clicks for the associated query.
// */
// public final int getClicks(final int index) {
// return getEntry(index).getClicks();
// }
//
// /**
// * Returns the total cost for the associated query.
// *
// * @param query
// * the query
// * @return the total cost for the associated query.
// */
// public final double getCost(final Query query) {
// int index = indexForEntry(query);
//
// return index < 0 ? 0.0 : getCost(index);
// }
//
// /**
// * Returns the total cost for the associated query.
// *
// * @param index
// * the query index
// * @return the total cost for the associated query.
// */
// public final double getCost(final int index) {
// return getEntry(index).getCost();
// }
//
// /**
// * Returns the average position for the associated query and advertiser.
// *
// * @param query
// * the query
// * @param advertiser
// * the advertiser
// * @return the average position for the associated query and advertiser.
// */
// public final double getPosition(final Query query, final String
// advertiser) {
// int index = indexForEntry(query);
//
// return index < 0 ? Double.NaN : getPosition(index, advertiser);
// }
//
// /**
// * Returns the average position for the associated query and advertiser.
// *
// * @param index
// * the query index
// * @param advertiser
// * the advertiser
// * @return the average position for the associated query and advertiser.
// */
// public final double getPosition(final int index, final String advertiser)
// {
// return getEntry(index).getPosition(advertiser);
// }
//
// /**
// * Sets the average position for the associated query and advertiser.
// *
// * @param query
// * the query
// * @param advertiser
// * the advertiser
// * @param position
// * the average position for the associated query and advertiser.
// */
// public final void setPosition(final Query query, final String advertiser,
// final double position) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// index = addQuery(query);
// }
//
// setPosition(index, advertiser, position);
// }
//
// /**
// * Sets the average position for the associated query and advertiser.
// *
// * @param index
// * the query index
// * @param advertiser
// * the advertiser
// * @param position
// * the average position for the associated query and advertiser.
// */
// public final void setPosition(final int index, final String advertiser,
// final double position) {
// lockCheck();
//
// getEntry(index).setPosition(advertiser, position);
// }
//
// /**
// * Returns the shown ad for the associated query.
// *
// * @param query
// * the query
// * @return the shown ad for the associated query.
// */
// public final Ad getAd(final Query query) {
// int index = indexForEntry(query);
//
// return index < 0 ? null : getAd(index);
// }
//
// /**
// * Returns the shown ad for the associated query.
// *
// * @param index
// * the query index
// * @return the shown ad for the associated query.
// */
// public final Ad getAd(final int index) {
// return getEntry(index).getAd();
// }
//
// /**
// * Sets the shown ad for the associated query.
// *
// * @param query
// * the query
// * @param ad
// * the ad
// */
// public final void setAd(final Query query, final Ad ad) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// index = addQuery(query);
// }
//
// setAd(index, ad);
// }
//
// /**
// * Sets the shown ad for the associated query.
// *
// * @param index
// * the query index
// * @param ad
// * the ad
// */
// public final void setAd(final int index, final Ad ad) {
// lockCheck();
//
// getEntry(index).setAd(ad);
// }
//
// /**
// * Returns the shown ad for the associated query and advertiser.
// *
// * @param query
// * the query
// * @param advertiser
// * the advertiser
// * @return the shown ad for the associated query and advertiser.
// */
// public final Ad getAd(final Query query, final String advertiser) {
// int index = indexForEntry(query);
//
// return index < 0 ? null : getAd(index, advertiser);
// }
//
// /**
// * Returns the shown ad for the associated query and advertiser.
// *
// * @param index
// * the query index
// * @param advertiser
// * the advertiser
// * @return the shown ad for the associated query and advertiser.
// */
// public final Ad getAd(final int index, final String advertiser) {
// return getEntry(index).getAd(advertiser);
// }
//
// /**
// * Sets the shown ad for the associated query and advertiser.
// *
// * @param query
// * the query
// * @param advertiser
// * the advertiser
// * @param ad
// * the shown ad for the associated query and advertiser.
// */
// public final void setAd(final Query query, final String advertiser,
// final Ad ad) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// index = addQuery(query);
// }
//
// setAd(index, advertiser, ad);
// }
//
// /**
// * Sets the shown ad for the associated query and advertiser.
// *
// * @param index
// * the query index
// * @param advertiser
// * the advertiser
// * @param ad
// * the shown ad for the associated query and advertiser.
// */
// public final void setAd(final int index, final String advertiser,
// final Ad ad) {
// lockCheck();
//
// getEntry(index).setAd(advertiser, ad);
// }
//
// /**
// * Sets the shown ad and average position for the associated query and
// * advertiser.
// *
// * @param query
// * the query
// * @param advertiser
// * the advertiser
// * @param ad
// * the shown ad for the associated query and advertiser.
// * @param position
// * the average position for the associated query and advertiser.
// */
// public final void setAdAndPosition(final Query query,
// final String advertiser, final Ad ad, final double position) {
// lockCheck();
//
// int index = indexForEntry(query);
//
// if (index < 0) {
// index = addQuery(query);
// }
//
// setAdAndPosition(index, advertiser, ad, position);
// }
//
// /**
// * Sets the shown ad and average position for the associated query and
// * advertiser.
// *
// * @param index
// * the query index
// * @param advertiser
// * the advertiser
// * @param ad
// * the shown ad for the associated query and advertiser.
// * @param position
// * the average position for the associated query and advertiser.
// */
// public final void setAdAndPosition(final int index,
// final String advertiser, final Ad ad, final double position) {
// lockCheck();
//
// getEntry(index).setAdAndPosition(advertiser, ad, position);
// }
//
// /**
// * Returns the set of advertisers with data for the given query.
// *
// * @param query
// * the query
// * @return the set of advertisers with data for the given query.
// */
// public final Set<String> advertisers(final Query query) {
// int index = indexForEntry(query);
//
// return index < 0 ? Collections.EMPTY_SET : advertisers(index);
// }
//
// /**
// * Returns the set of advertisers with data for the given query.
// *
// * @param index
// * the query index
// * @return the set of advertisers with data for the given query.
// */
// public final Set<String> advertisers(final int index) {
// return getEntry(index).advertisers();
// }
//
// }
