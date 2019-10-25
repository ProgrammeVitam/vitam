/*
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
/**
 * Global Unique Identifier reader for Vitam <br>
 * <br>
 * <h3>Overriding ProcessId</h3> To override the processId, one can use the following property:<br>
 *
 * <pre>
 *  -Dfr.gouv.vitam.processId=nnnnn
 * </pre>
 *
 * Where nnnnn is a number between 0 and 2^22 (4194304). <br>
 * <br>
 * <h3>GUID Factory</h3> Usage:<br>
 * One should use the appropriate helper according to the type of the object for the GUID.<br>
 * For instance:
 * <ul>
 * <li>For a Unit and associated Unit Logbook:
 *
 * <pre>
 * GUID unitGuid = newUnitGUID(tenantId);
 * </pre>
 *
 * </li>
 * <li>For an ObjectGroup and associated ObjectGroup Logbook:
 *
 * <pre>
 * GUID objectGroupGuid = newObjectGroupGUID(tenantId);
 * or
 * GUID objectGroupGuid = newObjectGroupGUID(unitParentGUID);
 * </pre>
 *
 * </li>
 * <li>For an Object and associated Binary object:
 *
 * <pre>
 * GUID objectGuid = newObjectGUID(tenantId);
 * or
 * GUID objectGuid = newObjectGUID(objectGroupParentGUID);
 * </pre>
 *
 * </li>
 * <li>For an Operation (process):
 *
 * <pre>
 * GUID operationGuid = newOperationIdGUID(tenantId);
 * </pre>
 *
 * </li>
 * <li>For a Request Id:
 *
 * <pre>
 * GUID requestIdGuid = newRequestIdGUID(tenantId);
 * </pre>
 *
 * </li></li>
 * <li>For a SIP / Manifest / Seda like informations Id:
 *
 * <pre>
 * GUID manifestGuid = newManifestGUID(tenantId);
 * </pre>
 *
 * </li>
 * <li>For an Logbook daily Id (Operation, Write):
 *
 * <pre>
 * GUID writeLogbookGuid = newWriteLogbookGUID(tenantId);
 * </pre>
 *
 * </li>
 * <li>For storage operation Id:
 *
 * <pre>
 * GUID storageOperationGuid = newStorageOperationGUID(tenantId);
 * </pre>
 *
 * </li>
 * </ul>
 * <h3>Attention</h3> <b>No one should not in general use directly newUuid helpers.</b><br>
 * Those methods are for special unknown yet usages.
 */
package fr.gouv.vitam.common.guid;
