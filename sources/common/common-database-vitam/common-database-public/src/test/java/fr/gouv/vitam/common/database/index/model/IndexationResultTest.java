/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.database.index.model;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;

public class IndexationResultTest {

    public static final String INDEX_RESULT_JSON = "indexationResult.json";

    @Test
    public void test() throws Exception {
        IndexationResult result = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(INDEX_RESULT_JSON),
            IndexationResult.class);
        Assert.assertNotNull(result);
        Assert.assertEquals("collection_name", result.getCollectionName());
        List<IndexOK> okList = result.getIndexOK();
        Assert.assertNotNull(okList);
        Assert.assertFalse(okList.isEmpty());
        Assert.assertEquals(2, okList.size());
        Assert.assertEquals("collection_name_0_date", okList.get(0).getIndexName());
        Assert.assertEquals((Integer) 0, okList.get(0).getTenant());
        Assert.assertEquals(2, okList.size());
        Assert.assertEquals("collection_name_2_date", okList.get(1).getIndexName());
        Assert.assertEquals((Integer) 2, okList.get(1).getTenant());

        List<IndexKO> koList = result.getIndexKO();
        Assert.assertNotNull(koList);
        Assert.assertFalse(koList.isEmpty());
        Assert.assertEquals(1, koList.size());
        Assert.assertEquals("collection_name_1_date", koList.get(0).getIndexName());
        Assert.assertEquals((Integer) 1, koList.get(0).getTenant());
        Assert.assertEquals("failed", koList.get(0).getMessage());
    }
}
