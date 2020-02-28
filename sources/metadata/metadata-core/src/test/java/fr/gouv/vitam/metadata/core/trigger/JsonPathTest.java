/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.metadata.core.trigger;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class JsonPathTest {

    @Test
    public void testGetTargetOfPathOneLevel() {
        Assert.assertEquals("_mgt", JsonPath.getTargetOfPath("_mgt"));
    }

    @Test
    public void testGetTargetOfPathTwoLevel() {
        Assert.assertEquals("ClassificationRule", JsonPath.getTargetOfPath("_mgt.ClassificationRule"));
    }

    @Test
    public void testGetPathOneLevel() {
        Assert.assertTrue(JsonPath.getSplitPath("_mgt").isEmpty());
    }

    @Test
    public void testGetPathTwoLevel() {
        List<String> splitPath = JsonPath.getSplitPath("_mgt.ClassificationRule");
        Assert.assertEquals(1, splitPath.size());
        Assert.assertEquals("_mgt", splitPath.get(0));
    }

    @Test
    public void testGetPathThreeLevel() {
        List<String> splitPath = JsonPath.getSplitPath("_mgt.ClassificationRule.ClassificationLevel");
        Assert.assertEquals(2, splitPath.size());
        Assert.assertEquals("_mgt", splitPath.get(0));
        Assert.assertEquals("ClassificationRule", splitPath.get(1));
    }
}
