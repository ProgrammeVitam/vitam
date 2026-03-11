/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.cold;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;

import java.util.Collections;
import java.util.List;

/**
 * Configuration for INA Tape Proxy Service
 * Extends DbConfigurationImpl to support MongoDB configuration
 */
public class InaTapeProxyConfiguration extends DbConfigurationImpl {

    @JsonProperty("nbDrives")
    private int nbDrives = 1;

    @JsonProperty("nbSlots")
    private int nbSlots = 15;

    @JsonProperty("tapeCodes")
    private List<String> tapeCodes = Collections.emptyList();

    // ========== TAPE CAPACITY CONFIGURATION ==========

    @JsonProperty("tapeMaxCapacityMB")
    private int tapeMaxCapacityMB = 1_000_000; // 1 TB default

    // ========== FILESYSTEM CONFIGURATION ==========

    @JsonProperty("exchangeDirectory")
    private String exchangeDirectory;

    @JsonProperty("inaStorageDirectory")
    private String inaStorageDirectory;

    @JsonProperty("offerStorageDirectory")
    private String offerStorageDirectory;

    // ========== LATENCIES CONFIGURATION (milliseconds) ==========

    @JsonProperty("writeLatencyMs")
    private long writeLatencyMs = 30000; // 30 seconds

    @JsonProperty("readLatencyMs")
    private long readLatencyMs = 60000; // 60 seconds

    @JsonProperty("readTimeout")
    private long readTimeout = 300000; // 5*60 seconds

    @JsonProperty("loadLatencyMs")
    private long loadLatencyMs = 20000; // 20 seconds

    @JsonProperty("unloadLatencyMs")
    private long unloadLatencyMs = 20000; // 20 seconds

    @JsonProperty("ejectLatencyMs")
    private long ejectLatencyMs = 20000; // 20 seconds

    @JsonProperty("roboticLatencyMs")
    private long roboticLatencyMs = 20000; // 20 seconds (move, rewind, goto...)

    // ========== STATUS CONFIGURATION (static values) ==========

    @JsonProperty("libraryName")
    private String libraryName = "INA_LIBRARY";

    public InaTapeProxyConfiguration() {}

    // Getters and Setters

    public int getNbDrives() {
        return nbDrives;
    }

    public int getNbSlots() {
        return nbSlots;
    }

    public List<String> getTapeCodes() {
        return tapeCodes;
    }

    public int getTapeMaxCapacityMB() {
        return tapeMaxCapacityMB;
    }

    // ========== GETTERS & SETTERS ==========
    public InaTapeProxyConfiguration setNbDrives(int nbDrives) {
        this.nbDrives = nbDrives;
        return this;
    }

    public InaTapeProxyConfiguration setNbSlots(int nbSlots) {
        this.nbSlots = nbSlots;
        return this;
    }

    public InaTapeProxyConfiguration setTapeCodes(List<String> tapeCodes) {
        this.tapeCodes = tapeCodes;
        return this;
    }

    public InaTapeProxyConfiguration setTapeMaxCapacityMB(int tapeMaxCapacityMB) {
        this.tapeMaxCapacityMB = tapeMaxCapacityMB;
        return this;
    }

    public String getExchangeDirectory() {
        return exchangeDirectory;
    }

    public InaTapeProxyConfiguration setExchangeDirectory(String exchangeDirectory) {
        this.exchangeDirectory = exchangeDirectory;
        return this;
    }

    public String getInaStorageDirectory() {
        return inaStorageDirectory;
    }

    public InaTapeProxyConfiguration setInaStorageDirectory(String inaStorageDirectory) {
        this.inaStorageDirectory = inaStorageDirectory;
        return this;
    }

    public String getOfferStorageDirectory() {
        return offerStorageDirectory;
    }

    public InaTapeProxyConfiguration setOfferStorageDirectory(String offerStorageDirectory) {
        this.offerStorageDirectory = offerStorageDirectory;
        return this;
    }

    public long getWriteLatencyMs() {
        return writeLatencyMs;
    }

    public InaTapeProxyConfiguration setWriteLatencyMs(long writeLatencyMs) {
        this.writeLatencyMs = writeLatencyMs;
        return this;
    }

    public long getReadLatencyMs() {
        return readLatencyMs;
    }

    public InaTapeProxyConfiguration setReadLatencyMs(long readLatencyMs) {
        this.readLatencyMs = readLatencyMs;
        return this;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public InaTapeProxyConfiguration setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public long getLoadLatencyMs() {
        return loadLatencyMs;
    }

    public InaTapeProxyConfiguration setLoadLatencyMs(long loadLatencyMs) {
        this.loadLatencyMs = loadLatencyMs;
        return this;
    }

    public long getUnloadLatencyMs() {
        return unloadLatencyMs;
    }

    public InaTapeProxyConfiguration setUnloadLatencyMs(long unloadLatencyMs) {
        this.unloadLatencyMs = unloadLatencyMs;
        return this;
    }

    public long getEjectLatencyMs() {
        return ejectLatencyMs;
    }

    public InaTapeProxyConfiguration setEjectLatencyMs(long ejectLatencyMs) {
        this.ejectLatencyMs = ejectLatencyMs;
        return this;
    }

    public long getRoboticLatencyMs() {
        return roboticLatencyMs;
    }

    public InaTapeProxyConfiguration setRoboticLatencyMs(long roboticLatencyMs) {
        this.roboticLatencyMs = roboticLatencyMs;
        return this;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public InaTapeProxyConfiguration setLibraryName(String libraryName) {
        this.libraryName = libraryName;
        return this;
    }
}
