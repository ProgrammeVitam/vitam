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
package fr.gouv.vitam.worker.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.processing.ProcessBehavior;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.DescriptionStep;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class DescriptionStepTest {

    @Test
    public void testDescriptionStep() {
        final Step step = new Step();
        step.setStepName("StepName");
        step.setBehavior(ProcessBehavior.NOBLOCKING);
        DescriptionStep ds = new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters());
        ds.setStep(step);
        ds.setWorkParams(WorkerParametersFactory.newWorkerParameters());
        assertNotNull(ds.getWorkParams());
        assertNotNull(ds.getStep());
        assertEquals(ProcessBehavior.NOBLOCKING, ds.getStep().getBehavior());
        assertEquals("StepName", ds.getStep().getStepName());
        try {
            ds = new DescriptionStep(null, WorkerParametersFactory.newWorkerParameters());
            fail("Should have raized an exception");
        } catch (final Exception e) {
            // nothing to do
        }
        try {
            ds = new DescriptionStep(null, null);
            fail("Should have raized an exception");
        } catch (final Exception e) {
            // nothing to do
        }
    }

    @Test
    public void testDescriptionStepJson() {
        try {
            final File json = PropertiesUtils.findFile("descriptionStep.json");
            final JsonNode node = JsonHandler.getFromFile(json);
            final DescriptionStep step = JsonHandler.getFromFile(json, DescriptionStep.class);
        } catch (final Exception e) {
            e.printStackTrace();
            fail("Should not have raised an exception");
        }
    }
}
