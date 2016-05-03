package fr.gouv.vitam.common;/*******************************************************************************
                             * This file is part of Vitam Project.
                             * 
                             * Copyright Vitam (2012, 2015)
                             *
                             * This software is governed by the CeCILL 2.1
                             * license under French law and abiding by the rules
                             * of distribution of free software. You can use,
                             * modify and/ or redistribute the software under
                             * the terms of the CeCILL license as circulated by
                             * CEA, CNRS and INRIA at the following URL
                             * "http://www.cecill.info".
                             *
                             * As a counterpart to the access to the source code
                             * and rights to copy, modify and redistribute
                             * granted by the license, users are provided only
                             * with a limited warranty and the software's
                             * author, the holder of the economic rights, and
                             * the successive licensors have only limited
                             * liability.
                             *
                             * In this respect, the user's attention is drawn to
                             * the risks associated with loading, using,
                             * modifying and/or developing or reproducing the
                             * software by the user in light of its specific
                             * status of free software, that may mean that it is
                             * complicated to manipulate, and that also
                             * therefore means that it is reserved for
                             * developers and experienced professionals having
                             * in-depth computer knowledge. Users are therefore
                             * encouraged to load and test the software's
                             * suitability as regards their requirements in
                             * conditions enabling the security of their systems
                             * and/or data to be ensured and, more generally, to
                             * use and operate it in the same conditions as
                             * regards security.
                             *
                             * The fact that you are presently reading this
                             * means that you have had knowledge of the CeCILL
                             * license and that you accept its terms.
                             *******************************************************************************/

/**
 * Defines all types that could have a UUID
 */
public class UuidObjectType {

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
     * Binary item
     */
    public static final int BINARY_TYPE = 4;
    /**
     * Unit logbook
     */
    public static final int UNIT_LOGBOOK_TYPE = 5;
    /**
     * Object Group logbook
     */
    public static final int OBJECTGROUP_LOGBOOK_TYPE = 6;
    /**
     * Operation logbook
     */
    public static final int OPERATION_LOGBOOK_TYPE = 7;
    /**
     * Write Operation logbook
     */
    public static final int WRITE_LOGBOOK_TYPE = 8;
    /**
     * Ingest manifest
     */
    public static final int MANIFEST_TYPE = 9;
    /**
     * Operation Id
     */
    public static final int OPERATIONID_TYPE = 10;
    /**
     * Access Request (not associated with an operation)
     */
    public static final int REQUESTID_TYPE = 11;
    /**
     * Tenant container
     */
    public static final int TENANT_CONTAINER_TYPE = 12;
    /**
     * Unit container
     */
    public static final int UNIT_CONTAINER_TYPE = 13;
    /**
     * ObjectGroup container
     */
    public static final int OBJECTGROUP_CONTAINER_TYPE = 14;
    /**
     * Binary Object container
     */
    public static final int BINARY_CONTAINER_TYPE = 15;
    /**
     * Unit Logbook container
     */
    public static final int UNIT_LOGBOOK_CONTAINER_TYPE = 16;
    /**
     * ObjectGroup Logbook container
     */
    public static final int OBJECTGROUP_LOGBOOK_CONTAINER_TYPE = 17;
    /**
     * Operation Logbook container
     */
    public static final int OPERATION_LOGBOOK_CONTAINER_TYPE = 18;
    /**
     * Write Logbook container
     */
    public static final int WRITE_LOGBOOK_CONTAINER_TYPE = 19;
    /**
     * Storage (CAS) Operation
     */
    public static final int STORAGE_OPERATION_TYPE = 20;
    /**
     * Fuse File
     */
    public static final int FUSE_FILE_TYPE = 21;

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
            case OBJECT_TYPE:
                return BINARY_TYPE;
            case UNIT_CONTAINER_TYPE:
                return UNIT_TYPE;
            case OBJECTGROUP_CONTAINER_TYPE:
                return OBJECTGROUP_TYPE;
            case BINARY_CONTAINER_TYPE:
                return BINARY_TYPE;
            case UNIT_LOGBOOK_CONTAINER_TYPE:
                return UNIT_LOGBOOK_TYPE;
            case OBJECTGROUP_LOGBOOK_CONTAINER_TYPE:
                return OBJECTGROUP_LOGBOOK_TYPE;
            case OPERATION_LOGBOOK_CONTAINER_TYPE:
                return OPERATION_LOGBOOK_TYPE;
            case WRITE_LOGBOOK_CONTAINER_TYPE:
                return WRITE_LOGBOOK_TYPE;
            case BINARY_TYPE:
            case UNIT_LOGBOOK_TYPE:
            case OBJECTGROUP_LOGBOOK_TYPE:
            case OPERATION_LOGBOOK_TYPE:
            case WRITE_LOGBOOK_TYPE:
            case MANIFEST_TYPE:
            case OPERATIONID_TYPE:
            case REQUESTID_TYPE:
            case TENANT_CONTAINER_TYPE:
            case STORAGE_OPERATION_TYPE:
            case FUSE_FILE_TYPE:
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
            case UNIT_TYPE:
            case OBJECTGROUP_TYPE:
            case UNIT_CONTAINER_TYPE:
            case UNIT_LOGBOOK_TYPE:
            case OBJECTGROUP_LOGBOOK_TYPE:
            case OBJECTGROUP_CONTAINER_TYPE:
            case UNIT_LOGBOOK_CONTAINER_TYPE:
            case OBJECTGROUP_LOGBOOK_CONTAINER_TYPE:
            case TENANT_CONTAINER_TYPE:
            case STORAGE_OPERATION_TYPE:
            case FUSE_FILE_TYPE:
                return false;
            case OBJECT_TYPE:
            case BINARY_TYPE:
            case BINARY_CONTAINER_TYPE:
            case OPERATION_LOGBOOK_TYPE:
            case WRITE_LOGBOOK_TYPE:
            case OPERATION_LOGBOOK_CONTAINER_TYPE:
            case WRITE_LOGBOOK_CONTAINER_TYPE:
            case MANIFEST_TYPE:
            case OPERATIONID_TYPE:
            case REQUESTID_TYPE:
                return true;
            default:
                return false;
        }
    }
}
