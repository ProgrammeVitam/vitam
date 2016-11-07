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
package fr.gouv.vitam.storage.engine.server.logbook;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Storage Logbook Factory
 *
 * Example of a lot addition:
 * 
 * <pre>
 * {
 *     &#64;code
 *     // Retrieves default storage logbook
 *     StorageLogbook storageLogbook = StorageLogbookFactory.getInstance().getStorageLogbook();
 *
 *     // Instantiates a new storage logbook parameters
 *     StorageLogbookParameters parameters = new StorageLogbookParameters();
 *
 *     // set properties
 *     parameters.putParameterValue(StorageLogbookParameterName.objectGroupIdentifier,
 *         StorageLogbookParameterName.objectGroupIdentifier.name()).putParameterValue(
 *             StorageLogbookParameterName.objectIdentifier, StorageLogbookParameterName.objectIdentifier.name());
 *
 *     storageLogbook.add(parameters);
 * }
 * </pre>
 */

public final class StorageLogbookFactory {

    /**
     * Default storage logbook type
     */
    private static StorageLogbookType defaultStorageLogbookType;
    private static final StorageLogbookFactory STORAGE_LOGBOOK_FACTORY = new StorageLogbookFactory();


    private StorageLogbookFactory() {
        changeDefaultStorageLogbookType(StorageLogbookType.MOCK);
    }

    /**
     * Get the StorageLogbookFactory instance
     *
     * @return the instance
     */
    public static final StorageLogbookFactory getInstance() {
        return STORAGE_LOGBOOK_FACTORY;
    }

    /**
     * Get the default type storage logbook
     *
     * @return the default storage logbook
     */
    public StorageLogbook getStorageLogbook() {
        StorageLogbook storageLogbook;
        switch (defaultStorageLogbookType) {
            case MOCK:
                storageLogbook = new StorageLogbookMock();
                break;
            default:
                throw new IllegalArgumentException("Storage Log type unknown");
        }
        return storageLogbook;
    }

    /**
     * Modify the default storage logbook type
     *
     * @param type the storage logbook type to set
     * @throws IllegalArgumentException if type null
     */
    static void changeDefaultStorageLogbookType(StorageLogbookType type) {
        ParametersChecker.checkParameter("Storage Logbook Type cannot be null", type);
        defaultStorageLogbookType = type;
    }

    /**
     * Get the default storage logbook type
     *
     * @return the default storage logbook type
     */
    public static StorageLogbookType getDefaultStorageLogbookType() {
        return defaultStorageLogbookType;
    }

    /**
     * enum to define storage logbook type
     */
    public enum StorageLogbookType {
        /**
         * To use only in MOCK
         */
        MOCK
    }
}
