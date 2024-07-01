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

package fr.gouv.vitam.scheduler.server.job;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.ReconstructionRequestItem;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.List;

@DisallowConcurrentExecution
public class ReconstructionAccessionRegisterJob implements Job {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionAccessionRegisterJob.class);

    private final AdminManagementClientFactory adminManagementClientFactory;

    public ReconstructionAccessionRegisterJob() {
        this(AdminManagementClientFactory.getInstance());
    }

    ReconstructionAccessionRegisterJob(AdminManagementClientFactory adminManagementClientFactory) {
        this.adminManagementClientFactory = adminManagementClientFactory;
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        final Integer adminTenant = VitamConfiguration.getAdminTenant();
        VitamThreadUtils.getVitamSession().setTenantId(adminTenant);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(adminTenant));
        try (AdminManagementClient client = this.adminManagementClientFactory.getClient()) {
            List<ReconstructionRequestItem> reconstructionItems = getReconstructionItems();

            LOGGER.info("Reconstruction accession register in progress...");
            client.reconstructAccessionRegister(reconstructionItems);
            LOGGER.info("Reconstruction accession register is finished");
        } catch (AdminManagementClientServerException e) {
            throw new JobExecutionException(" Error when reconstruction Admin  :  " + adminTenant, e);
        }
    }

    private List<ReconstructionRequestItem> getReconstructionItems() {
        List<ReconstructionRequestItem> reconstructionRequestItems = new ArrayList<>();

        VitamConfiguration.getTenants()
            .forEach(tenant -> {
                reconstructionRequestItems.add(new ReconstructionRequestItem("AccessionRegisterDetail", tenant));
                reconstructionRequestItems.add(new ReconstructionRequestItem("AccessionRegisterSymbolic", tenant));
            });

        return reconstructionRequestItems;
    }
}
