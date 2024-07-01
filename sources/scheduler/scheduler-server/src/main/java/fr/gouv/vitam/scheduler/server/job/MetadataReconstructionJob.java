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

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.model.ReconstructionRequestItem;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.List;

@DisallowConcurrentExecution
public class MetadataReconstructionJob implements Job {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataReconstructionJob.class);

    private final MetaDataClientFactory metaDataClientFactory;

    public static final String OBJECTGROUP = "OBJECTGROUP";

    public static final String UNIT = "UNIT";

    public static final String UNIT_GRAPH = "UNIT_GRAPH";

    public static final String OBJECTGROUP_GRAPH = "OBJECTGROUP_GRAPH";

    public MetadataReconstructionJob() {
        this.metaDataClientFactory = MetaDataClientFactory.getInstance();
    }

    @VisibleForTesting
    public MetadataReconstructionJob(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        final Integer adminTenant = VitamConfiguration.getAdminTenant();
        VitamThreadUtils.getVitamSession().setTenantId(adminTenant);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(adminTenant));

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            LOGGER.info("Process of reconstruction in progress...");
            List<ReconstructionRequestItem> requestItemList = new ArrayList<>();
            VitamConfiguration.getTenants()
                .forEach(tenant -> {
                    requestItemList.add(getReconstructionRequestItem(tenant, UNIT));
                    requestItemList.add(getReconstructionRequestItem(tenant, OBJECTGROUP));
                });
            requestItemList.add(getReconstructionRequestItem(VitamConfiguration.getAdminTenant(), UNIT_GRAPH));
            requestItemList.add(getReconstructionRequestItem(VitamConfiguration.getAdminTenant(), OBJECTGROUP_GRAPH));

            metaDataClient.reconstructCollection(requestItemList);
            LOGGER.info("End of process reconstruction");
        } catch (InvalidParseOperationException | MetaDataClientServerException | MetaDataNotFoundException e) {
            throw new JobExecutionException(e);
        }
    }

    private ReconstructionRequestItem getReconstructionRequestItem(Integer tenant, String collection) {
        ReconstructionRequestItem requestItem = new ReconstructionRequestItem();
        requestItem.setCollection(collection).setTenant(tenant).setLimit(VitamConfiguration.getRestoreBulkSize());
        return requestItem;
    }
}
