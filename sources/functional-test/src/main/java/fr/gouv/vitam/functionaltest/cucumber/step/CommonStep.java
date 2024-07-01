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
package fr.gouv.vitam.functionaltest.cucumber.step;

import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class CommonStep {

    protected final World world;

    public CommonStep(World world) {
        this.world = world;
    }

    protected void checkOperationStatus(String operationId, StatusCode... statuses) throws VitamException {
        assertThat(operationId).isNotNull();

        final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
        boolean process_timeout = vitamPoolingClient.wait(
            world.getTenantId(),
            operationId,
            ProcessState.COMPLETED,
            1800,
            1_000L,
            TimeUnit.MILLISECONDS
        );
        if (!process_timeout) {
            fail("Operation " + operationId + " timed out.");
        }

        VitamContext vitamContext = new VitamContext(world.getTenantId())
            .setAccessContract(world.getContractId())
            .setApplicationSessionId(world.getApplicationSessionId());
        RequestResponse<ItemStatus> operationProcessExecutionDetails = world
            .getAdminClient()
            .getOperationProcessExecutionDetails(vitamContext, operationId);

        assertThat(operationProcessExecutionDetails.isOk()).isTrue();

        assertThat(
            ((RequestResponseOK<ItemStatus>) operationProcessExecutionDetails).getFirstResult().getGlobalStatus()
        ).isIn((Object[]) statuses);
    }

    /**
     * runInVitamThread.
     *
     * @param
     */
    protected void runInVitamThread(MyRunnable r) {
        ExecutorService executorService = Executors.newSingleThreadExecutor(VitamThreadFactory.getInstance());
        CompletableFuture<Void> task = CompletableFuture.runAsync(
            () -> {
                try {
                    r.run();
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            },
            executorService
        ).exceptionally(e -> {
            fail("Test failed with error", e);
            return null;
        });
        task.join();
    }

    public interface MyRunnable {
        void run() throws Exception;
    }
}
