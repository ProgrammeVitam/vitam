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
package fr.gouv.vitam.common.guid;

/**
 * Defines all types that could have a UUID
 */
public final class GUIDObjectType {

    /**
     * Utility Enum for GUIDObjectType
     */
    public enum GUIDObjectEnumType {
        /**
         * Unassigned type
         */
        UNASSIGNED(UNASSIGNED_TYPE),
        /**
         * Unit type
         */
        UNIT(UNIT_TYPE),
        /**
         * ObjectGroup type
         */
        OBJECTGROUP(OBJECTGROUP_TYPE),
        /**
         * Object type
         */
        OBJECT(OBJECT_TYPE),
        /**
         * Operation Logbook file type
         */
        OPERATION_LOGBOOK(OPERATION_LOGBOOK_TYPE),
        /**
         * Write Logbook file type
         */
        WRITE_LOGBOOK(WRITE_LOGBOOK_TYPE),
        /**
         * Manifest file type (if used)
         */
        MANIFEST(MANIFEST_TYPE),
        /**
         * Event type (for a task or step in a workflow)
         */
        EVENT(EVENT_TYPE),
        /**
         * Request Id type (as for Access when receiving a request)
         */
        REQUESTID(REQUESTID_TYPE),
        /**
         * Store operation Id type (when a write opeation occurs within the Write Logbook)
         */
        STORAGE_OPERATION(STORAGE_OPERATION_TYPE),
        /**
         * Store Accession register Symmary type
         */
        ACCESSION_REGISTER_SUMMARY(ACCESSION_REGISTER_SUMMARY_TYPE),
        /**
         * Store Accession register detail type
         */
        ACCESSION_REGISTER_DETAIL(ACCESSION_REGISTER_DETAIL_TYPE),

        /**
         * Contract type
         */
        CONTRACT(CONTRACT_TYPE),

        /**
         * Agencies contract type
         */
        AGENCIES(AGENCY_TYPE),

        /**
         * Ontologies type
         */
        ONTOLOGIES(ONTOLOGY_TYPE);

        final int id;

        private GUIDObjectEnumType(int id) {
            this.id = id;
        }

        /**
         * @return the corresponding GUIDObjectType id
         */
        public int getId() {
            return id;
        }
    }

    /**
     * Unassigned
     */
    public static final int UNASSIGNED_TYPE = 0;

    /**
     * Unit
     */
    public static final int UNIT_TYPE = 1;
    /**
     * Object Group
     */
    public static final int OBJECTGROUP_TYPE = 2;
    /**
     * Object
     */
    public static final int OBJECT_TYPE = 3;
    /**
     * Operation logbook
     */
    public static final int OPERATION_LOGBOOK_TYPE = 4;
    /**
     * Write Operation logbook
     */
    public static final int WRITE_LOGBOOK_TYPE = 5;
    /**
     * Ingest manifest
     */
    public static final int MANIFEST_TYPE = 6;
    /**
     * Event within a Logbook (Operation or Lifecycle Logbook)
     */
    public static final int EVENT_TYPE = 7;
    /**
     * Access Request (not associated with an operation)
     */
    public static final int REQUESTID_TYPE = 8;
    /**
     * Storage (CAS) Operation
     */
    public static final int STORAGE_OPERATION_TYPE = 9;
    /**
     * Accession register summary
     */
    public static final int ACCESSION_REGISTER_SUMMARY_TYPE = 10;

    /**
     * Entry contract
     */
    public static final int CONTRACT_TYPE = 11;

    /**
     * Profile
     */
    public static final int PROFILE_TYPE = 12;

    /**
     * Context
     */
    public static final int CONTEXT_TYPE = 13;

    /**
     * Accession register detail
     */
    public static final int ACCESSION_REGISTER_DETAIL_TYPE = 14;
    /**
     * Agency
     */
    public static final int AGENCY_TYPE = 15;

    /**
     * Ontology
     */
    public static final int ONTOLOGY_TYPE = 16;

    private GUIDObjectType() {
        // empty
    }

    /**
     * Utility method to get Enum instead of int
     *
     * @param id
     * @return the enum corresponding Id
     */
    public static GUIDObjectEnumType getEnumType(int id) {
        switch (id) {
            case UNASSIGNED_TYPE:
                return GUIDObjectEnumType.UNASSIGNED;
            case UNIT_TYPE:
                return GUIDObjectEnumType.UNIT;
            case OBJECTGROUP_TYPE:
                return GUIDObjectEnumType.OBJECTGROUP;
            case OBJECT_TYPE:
                return GUIDObjectEnumType.OBJECT;
            case OPERATION_LOGBOOK_TYPE:
                return GUIDObjectEnumType.OPERATION_LOGBOOK;
            case WRITE_LOGBOOK_TYPE:
                return GUIDObjectEnumType.WRITE_LOGBOOK;
            case MANIFEST_TYPE:
                return GUIDObjectEnumType.MANIFEST;
            case EVENT_TYPE:
                return GUIDObjectEnumType.EVENT;
            case REQUESTID_TYPE:
                return GUIDObjectEnumType.REQUESTID;
            case STORAGE_OPERATION_TYPE:
                return GUIDObjectEnumType.STORAGE_OPERATION;
            case ACCESSION_REGISTER_SUMMARY_TYPE:
                return GUIDObjectEnumType.ACCESSION_REGISTER_SUMMARY;
            case CONTRACT_TYPE:
                return GUIDObjectEnumType.CONTRACT;
            case AGENCY_TYPE:
                return GUIDObjectEnumType.AGENCIES;
            case ONTOLOGY_TYPE:
                return GUIDObjectEnumType.ONTOLOGIES;
            default:
                return GUIDObjectEnumType.UNASSIGNED;
        }
    }

    /**
     * @param type
     * @return the associated children type
     */
    public static int getChildrenType(int type) {
        switch (type) {
            case UNIT_TYPE:
                return OBJECTGROUP_TYPE;
            case OBJECTGROUP_TYPE:
                return OBJECT_TYPE;
            case OPERATION_LOGBOOK_TYPE:
            case WRITE_LOGBOOK_TYPE:
            case MANIFEST_TYPE:
            case EVENT_TYPE:
            case REQUESTID_TYPE:
            case STORAGE_OPERATION_TYPE:
            case ACCESSION_REGISTER_SUMMARY_TYPE:
            default:
                return type;
        }
    }

    /**
     * @param type
     * @return the default Worm status
     */
    public static boolean getDefaultWorm(int type) {
        switch (type) {
            case CONTRACT_TYPE:
                return false;
            case UNIT_TYPE:
            case OBJECTGROUP_TYPE:
            case STORAGE_OPERATION_TYPE:
            case ACCESSION_REGISTER_SUMMARY_TYPE:
                return false;
            case OBJECT_TYPE:
            case OPERATION_LOGBOOK_TYPE:
            case WRITE_LOGBOOK_TYPE:
            case MANIFEST_TYPE:
            case EVENT_TYPE:
            case REQUESTID_TYPE:
                return true;
            default:
                return false;
        }
    }
}
