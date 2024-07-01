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
package fr.gouv.vitam.common.guid;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class GUIDFactoryTest {

    @Test
    public final void testNewGUID() {
        assertEquals(0, GUIDFactory.newGUID().getObjectId());
        assertEquals(
            GUIDObjectType.OBJECTGROUP_TYPE,
            GUIDFactory.newChildrenGUID(GUIDFactory.newUnitGUID(1)).getObjectId()
        );
        final GUID unit = GUIDFactory.newUnitGUID(0);
        assertEquals(GUIDObjectType.UNIT_TYPE, unit.getObjectId());
        final GUID og = GUIDFactory.newObjectGroupGUID(0);
        assertEquals(GUIDObjectType.OBJECTGROUP_TYPE, og.getObjectId());
        assertEquals(GUIDObjectType.OBJECTGROUP_TYPE, GUIDFactory.newObjectGroupGUID(unit).getObjectId());
        assertEquals(GUIDObjectType.OBJECT_TYPE, GUIDFactory.newObjectGUID(0).getObjectId());
        assertEquals(GUIDObjectType.OBJECT_TYPE, GUIDFactory.newObjectGUID(og).getObjectId());
        assertEquals(GUIDObjectType.OPERATION_LOGBOOK_TYPE, GUIDFactory.newOperationLogbookGUID(0).getObjectId());
        assertEquals(GUIDObjectType.WRITE_LOGBOOK_TYPE, GUIDFactory.newWriteLogbookGUID(0).getObjectId());
        assertEquals(GUIDObjectType.STORAGE_OPERATION_TYPE, GUIDFactory.newStorageOperationGUID(0, true).getObjectId());
        assertEquals(GUIDObjectType.EVENT_TYPE, GUIDFactory.newEventGUID(0).getObjectId());
        assertEquals(GUIDObjectType.REQUESTID_TYPE, GUIDFactory.newRequestIdGUID(0).getObjectId());
        assertEquals(GUIDObjectType.MANIFEST_TYPE, GUIDFactory.newManifestGUID(0).getObjectId());
        assertEquals(GUIDObjectType.EVENT_TYPE, GUIDFactory.newEventGUID(0).getObjectId());
        assertEquals(
            GUIDObjectType.ACCESSION_REGISTER_SUMMARY_TYPE,
            GUIDFactory.newAccessionRegisterSummaryGUID(0).getObjectId()
        );

        assertEquals(true, GUIDFactory.isWorm(GUIDFactory.newStorageOperationGUID(0, true)));
        assertEquals(GUIDImpl.getKeySize(), GUIDFactory.getKeysize());
        assertEquals(GUIDImpl.KEYB32SIZE, GUIDFactory.getKeysizeBase32());
    }

    @Test
    public final void testNewUnitGUID() {
        final GUID guid = GUIDFactory.newUnitGUID(1);
        assertEquals(GUIDObjectType.UNIT_TYPE, guid.getObjectId());
        assertEquals(1, guid.getTenantId());
        assertFalse(guid.isWorm());
    }

    @Test
    public void testGUIDObjectType() {
        assertEquals(GUIDObjectType.OBJECTGROUP_TYPE, GUIDObjectType.getChildrenType(GUIDObjectType.UNIT_TYPE));
        assertEquals(GUIDObjectType.OBJECT_TYPE, GUIDObjectType.getChildrenType(GUIDObjectType.OBJECTGROUP_TYPE));
        assertEquals(
            GUIDObjectType.OPERATION_LOGBOOK_TYPE,
            GUIDObjectType.getChildrenType(GUIDObjectType.OPERATION_LOGBOOK_TYPE)
        );
        assertEquals(
            GUIDObjectType.WRITE_LOGBOOK_TYPE,
            GUIDObjectType.getChildrenType(GUIDObjectType.WRITE_LOGBOOK_TYPE)
        );
        assertEquals(GUIDObjectType.MANIFEST_TYPE, GUIDObjectType.getChildrenType(GUIDObjectType.MANIFEST_TYPE));
        assertEquals(GUIDObjectType.EVENT_TYPE, GUIDObjectType.getChildrenType(GUIDObjectType.EVENT_TYPE));
        assertEquals(GUIDObjectType.REQUESTID_TYPE, GUIDObjectType.getChildrenType(GUIDObjectType.REQUESTID_TYPE));
        assertEquals(
            GUIDObjectType.STORAGE_OPERATION_TYPE,
            GUIDObjectType.getChildrenType(GUIDObjectType.STORAGE_OPERATION_TYPE)
        );
        assertEquals(GUIDObjectType.UNASSIGNED_TYPE, GUIDObjectType.getChildrenType(GUIDObjectType.UNASSIGNED_TYPE));
        assertEquals(GUIDObjectType.CONTRACT_TYPE, GUIDObjectType.getChildrenType(GUIDObjectType.CONTRACT_TYPE));

        assertEquals(false, GUIDObjectType.getDefaultWorm(GUIDObjectType.UNIT_TYPE));
        assertEquals(false, GUIDObjectType.getDefaultWorm(GUIDObjectType.OBJECTGROUP_TYPE));
        assertEquals(true, GUIDObjectType.getDefaultWorm(GUIDObjectType.OBJECT_TYPE));
        assertEquals(true, GUIDObjectType.getDefaultWorm(GUIDObjectType.OPERATION_LOGBOOK_TYPE));
        assertEquals(true, GUIDObjectType.getDefaultWorm(GUIDObjectType.WRITE_LOGBOOK_TYPE));
        assertEquals(true, GUIDObjectType.getDefaultWorm(GUIDObjectType.MANIFEST_TYPE));
        assertEquals(true, GUIDObjectType.getDefaultWorm(GUIDObjectType.EVENT_TYPE));
        assertEquals(true, GUIDObjectType.getDefaultWorm(GUIDObjectType.REQUESTID_TYPE));
        assertEquals(false, GUIDObjectType.getDefaultWorm(GUIDObjectType.STORAGE_OPERATION_TYPE));
        assertEquals(false, GUIDObjectType.getDefaultWorm(GUIDObjectType.UNASSIGNED_TYPE));
        assertEquals(false, GUIDObjectType.getDefaultWorm(GUIDObjectType.CONTRACT_TYPE));

        assertEquals(GUIDObjectType.GUIDObjectEnumType.UNIT, GUIDObjectType.getEnumType(GUIDObjectType.UNIT_TYPE));
        assertEquals(
            GUIDObjectType.GUIDObjectEnumType.OBJECTGROUP,
            GUIDObjectType.getEnumType(GUIDObjectType.OBJECTGROUP_TYPE)
        );
        assertEquals(GUIDObjectType.GUIDObjectEnumType.OBJECT, GUIDObjectType.getEnumType(GUIDObjectType.OBJECT_TYPE));
        assertEquals(
            GUIDObjectType.GUIDObjectEnumType.OPERATION_LOGBOOK,
            GUIDObjectType.getEnumType(GUIDObjectType.OPERATION_LOGBOOK_TYPE)
        );
        assertEquals(
            GUIDObjectType.GUIDObjectEnumType.WRITE_LOGBOOK,
            GUIDObjectType.getEnumType(GUIDObjectType.WRITE_LOGBOOK_TYPE)
        );
        assertEquals(
            GUIDObjectType.GUIDObjectEnumType.MANIFEST,
            GUIDObjectType.getEnumType(GUIDObjectType.MANIFEST_TYPE)
        );
        assertEquals(GUIDObjectType.GUIDObjectEnumType.EVENT, GUIDObjectType.getEnumType(GUIDObjectType.EVENT_TYPE));
        assertEquals(
            GUIDObjectType.GUIDObjectEnumType.REQUESTID,
            GUIDObjectType.getEnumType(GUIDObjectType.REQUESTID_TYPE)
        );
        assertEquals(
            GUIDObjectType.GUIDObjectEnumType.STORAGE_OPERATION,
            GUIDObjectType.getEnumType(GUIDObjectType.STORAGE_OPERATION_TYPE)
        );
        assertEquals(
            GUIDObjectType.GUIDObjectEnumType.UNASSIGNED,
            GUIDObjectType.getEnumType(GUIDObjectType.UNASSIGNED_TYPE)
        );
        assertEquals(
            GUIDObjectType.GUIDObjectEnumType.CONTRACT,
            GUIDObjectType.getEnumType(GUIDObjectType.CONTRACT_TYPE)
        );

        assertEquals(GUIDObjectType.GUIDObjectEnumType.UNIT.getId(), GUIDObjectType.UNIT_TYPE);
        assertEquals(GUIDObjectType.GUIDObjectEnumType.OBJECTGROUP.getId(), GUIDObjectType.OBJECTGROUP_TYPE);
        assertEquals(GUIDObjectType.GUIDObjectEnumType.OBJECT.getId(), GUIDObjectType.OBJECT_TYPE);
        assertEquals(
            GUIDObjectType.GUIDObjectEnumType.OPERATION_LOGBOOK.getId(),
            GUIDObjectType.OPERATION_LOGBOOK_TYPE
        );
        assertEquals(GUIDObjectType.GUIDObjectEnumType.WRITE_LOGBOOK.getId(), GUIDObjectType.WRITE_LOGBOOK_TYPE);
        assertEquals(GUIDObjectType.GUIDObjectEnumType.MANIFEST.getId(), GUIDObjectType.MANIFEST_TYPE);
        assertEquals(GUIDObjectType.GUIDObjectEnumType.EVENT.getId(), GUIDObjectType.EVENT_TYPE);
        assertEquals(GUIDObjectType.GUIDObjectEnumType.REQUESTID.getId(), GUIDObjectType.REQUESTID_TYPE);
        assertEquals(
            GUIDObjectType.GUIDObjectEnumType.STORAGE_OPERATION.getId(),
            GUIDObjectType.STORAGE_OPERATION_TYPE
        );
        assertEquals(GUIDObjectType.GUIDObjectEnumType.UNASSIGNED.getId(), GUIDObjectType.UNASSIGNED_TYPE);
        assertEquals(GUIDObjectType.GUIDObjectEnumType.CONTRACT.getId(), GUIDObjectType.CONTRACT_TYPE);
    }
}
