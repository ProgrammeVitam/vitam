/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.storage.tapelibrary;

import java.util.Map;

public class TapeLibraryConfiguration {

    /**
     * Folder for storing incoming files
     */
    private String inputFileStorageFolder;
    /**
     * Folder for storing tar file to send to tape library
     */
    private String inputTarStorageFolder;
    /**
     * Folder for storing temp tar files during read requests from tapes.
     */
    private String tmpTarOutputStorageFolder;
    /**
     * Folder for storing tar file archived in a tape, and currently stored on disk.
     * Should be on the same FileSystem partition than {@code tmpTarOutputStorageFolder} and {@code inputTarStorageFolder} so that atomic file moves is possible.
     */
    private String cachedTarStorageFolder;

    /**
     * Max capacity that cannot be exceeded by cached tar files
     */
    private Long cachedTarMaxStorageSpaceInMB;

    /**
     * Triggers background delete of old unused cached tar files
     */
    private Long cachedTarEvictionStorageSpaceThresholdInMB;

    /**
     * Safe cache storage space level. When enough space is available, background cache delete process ends.
     */
    private Long cachedTarSafeStorageSpaceThresholdInMB;

    /**
     * Max single entry size. Must not exceed TarConstants.MAXSIZE
     */
    private long maxTarEntrySize = 1_000_000_000L;
    /**
     * Max tar file size
     */
    private long maxTarFileSize = 10_000_000_000L;

    /**
     * Execute tape library command with sudo if true.
     */
    private boolean useSudo = false;

    /**
     * Override non empty cartridges before label creation
     */
    private boolean forceOverrideNonEmptyCartridges = false;

    /**
     * File bucket & bucket configuration
     */
    private TapeLibraryTopologyConfiguration topology;

    private Map<String, TapeLibraryConf> tapeLibraries;

    public TapeLibraryTopologyConfiguration getTopology() {
        return topology;
    }

    public TapeLibraryConfiguration setTopology(TapeLibraryTopologyConfiguration topology) {
        this.topology = topology;
        return this;
    }

    public Map<String, TapeLibraryConf> getTapeLibraries() {
        return tapeLibraries;
    }

    public TapeLibraryConfiguration setTapeLibraries(
        Map<String, TapeLibraryConf> tapeLibraries) {
        this.tapeLibraries = tapeLibraries;
        return this;
    }

    public String getInputFileStorageFolder() {
        return inputFileStorageFolder;
    }

    public TapeLibraryConfiguration setInputFileStorageFolder(
        String inputFileStorageFolder) {
        this.inputFileStorageFolder = inputFileStorageFolder;
        return this;
    }

    public String getInputTarStorageFolder() {
        return inputTarStorageFolder;
    }

    public TapeLibraryConfiguration setInputTarStorageFolder(String inputTarStorageFolder) {
        this.inputTarStorageFolder = inputTarStorageFolder;
        return this;
    }

    public String getTmpTarOutputStorageFolder() {
        return tmpTarOutputStorageFolder;
    }

    public TapeLibraryConfiguration setTmpTarOutputStorageFolder(String tmpTarOutputStorageFolder) {
        this.tmpTarOutputStorageFolder = tmpTarOutputStorageFolder;
        return this;
    }

    public String getCachedTarStorageFolder() {
        return cachedTarStorageFolder;
    }

    public TapeLibraryConfiguration setCachedTarStorageFolder(
        String cachedTarStorageFolder) {
        this.cachedTarStorageFolder = cachedTarStorageFolder;
        return this;
    }

    public long getMaxTarEntrySize() {
        return maxTarEntrySize;
    }

    public TapeLibraryConfiguration setMaxTarEntrySize(long maxTarEntrySize) {
        this.maxTarEntrySize = maxTarEntrySize;
        return this;
    }

    public long getMaxTarFileSize() {
        return maxTarFileSize;
    }

    public TapeLibraryConfiguration setMaxTarFileSize(long maxTarFileSize) {
        this.maxTarFileSize = maxTarFileSize;
        return this;
    }

    public boolean isUseSudo() {
        return useSudo;
    }

    public TapeLibraryConfiguration setUseSudo(boolean useSudo) {
        this.useSudo = useSudo;
        return this;
    }

    public boolean isForceOverrideNonEmptyCartridges() {
        return forceOverrideNonEmptyCartridges;
    }

    public TapeLibraryConfiguration setForceOverrideNonEmptyCartridges(boolean forceOverrideNonEmptyCartridges) {
        this.forceOverrideNonEmptyCartridges = forceOverrideNonEmptyCartridges;
        return this;
    }

    public Long getCachedTarMaxStorageSpaceInMB() {
        return cachedTarMaxStorageSpaceInMB;
    }

    public TapeLibraryConfiguration setCachedTarMaxStorageSpaceInMB(Long cachedTarMaxStorageSpaceInMB) {
        this.cachedTarMaxStorageSpaceInMB = cachedTarMaxStorageSpaceInMB;
        return this;
    }

    public Long getCachedTarEvictionStorageSpaceThresholdInMB() {
        return cachedTarEvictionStorageSpaceThresholdInMB;
    }

    public TapeLibraryConfiguration setCachedTarEvictionStorageSpaceThresholdInMB(
        Long cachedTarEvictionStorageSpaceThresholdInMB) {
        this.cachedTarEvictionStorageSpaceThresholdInMB = cachedTarEvictionStorageSpaceThresholdInMB;
        return this;
    }

    public Long getCachedTarSafeStorageSpaceThresholdInMB() {
        return cachedTarSafeStorageSpaceThresholdInMB;
    }

    public TapeLibraryConfiguration setCachedTarSafeStorageSpaceThresholdInMB(
        Long cachedTarSafeStorageSpaceThresholdInMB) {
        this.cachedTarSafeStorageSpaceThresholdInMB = cachedTarSafeStorageSpaceThresholdInMB;
        return this;
    }
}
