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

/**
 * Define the differents type of "object" than can be stored, retrieve or deleted from different storage offer
 */
public enum DataCategory {
    /**
     * Archive Unit
     */
    UNIT("unit"), 
    /**
     * Binary Object
     */
    OBJECT("object"),
    /**
     * Object Group
     */
    OBJECT_GROUP("objectGroup"), 
    /**
     * Logbook (any)
     */
    LOGBOOK("logbook"),
    /**
     * Report of operations (like ArchiveTransferReply)
     */
    REPORT("report"),
    /**
     * Manitesf.xml from a SIP
     */
    MANIFEST("manifest");

    /**
     * Folder
     */
    private String folder;

    /**
     * Constructor.
     *
     * @param folder folder
     */
    private DataCategory(String folder) {
        this.folder = folder;
    }

    /**
     * Gets the folder
     *
     * @return the folder
     */
    public String getFolder() {
        return folder;
    }


    /**
     * Get DataCategory from folder
     *
     * @param folder the wanted folder
     * @return the DataCategory if exists, null otherwise
     */
    public static DataCategory getByFolder(String folder) {
        for (final DataCategory v : values()) {
            if (v.getFolder().equals(folder)) {
                return v;
            }
        }
        return null;
    }
}
