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
package fr.gouv.vitam.functional.administration.common;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import net.javacrumbs.jsonunit.JsonAssert;
import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AccessionRegisterSummaryTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final Integer TENANT_ID = 0;

    @Test
    @RunWithCustomExecutor
    public void testConstructor() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final RegisterValueDetailModel initialValue = new RegisterValueDetailModel();
        AccessionRegisterSummary register = new AccessionRegisterSummary();
        final String id = GUIDFactory.newAccessionRegisterSummaryGUID(TENANT_ID).getId();
        register
            .setId(id)
            .setOriginatingAgency(id)
            .setObjectSize(initialValue)
            .setTotalObjectGroups(initialValue)
            .setTotalObjects(initialValue)
            .setTotalUnits(initialValue);

        assertEquals(id, register.get("_id"));
        JsonAssert.assertJsonEquals(initialValue, register.getTotalObjectGroups());
        JsonAssert.assertJsonEquals(initialValue, register.getTotalObjectSize());
        JsonAssert.assertJsonEquals(initialValue, register.getTotalUnits());
        JsonAssert.assertJsonEquals(initialValue, register.getTotalObjects());

        final InputStream stream = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("accession-register.json");
        final Map<String, Object> documentMap = JsonHandler.getMapFromInputStream(stream);
        documentMap.put("_id", id);
        register = new AccessionRegisterSummary(new Document(documentMap));
        assertEquals(1, register.getTotalObjectGroups().getIngested());
        assertEquals(1, register.getTotalObjectGroups().getRemained());
        assertEquals(0, register.getTotalObjectGroups().getDeleted());
        assertEquals(1, register.getTotalUnits().getIngested());
        assertEquals(1, register.getTotalUnits().getRemained());
        assertEquals(0, register.getTotalUnits().getDeleted());
        assertEquals(1, register.getTotalObjects().getIngested());
        assertEquals(1, register.getTotalObjects().getRemained());
        assertEquals(0, register.getTotalObjects().getDeleted());
        assertEquals(1, register.getTotalObjectSize().getIngested());
        assertEquals(1, register.getTotalObjectSize().getRemained());
        assertEquals(0, register.getTotalObjectSize().getDeleted());
    }
}
