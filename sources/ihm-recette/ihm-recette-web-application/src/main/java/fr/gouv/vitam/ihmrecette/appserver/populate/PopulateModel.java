/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ihmrecette.appserver.populate;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PopulateModel {

    private int numberOfUnit;

    private int bulkSize;

    private String rootId;

    private String sp;

    private int tenant;

    private boolean indexInEs = true;
    
    private boolean storeInDb = true;
    
    private boolean withGots = true;

    private boolean withLFCUnits = true;

    private boolean withLFCGots = true;

    /**
     * The number of events to generate per LFCGot
     */
    @JsonProperty("LFCGotsEventsSize")
    private int LFCGotsEventsSize=5;

    /**
     * The number of events to generate per LFCUnit
     */
    @JsonProperty("LFCUnitsEventsSize")
    private int LFCUnitsEventsSize=5;

    @JsonProperty("objectSize")
    private Integer objectSize;

    @JsonProperty("ruleTemplatePercent")
    private Map<String, Integer> ruleTemplatePercent;

    private boolean withRules;

    private boolean storeMetadataAndLfcInOffers = false;

    public int getNumberOfUnit() {
        return numberOfUnit;
    }

    public void setNumberOfUnit(int numberOfUnit) {
        this.numberOfUnit = numberOfUnit;
    }

    public int getBulkSize() {
        return bulkSize;
    }

    public void setBulkSize(int bulkSize) {
        this.bulkSize = bulkSize;
    }

    public String getRootId() {
        return rootId;
    }

    public void setRootId(String rootId) {
        this.rootId = rootId;
    }

    public String getSp() {
        return sp;
    }

    public void setSp(String sp) {
        this.sp = sp;
    }

    public int getTenant() {
        return tenant;
    }

    public void setTenant(int tenant) {
        this.tenant = tenant;
    }

    public boolean isIndexInEs() {
        return indexInEs;
    }

    public void setIndexInEs(boolean indexInEs) {
        this.indexInEs = indexInEs;
    }

    public boolean isStoreInDb() {
        return storeInDb;
    }

    public void setStoreInDb(boolean storeInDb) {
        this.storeInDb = storeInDb;
    }

    public boolean isWithGots() {
        return withGots;
    }

    public void setWithGots(boolean withGots) {
        this.withGots = withGots;
    }

    public boolean isWithLFCUnits() {
        return withLFCUnits;
    }

    public void setWithLFCUnits(boolean withLFCUnits) {
        this.withLFCUnits = withLFCUnits;
    }

    public boolean isWithLFCGots() {
        return withLFCGots;
    }

    public void setWithLFCGots(boolean withLFCGots) {
        this.withLFCGots = withLFCGots;
    }

    public boolean isWithRules() {
        return withRules;
    }

    public void setWithRules(boolean withRules) {
        this.withRules = withRules;
    }

    public Map<String, Integer> getRuleTemplatePercent() {
        return ruleTemplatePercent;
    }

    public void setRuleTemplatePercent(Map<String, Integer> ruleTemplatePercent) {
        this.ruleTemplatePercent = ruleTemplatePercent;
    }

    public Integer getObjectSize() {
        return objectSize != null ? objectSize : 0;
    }

    public void setObjectSize(Integer objectSize) {
        this.objectSize = objectSize;
    }

    public int getLFCGotsEventsSize() {
        return LFCGotsEventsSize;
    }

    public void setLFCGotsEventsSize(int LFCGotsEventsSize) {
        this.LFCGotsEventsSize = LFCGotsEventsSize;
    }

    public int getLFCUnitsEventsSize() {
        return LFCUnitsEventsSize;
    }

    public void setLFCUnitsEventsSize(int LFCUnitsEventsSize) {
        this.LFCUnitsEventsSize = LFCUnitsEventsSize;
    }

    public boolean isStoreMetadataAndLfcInOffers() {
        return storeMetadataAndLfcInOffers;
    }

    public void setStoreMetadataAndLfcInOffers(boolean storeMetadataAndLfcInOffers) {
        this.storeMetadataAndLfcInOffers = storeMetadataAndLfcInOffers;
    }
}
