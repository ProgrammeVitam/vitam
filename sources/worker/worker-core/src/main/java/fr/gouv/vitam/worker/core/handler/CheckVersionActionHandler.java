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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsException;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;

import java.util.Map;

/**
 * CheckVersionActionHandler handler class used to check the versions of DataObject in manifest
 */
public class CheckVersionActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckVersionActionHandler.class);
    private static final String HANDLER_ID = "CHECK_MANIFEST_DATAOBJECT_VERSION";

    private static final String SUBTASK_PDO_DATAOBJECTIONVERSION_BINARYMASTER = "PDO_DATAOBJECTIONVERSION_BINARYMASTER";
    private static final String SUBTASK_BDO_DATAOBJECTIONVERSION_PHYSICALMASTER =
        "BDO_DATAOBJECTIONVERSION_PHYSICALMASTER";
    private static final String SUBTASK_INVALID_DATAOBJECTVERSION = "INVALID_DATAOBJECTVERSION";
    private static final String SUBTASK_EMPTY_REQUIRED_FIELD = "EMPTY_REQUIRED_FIELD";
    private static final String SUBTASK_INVALIDE_ALGO = "INVALIDE_ALGO";

    private static final String BDO_CONTAINS_OTHER_TYPE = "BinaryDataObjectContainsOtherType";
    private static final String PDO_CONTAINS_OTHER_TYPE = "PhysicalDataObjectContainsOtherType";
    private static final String INCORRECT_VERSION_FORMAT = "IncorrectVersionFormat";
    private static final String INCORRECT_URI = "IncorrectUri";
    private static final String INCORRECT_PHYSICAL_ID = "IncorrectPhysicalId";
    private final SedaUtilsFactory sedaUtilsFactory;

    public CheckVersionActionHandler() {
        this(SedaUtilsFactory.getInstance());
    }

    @VisibleForTesting
    public CheckVersionActionHandler(SedaUtilsFactory sedaUtilsFactory) {
        this.sedaUtilsFactory = sedaUtilsFactory;
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) {
        checkMandatoryParameters(params);

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        final SedaUtils sedaUtils = sedaUtilsFactory.createSedaUtils(handlerIO);

        try {
            checkMandatoryIOParameter(handlerIO);
            final Map<String, Map<String, String>> versionMap = sedaUtils.checkSupportedDataObjectVersion(params);
            final Map<String, String> invalidVersionMap = versionMap.get(SedaUtils.INVALID_DATAOBJECT_VERSION);
            final Map<String, String> validVersionMap = versionMap.get(SedaUtils.VALID_DATAOBJECT_VERSION);
            if (!invalidVersionMap.isEmpty()) {
                itemStatus.increment(StatusCode.KO, invalidVersionMap.size());
                itemStatus.increment(StatusCode.OK, validVersionMap.size());

                invalidVersionMap.forEach((key, value) -> {
                    if (key.endsWith(BDO_CONTAINS_OTHER_TYPE)) {
                        updateDetailItemStatus(
                            itemStatus,
                            getMessageItemStatusUsageError(SedaConstants.TAG_BINARY_DATA_OBJECT, key, value),
                            SUBTASK_BDO_DATAOBJECTIONVERSION_PHYSICALMASTER
                        );
                    } else if (key.endsWith(PDO_CONTAINS_OTHER_TYPE)) {
                        updateDetailItemStatus(
                            itemStatus,
                            getMessageItemStatusUsageError(SedaConstants.TAG_PHYSICAL_DATA_OBJECT, key, value),
                            SUBTASK_PDO_DATAOBJECTIONVERSION_BINARYMASTER
                        );
                    } else if (key.endsWith(INCORRECT_VERSION_FORMAT)) {
                        updateDetailItemStatus(
                            itemStatus,
                            getMessageItemStatusUsageError(
                                key.contains(SedaConstants.TAG_BINARY_DATA_OBJECT)
                                    ? SedaConstants.TAG_BINARY_DATA_OBJECT
                                    : SedaConstants.TAG_PHYSICAL_DATA_OBJECT,
                                key,
                                value
                            ),
                            SUBTASK_INVALID_DATAOBJECTVERSION
                        );
                    } else if (key.endsWith(INCORRECT_URI)) {
                        updateDetailItemStatus(
                            itemStatus,
                            getMessageItemStatusUsageError(SedaConstants.TAG_BINARY_DATA_OBJECT, key, value),
                            SUBTASK_EMPTY_REQUIRED_FIELD
                        );
                    } else if (key.endsWith(INCORRECT_PHYSICAL_ID)) {
                        updateDetailItemStatus(
                            itemStatus,
                            getMessageItemStatusUsageError(SedaConstants.TAG_PHYSICAL_DATA_OBJECT, key, value),
                            SUBTASK_EMPTY_REQUIRED_FIELD
                        );
                    }
                });

                itemStatus.setData("errorNumber", invalidVersionMap.size());
            } else {
                itemStatus.increment(StatusCode.OK);
            }
        } catch (final SedaUtilsException e) {
            ObjectNode errorDetail = JsonHandler.createObjectNode();
            errorDetail.put("Error", e.getMessage());

            updateDetailItemStatus(itemStatus, JsonHandler.unprettyPrint(errorDetail), SUBTASK_INVALIDE_ALGO);

            itemStatus.increment(StatusCode.KO);
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private String getMessageItemStatusUsageError(
        final String typeDataObject,
        final String key,
        final String errorVersion
    ) {
        ObjectNode errorDetail = JsonHandler.createObjectNode();
        errorDetail.put(typeDataObject, errorVersion + (key.contains("_") ? (" - " + key.split("_")[0]) : ""));
        return JsonHandler.unprettyPrint(errorDetail);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO P0 Add Workspace:SIP/manifest.xml and check it
    }
}
