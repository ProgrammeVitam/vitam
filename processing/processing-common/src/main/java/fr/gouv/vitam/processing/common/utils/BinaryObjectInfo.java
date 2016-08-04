/**
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
package fr.gouv.vitam.processing.common.utils;

import java.net.URI;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.digest.DigestType;

/**
 * The class BinaryObjectInfo is stored all information of the BinaryDataObject
 */
public final class BinaryObjectInfo {
    private String id;
    private String version;
    private URI uri;
    private String messageDigest;
    private long size;
    private DigestType algo;

    /**
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id of the binary data to set
     * @return BinaryObjectInfo
     */
    public BinaryObjectInfo setId(String id) {
        ParametersChecker.checkParameter("id is a mandatory parameter", id);
        this.id = id;
        return this;
    }

    /**
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version of the binary data to set
     * @return BinaryObjectInfo
     */
    public BinaryObjectInfo setVersion(String version) {
        ParametersChecker.checkParameter("version is a mandatory parameter", version);
        this.version = version;
        return this;
    }

    /**
     * @return uri
     */
    public URI getUri() {
        return uri;
    }

    /**
     * @param uri the uri of the binary data to set
     * @return BinaryObjectInfo
     */
    public BinaryObjectInfo setUri(URI uri) {
        ParametersChecker.checkParameter("uri is a mandatory parameter", uri);
        this.uri = uri;
        return this;
    }

    /**
     * @return messageDigest
     */
    public String getMessageDigest() {
        return messageDigest;
    }

    /**
     * @param messageDigest the message digest of the binary data to set
     * @return BinaryObjectInfo
     */
    public BinaryObjectInfo setMessageDigest(String messageDigest) {
        ParametersChecker.checkParameter("messageDigest is a mandatory parameter", messageDigest);
        this.messageDigest = messageDigest;
        return this;
    }

    /**
     * @return size
     */
    public long getSize() {
        return size;
    }

    /**
     * @param size the size of the binary data to set
     * @return BinaryObjectInfo
     */
    public BinaryObjectInfo setSize(long size) {
        this.size = size;
        return this;
    }

    /**
     * @return DigestType
     */
    public DigestType getAlgo() {
        return algo;
    }


    /**
     * @param algo digest algorithm
     * @return BinaryObjectInfo
     */
    public BinaryObjectInfo setAlgo(String algo) {
        ParametersChecker.checkParameter("algo is a mandatory parameter", algo);
        this.algo = DigestType.fromValue(algo);
        return this;
    }

}
