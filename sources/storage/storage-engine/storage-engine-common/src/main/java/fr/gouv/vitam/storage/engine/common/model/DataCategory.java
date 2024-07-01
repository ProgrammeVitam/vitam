/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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

/**
 * Define the differents type of "object" than can be stored, retrieve or deleted from different storage offer
 */
public enum DataCategory {
    /**
     * Archive Unit
     */
    UNIT("units", "unit", true, true),
    /**
     * Binary Object
     */
    OBJECT("objects", "object", false, true),
    /**
     * Object Group
     */
    OBJECTGROUP("objectgroups", "objectGroup", true, true),
    /**
     * Logbook (any)
     */
    LOGBOOK("logbooks", "logbook", false, false),
    /**
     * Report of operations (like ArchiveTransferReply)
     */
    REPORT("reports", "report", false, false),
    /**
     * Report of probative
     */
    PROBATIVE_REPORT("probativereports", "report", false, false),
    /**
     * Manitesf.xml from a SIP
     */
    MANIFEST("manifests", "manifest", false, false),

    /**
     * Profile xsd, rng, ...
     */
    PROFILE("profiles", "profile", false, false),

    /**
     * StorageLog (any)
     */
    STORAGELOG("storagelog", "storagelog", false, false),

    /**
     * Storage AccessLog (log)
     */
    STORAGEACCESSLOG("storageaccesslog", "storageaccesslog", false, false),

    /**
     * StorageTraceability (zip)
     */
    STORAGETRACEABILITY("storagetraceability", "storagetraceability", false, false),

    /**
     * Rules files
     */
    RULES("rules", "rules", false, false),

    /**
     * Batch update reports - Hack, to be fixed with US #5621
     */
    BATCH_REPORT("batch_report", "report", false, false),

    /**
     * Referential csv imported for rules - Hack, to be fixed with US #5621
     */
    REFERENTIAL_RULES_CSV("referential_rules_csv", "rules", false, false),

    /**
     * Referential csv imported for agencies - Hack, to be fixed with US #5621
     */
    REFERENTIAL_AGENCIES_CSV("referential_agencies_csv", "report", false, false),

    /**
     * dip collection
     *
     * @deprecated : DIP are no more stored in offers.
     */
    @Deprecated
    DIP("dip", "dip", false, true),
    /**
     * Agencies files
     */
    AGENCIES("agencies", "agencies", false, false),

    /**
     * backup files
     */
    BACKUP("backup", "backup", false, false),
    /**
     * backup operation files
     */
    BACKUP_OPERATION("backupoperations", "backup_operation", true, false),

    /**
     * Unit graph
     */
    UNIT_GRAPH("unitgraph", "unitgraph", false, true),

    /**
     * Object group graph
     */
    OBJECTGROUP_GRAPH("objectgroupgraph", "objectgroupgraph", false, true),

    /**
     * distribution_reports files
     */
    DISTRIBUTIONREPORTS("distributionreports", "distribution_reports", true, true),

    /**
     * Accession Register Detail
     */
    ACCESSION_REGISTER_DETAIL("accessionregistersdetail", "accessionregisterdetail", true, true),
    /**
     * Archival Transfer Reply
     */
    ARCHIVAL_TRANSFER_REPLY("archivaltransferreply", "archivaltransferreply", false, false),
    /**
     * Accession Register Detail
     */
    ACCESSION_REGISTER_SYMBOLIC("accessionregisterssymbolic", "accessionregistersymbolic", true, true),
    /**
     *
     */
    TMP("tmp", "tmp", true, true);

    /**
     * Collection name
     */
    private String collectionName;

    /**
     * Folder
     */
    private String folder;

    /**
     * Updatable data type information
     */
    private boolean updatable;

    /**
     * Deletable data type information
     */
    private boolean deletable;

    /**
     * Default constructor
     *
     * @param collectionName the collection name
     * @param folder the folder name for storage
     * @param udpatable true if this kind of object is updatable, false otherwise
     * @param deletable true if this kind of object is deletable, false otherwise
     */
    DataCategory(String collectionName, String folder, boolean udpatable, boolean deletable) {
        this.collectionName = collectionName;
        this.folder = folder;
        this.updatable = udpatable;
        this.deletable = deletable;
    }

    /**
     * Get collection name
     *
     * @return the collection name
     */
    public String getCollectionName() {
        return collectionName;
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
     * To know if data type is updatable
     *
     * @return true if data type is updatable, false otherwise
     */
    public boolean canUpdate() {
        return updatable;
    }

    /**
     * To know if data type is deletable
     *
     * @return true if data type is deletable, false otherwise
     */
    public boolean canDelete() {
        return deletable;
    }

    /**
     * Get DataCategory from folder
     *
     * @param folder the wanted folder
     * @return the DataCategory if exists, null otherwise
     */
    public static DataCategory getByFolder(String folder) {
        for (final DataCategory v : values()) {
            if (v.getFolder().equalsIgnoreCase(folder)) {
                return v;
            }
        }
        // TODO: IllegalArgumentException is better (as valueOf default method)
        return null;
    }

    /**
     * Get DataCategory by collection name
     *
     * @param collectionName the wanted collection name
     * @return the DataCategory if exists
     * @throws IllegalArgumentException if DataCategory does not exist
     */
    public static DataCategory getByCollectionName(String collectionName) {
        for (final DataCategory v : values()) {
            if (v.getCollectionName().equalsIgnoreCase(collectionName)) {
                return v;
            }
        }
        throw new IllegalArgumentException(collectionName + " is not a collectionName in DataCategory entry");
    }
}
