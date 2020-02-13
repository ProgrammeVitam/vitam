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
package fr.gouv.vitam.storage.engine.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TarEntryDescription {

    @JsonProperty("tarFileId")
    private String tarFileId;

    @JsonProperty("entryName")
    private String entryName;

    @JsonProperty("startPos")
    private long startPos;

    @JsonProperty("size")
    private long size;

    @JsonProperty("digest")
    private String digestValue;

    public TarEntryDescription() {
        // Empty constructor for deserialization
    }

    public TarEntryDescription(String tarFileId, String entryName, long startPos, long size, String digestValue) {
        this.tarFileId = tarFileId;
        this.entryName = entryName;
        this.startPos = startPos;
        this.size = size;
        this.digestValue = digestValue;
    }

    public String getTarFileId() {
        return tarFileId;
    }

    public TarEntryDescription setTarFileId(String tarFileId) {
        this.tarFileId = tarFileId;
        return this;
    }

    public String getEntryName() {
        return entryName;
    }

    public TarEntryDescription setEntryName(String entryName) {
        this.entryName = entryName;
        return this;
    }

    public long getStartPos() {
        return startPos;
    }

    public TarEntryDescription setStartPos(long startPos) {
        this.startPos = startPos;
        return this;
    }

    public long getSize() {
        return size;
    }

    public TarEntryDescription setSize(long size) {
        this.size = size;
        return this;
    }

    public String getDigestValue() {
        return digestValue;
    }

    public TarEntryDescription setDigestValue(String digestValue) {
        this.digestValue = digestValue;
        return this;
    }
}
