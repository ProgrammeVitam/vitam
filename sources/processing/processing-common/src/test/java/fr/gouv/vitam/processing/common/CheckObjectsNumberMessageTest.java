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
package fr.gouv.vitam.processing.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Check ObjectsNumber Message Test
 */
public class CheckObjectsNumberMessageTest {

    private static final String COUNT_DIGITAL_OBJECT_CONSISTENT = "Conformité du nombre d'objets numériques";
    private static final String DUPLICATED_DIGITAL_OBJECT_WORKSPACE = "Objet numérique dupliqué trouvé dans le SIP: ";
    private static final String LIST_OF_UNDECLARED_DIGITAL_OBJECT = "Liste des objets numériques non déclarés: ";
    private static final String COUNT_DIGITAL_OBJECT_SIP = "Nombre d'objets numériques trouvés dans le SIP: ";
    private static final String COUNT_DIGITAL_OBJECT_MANIFEST =
        "Nombre d'objets numériques référencés dans le bordereau: ";
    private static final String NOT_FOUND_DIGITAL_OBJECT_MANIFEST =
        "Objet(s) numériques non référencé(s) dans le bordereau: ";
    private static final String NOT_FOUND_DIGITAL_OBJECT_WORKSPACE =
        "Objet(s) numériques non référencé(s) dans le SIP: ";

    @Test
    public void testCheckObjectsNumberMessage() {
        assertEquals(
            COUNT_DIGITAL_OBJECT_CONSISTENT,
            CheckObjectsNumberMessage.COUNT_DIGITAL_OBJECT_CONSISTENT.getMessage()
        );
        assertEquals(
            DUPLICATED_DIGITAL_OBJECT_WORKSPACE,
            CheckObjectsNumberMessage.DUPLICATED_DIGITAL_OBJECT_WORKSPACE.getMessage()
        );
        assertEquals(
            LIST_OF_UNDECLARED_DIGITAL_OBJECT,
            CheckObjectsNumberMessage.LIST_OF_UNDECLARED_DIGITAL_OBJECT.getMessage()
        );
        assertEquals(COUNT_DIGITAL_OBJECT_SIP, CheckObjectsNumberMessage.COUNT_DIGITAL_OBJECT_SIP.getMessage());
        assertEquals(
            COUNT_DIGITAL_OBJECT_MANIFEST,
            CheckObjectsNumberMessage.COUNT_DIGITAL_OBJECT_MANIFEST.getMessage()
        );
        assertEquals(
            NOT_FOUND_DIGITAL_OBJECT_MANIFEST,
            CheckObjectsNumberMessage.NOT_FOUND_DIGITAL_OBJECT_MANIFEST.getMessage()
        );
        assertEquals(
            NOT_FOUND_DIGITAL_OBJECT_WORKSPACE,
            CheckObjectsNumberMessage.NOT_FOUND_DIGITAL_OBJECT_WORKSPACE.getMessage()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void valueOfCheckObjectsNumberError() {
        CheckObjectsNumberMessage.valueOf("test");
    }
}
