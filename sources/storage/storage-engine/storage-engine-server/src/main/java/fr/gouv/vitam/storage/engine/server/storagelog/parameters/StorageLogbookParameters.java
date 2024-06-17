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
package fr.gouv.vitam.storage.engine.server.storagelog.parameters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Storage Logbook Parameters Class
 */
@JsonSerialize(using = StorageLogbookParametersSerializer.class)
public class StorageLogbookParameters implements StorageLogStructure {

    private static final String MANDATORY_PARAMETER_CAN_NOT_BE_NULL_OR_EMPTY =
        "Mandatory parameters can not be null or empty";

    private static final Set<StorageLogbookParameterName> mandatoryParametersForCreate = new HashSet<>(
        Arrays.asList(
            StorageLogbookParameterName.objectIdentifier,
            StorageLogbookParameterName.digest,
            StorageLogbookParameterName.digestAlgorithm,
            StorageLogbookParameterName.size,
            StorageLogbookParameterName.agentIdentifiers,
            StorageLogbookParameterName.dataCategory,
            StorageLogbookParameterName.eventType,
            StorageLogbookParameterName.outcome
        )
    );

    private static final Set<StorageLogbookParameterName> mandatoryParametersForDelete = new HashSet<>(
        Arrays.asList(
            StorageLogbookParameterName.objectIdentifier,
            StorageLogbookParameterName.agentIdentifiers,
            StorageLogbookParameterName.dataCategory,
            StorageLogbookParameterName.eventType,
            StorageLogbookParameterName.outcome
        )
    );

    @JsonIgnore
    private final Map<StorageLogbookParameterName, String> mapParameters = new TreeMap<>();

    /**
     * Set directly at least all mandatory parameters in the
     * StorageLogbookParameters. This constructor checks if all mandatory
     * parameters are set
     *
     * @param mapParameters The initial parameters (MUST contains mandatory parameters
     */
    private StorageLogbookParameters(Map<StorageLogbookParameterName, String> mapParameters) {
        this.mapParameters.putAll(mapParameters);
    }

    public static StorageLogbookParameters createLogParameters(
        String objectIdentifier,
        String dataCategory,
        String digest,
        String digestAlgorithm,
        String size,
        String agentIdentifiers,
        String agentIdentifierRequester,
        StorageLogbookOutcome outcome
    ) {
        final Map<StorageLogbookParameterName, String> mandatoryParameters = new TreeMap<>();
        mandatoryParameters.put(StorageLogbookParameterName.eventDateTime, LocalDateUtil.nowFormatted());
        mandatoryParameters.put(StorageLogbookParameterName.outcome, outcome.name());
        mandatoryParameters.put(StorageLogbookParameterName.objectIdentifier, objectIdentifier);
        mandatoryParameters.put(StorageLogbookParameterName.dataCategory, dataCategory);
        mandatoryParameters.put(StorageLogbookParameterName.objectGroupIdentifier, "objGId NA");
        mandatoryParameters.put(StorageLogbookParameterName.digest, digest);
        mandatoryParameters.put(StorageLogbookParameterName.digestAlgorithm, digestAlgorithm);
        mandatoryParameters.put(StorageLogbookParameterName.size, size);
        mandatoryParameters.put(StorageLogbookParameterName.eventType, "CREATE");
        mandatoryParameters.put(
            StorageLogbookParameterName.xRequestId,
            VitamThreadUtils.getVitamSession().getRequestId()
        );
        mandatoryParameters.put(StorageLogbookParameterName.agentIdentifiers, agentIdentifiers);
        mandatoryParameters.put(StorageLogbookParameterName.tenantId, ParameterHelper.getTenantParameter().toString());
        mandatoryParameters.put(StorageLogbookParameterName.agentIdentifierRequester, agentIdentifierRequester);

        return buildCreateLogParameters(mandatoryParameters);
    }

    static StorageLogbookParameters buildCreateLogParameters(Map<StorageLogbookParameterName, String> mapParameters) {
        checkMandatoryParameters(mapParameters, mandatoryParametersForCreate);
        return new StorageLogbookParameters(mapParameters);
    }

    public static StorageLogbookParameters buildDeleteLogParameters(
        Map<StorageLogbookParameterName, String> mapParameters
    ) {
        checkMandatoryParameters(mapParameters, mandatoryParametersForDelete);
        return new StorageLogbookParameters(mapParameters);
    }

    /**
     * Get the event date time as a local date time
     *
     * @return the local date time as a LocalDateTime
     */
    @JsonIgnore
    public LocalDateTime getEventDateTime() {
        final String date = mapParameters.get(StorageLogbookParameterName.eventDateTime);
        return LocalDateTime.parse(date);
    }

    /**
     * set The status of the operation
     *
     * @param outcome the outcome
     * @return the StorageLogbookParameters after the parameter has been added
     */
    @JsonIgnore
    public StorageLogbookParameters setStatus(StorageLogbookOutcome outcome) {
        mapParameters.put(StorageLogbookParameterName.outcome, outcome.name());
        return this;
    }

    /**
     * Get the status as an outcome
     *
     * @return the status as a StorageLogbookOutcome
     */
    @JsonIgnore
    public StorageLogbookOutcome getStatus() {
        final String status = mapParameters.get(StorageLogbookParameterName.outcome);
        return StorageLogbookOutcome.valueOf(status);
    }

    /**
     * Check if mandatories parameters are not empty or null
     *
     * @return true if mandatories parameters are ok
     * @throws IllegalArgumentException thrown when one parameter is empty or null
     */
    private static void checkMandatoryParameters(
        Map<StorageLogbookParameterName, String> mapParameters,
        Set<StorageLogbookParameterName> mandatoryParameters
    ) throws IllegalArgumentException {
        for (final StorageLogbookParameterName s : mandatoryParameters) {
            ParametersChecker.checkParameter(MANDATORY_PARAMETER_CAN_NOT_BE_NULL_OR_EMPTY, mapParameters.get(s));
        }
    }

    /**
     * Get the parameters map
     *
     * @return the parameters map
     */
    @JsonIgnore
    public Map<StorageLogbookParameterName, String> getMapParameters() {
        return mapParameters;
    }
}
