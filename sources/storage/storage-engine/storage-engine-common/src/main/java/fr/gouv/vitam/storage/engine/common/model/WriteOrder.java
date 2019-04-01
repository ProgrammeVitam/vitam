/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.storage.engine.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUIDFactory;

public class WriteOrder extends QueueMessageEntity implements ReadWriteOrder {

    public static final String BUCKET = "bucket";
    public static final String FILE_PATH = "filePath";
    public static final String SIZE = "size";
    public static final String DIGEST = "digest";
    public static final String TAR_ID = "tarId";

    @JsonProperty(BUCKET)
    private String bucket;

    @JsonProperty(FILE_PATH)
    private String filePath;

    @JsonProperty(SIZE)
    private long size;

    @JsonProperty(DIGEST)
    private String digest;

    @JsonProperty(TAR_ID)
    private String tarId;

    public WriteOrder() {
        super(GUIDFactory.newGUID().getId(), QueueMessageType.WriteOrder);
    }

    public WriteOrder(String bucket, String filePath, long size, String digest, String tarId) {
        this();
        this.bucket = bucket;
        this.filePath = filePath;
        this.size = size;
        this.digest = digest;
        this.tarId = tarId;
    }

    public String getBucket() {
        return bucket;
    }

    public WriteOrder setBucket(String bucket) {
        ParametersChecker.checkParameter("bucket is required", bucket);
        this.bucket = bucket;
        return this;
    }

    public String getFilePath() {
        return filePath;
    }

    public WriteOrder setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public long getSize() {
        return size;
    }

    public WriteOrder setSize(long size) {
        this.size = size;
        return this;
    }

    public String getDigest() {
        return digest;
    }

    public WriteOrder setDigest(String digest) {
        this.digest = digest;
        return this;
    }

    @Override
    public boolean isWriteOrder() {
        return true;
    }

    public String getTarId() {
        return tarId;
    }

    public void setTarId(String tarId) {
        this.tarId = tarId;
    }
}
