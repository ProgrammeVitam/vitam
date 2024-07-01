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
package fr.gouv.vitam.worker.core.plugin.evidence;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.StoreMetaDataObjectGroupActionPlugin;
import fr.gouv.vitam.worker.core.plugin.StoreMetaDataUnitActionPlugin;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * DataRectificationStep class
 */
public class DataRectificationStep extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DataRectificationStep.class);
    private static final String CORRECTIVE_AUDIT = "CORRECTIVE_AUDIT";
    private static final String CORRECT = "correct" + File.separator;
    private static final String ALTER = "alter";
    private final StoreMetaDataObjectGroupActionPlugin storeMetaDataObjectGroupActionPlugin;
    private final StoreMetaDataUnitActionPlugin storeMetaDataUnitActionPlugin;
    private final DataRectificationService dataRectificationService;

    @VisibleForTesting
    public DataRectificationStep(
        DataRectificationService dataRectificationService,
        StoreMetaDataObjectGroupActionPlugin storeMetaDataObjectGroupActionPlugin,
        StoreMetaDataUnitActionPlugin storeMetaDataUnitActionPlugin
    ) {
        this.dataRectificationService = dataRectificationService;
        this.storeMetaDataObjectGroupActionPlugin = storeMetaDataObjectGroupActionPlugin;
        this.storeMetaDataUnitActionPlugin = storeMetaDataUnitActionPlugin;
    }

    public DataRectificationStep() {
        this(
            new DataRectificationService(),
            new StoreMetaDataObjectGroupActionPlugin(),
            new StoreMetaDataUnitActionPlugin()
        );
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        ItemStatus itemStatus = new ItemStatus(CORRECTIVE_AUDIT);

        try {
            List<IdentifierType> identifierTypes = new ArrayList<>();

            File file = handler.getFileFromWorkspace(ALTER + File.separator + param.getObjectName());

            EvidenceAuditReportLine evidenceAuditReportLine = JsonHandler.getFromFile(
                file,
                EvidenceAuditReportLine.class
            );

            if (evidenceAuditReportLine.getObjectType().equals(MetadataType.OBJECTGROUP)) {
                List<IdentifierType> objectGroups = dataRectificationService.correctObjectGroups(
                    evidenceAuditReportLine,
                    param.getContainerName()
                );
                identifierTypes.addAll(objectGroups);
            }
            Optional<IdentifierType> identifierType = Optional.empty();

            if (evidenceAuditReportLine.getObjectType().equals(MetadataType.UNIT)) {
                identifierType = dataRectificationService.correctUnits(
                    evidenceAuditReportLine,
                    param.getContainerName()
                );
            }
            identifierType.ifPresent(identifierTypes::add);

            file = handler.getNewLocalFile(param.getObjectName());

            if (!identifierTypes.isEmpty()) {
                JsonHandler.writeAsFile(identifierTypes, file);
                handler.transferFileToWorkspace(CORRECT + param.getObjectName() + ".report.json", file, true, false);

                for (IdentifierType type : identifierTypes) {
                    if (
                        type.getType().equals(DataCategory.OBJECTGROUP.name()) ||
                        type.getType().equals(DataCategory.OBJECT.name())
                    ) {
                        storeMetaDataObjectGroupActionPlugin.storeDocumentsWithLfc(
                            param,
                            handler,
                            Collections.singletonList(type.getId())
                        );
                    }

                    if (type.getType().equals(DataCategory.UNIT.name())) {
                        storeMetaDataUnitActionPlugin.storeDocumentsWithLfc(
                            param,
                            handler,
                            Collections.singletonList(type.getId())
                        );
                    }
                }
            }

            itemStatus.increment(StatusCode.OK);
        } catch (IOException | VitamException e) {
            LOGGER.error(e);

            return itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(CORRECTIVE_AUDIT).setItemsStatus(CORRECTIVE_AUDIT, itemStatus);
    }
}
