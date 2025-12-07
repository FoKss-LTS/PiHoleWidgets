/*
 *
 *  Copyright (C) 2022.  Reda ELFARISSI aka foxy999
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package domain.pihole;

import java.util.StringJoiner;

/**
 * Represents Pi-hole statistics and status data.
 * Mutable for Jackson deserialization compatibility.
 */
public class PiHole {

    private static final String STATUS_ENABLED = "enabled";
    
    private long domainsBeingBlocked;
    private long dnsQueriesToday;
    private long adsBlockedToday;
    private double adsPercentageToday;
    private long uniqueDomains;
    private long queriesForwarded;
    private long queriesCached;
    private long clientsEverSeen;
    private long uniqueClients;
    private long dnsQueriesAllTypes;
    private long replyNodata;
    private long replyNxdomain;
    private long replyCname;
    private long replyIp;
    private long privacyLevel;
    private String status;
    private Gravity gravity;

    /**
     * Default constructor for Jackson deserialization.
     */
    public PiHole() {
    }

    /**
     * Full constructor.
     */
    public PiHole(long domainsBeingBlocked, long dnsQueriesToday, long adsBlockedToday,
                  double adsPercentageToday, long uniqueDomains, long queriesForwarded,
                  long queriesCached, long clientsEverSeen, long uniqueClients,
                  long dnsQueriesAllTypes, long replyNodata, long replyNxdomain,
                  long replyCname, long replyIp, long privacyLevel, String status,
                  Gravity gravity) {
        this.domainsBeingBlocked = domainsBeingBlocked;
        this.dnsQueriesToday = dnsQueriesToday;
        this.adsBlockedToday = adsBlockedToday;
        this.adsPercentageToday = adsPercentageToday;
        this.uniqueDomains = uniqueDomains;
        this.queriesForwarded = queriesForwarded;
        this.queriesCached = queriesCached;
        this.clientsEverSeen = clientsEverSeen;
        this.uniqueClients = uniqueClients;
        this.dnsQueriesAllTypes = dnsQueriesAllTypes;
        this.replyNodata = replyNodata;
        this.replyNxdomain = replyNxdomain;
        this.replyCname = replyCname;
        this.replyIp = replyIp;
        this.privacyLevel = privacyLevel;
        this.status = status;
        this.gravity = gravity;
    }

    // ==================== Getters ====================

    public long getDomainsBeingBlocked() {
        return domainsBeingBlocked;
    }

    // Legacy getter name for backward compatibility
    public long getDomains_being_blocked() {
        return domainsBeingBlocked;
    }

    public long getDnsQueriesToday() {
        return dnsQueriesToday;
    }

    public long getDns_queries_today() {
        return dnsQueriesToday;
    }

    public long getAdsBlockedToday() {
        return adsBlockedToday;
    }

    public long getAds_blocked_today() {
        return adsBlockedToday;
    }

    public double getAdsPercentageToday() {
        return adsPercentageToday;
    }

    public double getAds_percentage_today() {
        return adsPercentageToday;
    }

    public long getUniqueDomains() {
        return uniqueDomains;
    }

    public long getUnique_domains() {
        return uniqueDomains;
    }

    public long getQueriesForwarded() {
        return queriesForwarded;
    }

    public long getQueries_forwarded() {
        return queriesForwarded;
    }

    public long getQueriesCached() {
        return queriesCached;
    }

    public long getQueries_cached() {
        return queriesCached;
    }

    public long getClientsEverSeen() {
        return clientsEverSeen;
    }

    public long getClients_ever_seen() {
        return clientsEverSeen;
    }

    public long getUniqueClients() {
        return uniqueClients;
    }

    public long getUnique_clients() {
        return uniqueClients;
    }

    public long getDnsQueriesAllTypes() {
        return dnsQueriesAllTypes;
    }

    public long getDns_queries_all_types() {
        return dnsQueriesAllTypes;
    }

    public long getReplyNodata() {
        return replyNodata;
    }

    public long getReply_NODATA() {
        return replyNodata;
    }

    public long getReplyNxdomain() {
        return replyNxdomain;
    }

    public long getReply_NXDOMAIN() {
        return replyNxdomain;
    }

    public long getReplyCname() {
        return replyCname;
    }

    public long getReply_CNAME() {
        return replyCname;
    }

    public long getReplyIp() {
        return replyIp;
    }

    public long getReply_IP() {
        return replyIp;
    }

    public long getPrivacyLevel() {
        return privacyLevel;
    }

    public long getPrivacy_level() {
        return privacyLevel;
    }

    public String getStatus() {
        return status;
    }

    public Gravity getGravity() {
        return gravity;
    }

    // ==================== Setters ====================

    public void setDomainsBeingBlocked(long domainsBeingBlocked) {
        this.domainsBeingBlocked = domainsBeingBlocked;
    }

    public void setDomains_being_blocked(long domainsBeingBlocked) {
        this.domainsBeingBlocked = domainsBeingBlocked;
    }

    public void setDnsQueriesToday(long dnsQueriesToday) {
        this.dnsQueriesToday = dnsQueriesToday;
    }

    public void setDns_queries_today(long dnsQueriesToday) {
        this.dnsQueriesToday = dnsQueriesToday;
    }

    public void setAdsBlockedToday(long adsBlockedToday) {
        this.adsBlockedToday = adsBlockedToday;
    }

    public void setAds_blocked_today(long adsBlockedToday) {
        this.adsBlockedToday = adsBlockedToday;
    }

    public void setAdsPercentageToday(double adsPercentageToday) {
        this.adsPercentageToday = adsPercentageToday;
    }

    public void setAds_percentage_today(double adsPercentageToday) {
        this.adsPercentageToday = adsPercentageToday;
    }

    public void setUniqueDomains(long uniqueDomains) {
        this.uniqueDomains = uniqueDomains;
    }

    public void setUnique_domains(long uniqueDomains) {
        this.uniqueDomains = uniqueDomains;
    }

    public void setQueriesForwarded(long queriesForwarded) {
        this.queriesForwarded = queriesForwarded;
    }

    public void setQueries_forwarded(long queriesForwarded) {
        this.queriesForwarded = queriesForwarded;
    }

    public void setQueriesCached(long queriesCached) {
        this.queriesCached = queriesCached;
    }

    public void setQueries_cached(long queriesCached) {
        this.queriesCached = queriesCached;
    }

    public void setClientsEverSeen(long clientsEverSeen) {
        this.clientsEverSeen = clientsEverSeen;
    }

    public void setClients_ever_seen(long clientsEverSeen) {
        this.clientsEverSeen = clientsEverSeen;
    }

    public void setUniqueClients(long uniqueClients) {
        this.uniqueClients = uniqueClients;
    }

    public void setUnique_clients(long uniqueClients) {
        this.uniqueClients = uniqueClients;
    }

    public void setDnsQueriesAllTypes(long dnsQueriesAllTypes) {
        this.dnsQueriesAllTypes = dnsQueriesAllTypes;
    }

    public void setDns_queries_all_types(long dnsQueriesAllTypes) {
        this.dnsQueriesAllTypes = dnsQueriesAllTypes;
    }

    public void setReplyNodata(long replyNodata) {
        this.replyNodata = replyNodata;
    }

    public void setReply_NODATA(long replyNodata) {
        this.replyNodata = replyNodata;
    }

    public void setReplyNxdomain(long replyNxdomain) {
        this.replyNxdomain = replyNxdomain;
    }

    public void setReply_NXDOMAIN(long replyNxdomain) {
        this.replyNxdomain = replyNxdomain;
    }

    public void setReplyCname(long replyCname) {
        this.replyCname = replyCname;
    }

    public void setReply_CNAME(long replyCname) {
        this.replyCname = replyCname;
    }

    public void setReplyIp(long replyIp) {
        this.replyIp = replyIp;
    }

    public void setReply_IP(long replyIp) {
        this.replyIp = replyIp;
    }

    public void setPrivacyLevel(long privacyLevel) {
        this.privacyLevel = privacyLevel;
    }

    public void setPrivacy_level(long privacyLevel) {
        this.privacyLevel = privacyLevel;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setGravity(Gravity gravity) {
        this.gravity = gravity;
    }

    // ==================== Utility Methods ====================

    /**
     * Checks if the Pi-hole is enabled and active.
     */
    public boolean isActive() {
        return STATUS_ENABLED.equals(status);
    }

    /**
     * Calculates the ads blocking percentage.
     * This is a computed value based on queries and blocked ads.
     */
    public double calculateAdsPercentage() {
        if (dnsQueriesToday <= 0) {
            return 0.0;
        }
        return (adsBlockedToday / (double) dnsQueriesToday) * 100.0;
    }

    /**
     * Gets the total processed queries (forwarded + cached).
     */
    public long getTotalProcessedQueries() {
        return queriesForwarded + queriesCached;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "PiHole[", "]")
                .add("domainsBeingBlocked=" + domainsBeingBlocked)
                .add("dnsQueriesToday=" + dnsQueriesToday)
                .add("adsBlockedToday=" + adsBlockedToday)
                .add("adsPercentageToday=" + adsPercentageToday)
                .add("uniqueDomains=" + uniqueDomains)
                .add("queriesForwarded=" + queriesForwarded)
                .add("queriesCached=" + queriesCached)
                .add("clientsEverSeen=" + clientsEverSeen)
                .add("uniqueClients=" + uniqueClients)
                .add("dnsQueriesAllTypes=" + dnsQueriesAllTypes)
                .add("replyNodata=" + replyNodata)
                .add("replyNxdomain=" + replyNxdomain)
                .add("replyCname=" + replyCname)
                .add("replyIp=" + replyIp)
                .add("privacyLevel=" + privacyLevel)
                .add("status='" + status + "'")
                .add("gravity=" + gravity)
                .toString();
    }
}
