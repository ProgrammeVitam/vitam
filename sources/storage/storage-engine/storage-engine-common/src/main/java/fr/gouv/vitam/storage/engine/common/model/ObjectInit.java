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

import fr.gouv.vitam.common.digest.DigestType;

/**
 * Object creation data.
 */
public class ObjectInit {

    private String id;
    private long size;
    private DataCategory type;
    private DigestType digestAlgorithm;

    /**
     * Get object offer ID
     *
     * @return the object offer ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set object offer ID
     *
     * @param id
     *            the ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get object size
     *
     * @return the object size
     */
    public long getSize() {
        return size;
    }

    /**
     * Set object size
     *
     * @param size
     *            the object size
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Get object type
     *
     * @return the object type
     */
    public DataCategory getType() {
        return type;
    }

    /**
     * Set object type
     *
     * @param type
     *            the object type
     */
    public void setType(DataCategory type) {
        this.type = type;
    }

    /**
     * Get digest algorithm
     *
     * @return the digest algorithm
     */
    public DigestType getDigestAlgorithm() {
        return digestAlgorithm;
    }

    /**
     * Set digest algorithm
     *
     * @param digestAlgorithm
     *            the digest algorithm
     */
    public void setDigestAlgorithm(DigestType digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

}
