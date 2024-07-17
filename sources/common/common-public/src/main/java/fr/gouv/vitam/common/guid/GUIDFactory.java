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

import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.ServerIdentityInterface;

/**
 * GUID Factory <br>
 * <br>
 * Usage:<br>
 * One should use the appropriate helper according to the type of the object for the GUID.<br>
 * For instance: for a Unit newUnitGUID(tenantId);<br>
 * <br>
 * <b>No one should not in general use directly newGUID helpers.</b><br>
 */

public final class GUIDFactory {

    private static final ServerIdentityInterface serverIdentity = ServerIdentity.getInstance();

    /**
     * Default empty constructor
     */
    private GUIDFactory() {
        // Empty constructor
    }

    /**
     * Usable for internal GUID with default tenantId (0) and objectType (0)
     *
     * @return a new GUID
     */
    public static final GUID newGUID() {
        return new GUIDImplPrivate(0, 0, serverIdentity.getGlobalPlatformId(), false);
    }

    /**
     * Usable when a strict children GUID is to be created, therefore inherits information from parent GUID <br>
     * Keep in case in the future it could be useful.
     *
     * @param existingGUID used to get the objectType parent ({@link GUIDObjectType}), tenantId,
     * serverIdentity.getPlatformId() and Worm
     * @return a new GUID
     */
    static final GUID newChildrenGUID(final GUID existingGUID) {
        return new GUIDImplPrivate(
            GUIDObjectType.getChildrenType(existingGUID.getObjectId()),
            existingGUID.getTenantId(),
            existingGUID.getPlatformId(),
            existingGUID.isWorm()
        );
    }

    /**
     * Create a Unit GUID
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newUnitGUID(final int tenantId) {
        final int type = GUIDObjectType.UNIT_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a ObjectGroup GUID
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newObjectGroupGUID(final int tenantId) {
        final int type = GUIDObjectType.OBJECTGROUP_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a ObjectGroup GUID
     *
     * @param unitParentGUID GUID of parent Unit
     * @return a new GUID
     */
    public static final GUID newObjectGroupGUID(final GUID unitParentGUID) {
        final int type = GUIDObjectType.OBJECTGROUP_TYPE;
        return new GUIDImplPrivate(
            type,
            unitParentGUID.getTenantId(),
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a Object GUID
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newObjectGUID(final int tenantId) {
        final int type = GUIDObjectType.OBJECT_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a Object GUID
     *
     * @param objectGroupParentGUID GUID of parent ObjectGroup
     * @return a new GUID
     */
    public static final GUID newObjectGUID(final GUID objectGroupParentGUID) {
        final int type = GUIDObjectType.OBJECT_TYPE;
        return new GUIDImplPrivate(
            type,
            objectGroupParentGUID.getTenantId(),
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a Operation Logbook GUID
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newOperationLogbookGUID(final int tenantId) {
        final int type = GUIDObjectType.OPERATION_LOGBOOK_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a Write Logbook GUID
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newWriteLogbookGUID(final int tenantId) {
        final int type = GUIDObjectType.WRITE_LOGBOOK_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a Storage Operation GUID
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @param worm
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newStorageOperationGUID(final int tenantId, final boolean worm) {
        return new GUIDImplPrivate(
            GUIDObjectType.STORAGE_OPERATION_TYPE,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            worm
        );
    }

    /**
     * Create a Operation Id GUID
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newEventGUID(final int tenantId) {
        final int type = GUIDObjectType.EVENT_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create an Event GUID (within Operation or Lifecycle Logbooks)
     *
     * @param logbookGUID GUID of corresponding Logbook
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newEventGUID(final GUID logbookGUID) {
        final int type = GUIDObjectType.EVENT_TYPE;
        return new GUIDImplPrivate(
            type,
            logbookGUID.getTenantId(),
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a Request Id GUID (X-CID)
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newRequestIdGUID(final int tenantId) {
        final int type = GUIDObjectType.REQUESTID_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a Manifest GUID (SEDA)
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newManifestGUID(final int tenantId) {
        final int type = GUIDObjectType.MANIFEST_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a Accession register summary GUID
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newAccessionRegisterSummaryGUID(final int tenantId) {
        final int type = GUIDObjectType.ACCESSION_REGISTER_SUMMARY_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a Accession register summary GUID
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newAccessionRegisterSymbolicGUID(final int tenantId) {
        final int type = GUIDObjectType.ACCESSION_REGISTER_SUMMARY_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a contract GUID
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newContractGUID(final int tenantId) {
        final int type = GUIDObjectType.CONTRACT_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a Profile GUID
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newProfileGUID(final int tenantId) {
        final int type = GUIDObjectType.PROFILE_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create an ontology GUID
     *
     * @param tenantId tenant id between 0 and 2^30-1
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newOntologyGUID(final int tenantId) {
        final int type = GUIDObjectType.ONTOLOGY_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create a Context GUID
     *
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newContextGUID() {
        final int type = GUIDObjectType.CONTEXT_TYPE;
        return new GUIDImplPrivate(type, 0, serverIdentity.getGlobalPlatformId(), GUIDObjectType.getDefaultWorm(type));
    }

    /**
     * Create a Agencies GUID
     *
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newAgencyGUID(final int tenantId) {
        final int type = GUIDObjectType.AGENCY_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * Create an accession register detail GUID
     *
     * @return a new GUID
     * @throws IllegalArgumentException if any of the argument are out of range
     */
    public static final GUID newAccessionRegisterDetailGUID(int tenantId) {
        final int type = GUIDObjectType.ACCESSION_REGISTER_DETAIL_TYPE;
        return new GUIDImplPrivate(
            type,
            tenantId,
            serverIdentity.getGlobalPlatformId(),
            GUIDObjectType.getDefaultWorm(type)
        );
    }

    /**
     * @param uuid
     * @return True if the given GUID is using a WORM media
     */
    public static final boolean isWorm(final GUID uuid) {
        return uuid.isWorm();
    }

    /**
     * @return the size of the key in bytes
     */
    public static final int getKeysize() {
        return GUIDImpl.KEYSIZE;
    }

    /**
     * @return the size of the key using Base32 format in bytes
     */
    public static final int getKeysizeBase32() {
        return GUIDImpl.KEYB32SIZE;
    }
}
