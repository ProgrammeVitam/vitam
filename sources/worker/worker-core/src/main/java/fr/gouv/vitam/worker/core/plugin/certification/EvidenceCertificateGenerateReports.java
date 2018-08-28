/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.plugin.certification;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.evidence.EvidenceService;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceAuditException;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportLine;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import static fr.gouv.vitam.common.json.JsonHandler.getFromFile;
import static fr.gouv.vitam.common.json.JsonHandler.getFromFileAsTypeRefence;
import static fr.gouv.vitam.common.json.JsonHandler.writeAsFile;
import static java.nio.charset.Charset.defaultCharset;

/**
 * EvidenceAuditGenerateReports class
 */
public class EvidenceCertificateGenerateReports extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceCertificateGenerateReports.class);

    private static final String EVIDENCE_CERTIFICATE_PREPARE_GENERATE_REPORTS =
        "EVIDENCE_CERTIFICATE_PREPARE_GENERATE_REPORTS";
    private static final String DATA = "data";
    public static final String ZIP = "zip";
    private static final String REPORTS = "reports";
    private CertificateService certificateService;

    @VisibleForTesting
    public EvidenceCertificateGenerateReports(
        CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    public EvidenceCertificateGenerateReports() {
        this(new CertificateService());
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO)
        throws ProcessingException, ContentAddressableStorageServerException {
        ItemStatus itemStatus = new ItemStatus(EVIDENCE_CERTIFICATE_PREPARE_GENERATE_REPORTS);

        try {

            File securedDataFile =
                handlerIO.getFileFromWorkspace("dataDir" + File.separator + param.getObjectName() );

            File listOfObjectByFile =
                handlerIO.getFileFromWorkspace("operation" + File.separator + param.getObjectName());

            List<String> securedLines = Files.readAllLines(securedDataFile.toPath(), defaultCharset());

            ArrayList<String> listIds =
                getFromFileAsTypeRefence(listOfObjectByFile, new TypeReference<ArrayList<String>>() {
                });

            for (String objectToAuditId : listIds) {

                File infoFromDatabase = handlerIO.getFileFromWorkspace(DATA + File.separator + objectToAuditId);

                CertificateParameters parameters =
                    getFromFile(infoFromDatabase, CertificateParameters.class);

                File file = handlerIO.getNewLocalFile(objectToAuditId);

                LifeCycleTraceabilitySecureFileObject secureFileObject =
                    EvidenceService.loadInformationFromFile(securedLines, MetadataType.OBJECTGROUP,
                        objectToAuditId);

                certificateService.checkSecuredVersion(secureFileObject, parameters);

                writeAsFile(parameters, file);

                handlerIO.transferFileToWorkspace(REPORTS + File.separator + objectToAuditId + ".report.json",
                    file, true, false);

            }

            itemStatus.increment(StatusCode.OK);

        } catch (IOException | ContentAddressableStorageNotFoundException | InvalidParseOperationException e) {

            LOGGER.error(e);

            return itemStatus.increment(StatusCode.FATAL);
        } catch (EvidenceAuditException e) {

            LOGGER.error(e);

            return itemStatus.increment(StatusCode.KO);
        }
        return new ItemStatus(EVIDENCE_CERTIFICATE_PREPARE_GENERATE_REPORTS)
            .setItemsStatus(EVIDENCE_CERTIFICATE_PREPARE_GENERATE_REPORTS, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
    }

}
