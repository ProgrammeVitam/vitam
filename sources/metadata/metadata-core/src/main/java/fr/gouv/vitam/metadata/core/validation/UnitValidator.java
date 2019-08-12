/*
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
 */
package fr.gouv.vitam.metadata.core.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.InvalidJsonSchemaException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.JsonSchemaValidationException;
import fr.gouv.vitam.common.json.JsonSchemaValidator;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileStatus;
import org.apache.commons.collections4.IteratorUtils;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

public class UnitValidator {

    public static final String JSON_SCHEMA_ARCHIVE_UNIT_SCHEMA_JSON = "/json-schema/archive-unit-schema.json";

    private static final String MANAGEMENT = "Management";

    private final CachedArchiveUnitProfileLoader archiveUnitProfileLoader;
    private final CachedSchemaValidatorLoader schemaValidatorLoader;
    private final JsonSchemaValidator builtInSchemaValidator;

    public UnitValidator(CachedArchiveUnitProfileLoader archiveUnitProfileLoader,
        CachedSchemaValidatorLoader schemaValidatorLoader) {
        this.archiveUnitProfileLoader = archiveUnitProfileLoader;
        this.schemaValidatorLoader = schemaValidatorLoader;
        this.builtInSchemaValidator = JsonSchemaValidator.forBuiltInSchema(JSON_SCHEMA_ARCHIVE_UNIT_SCHEMA_JSON);
    }

    public void validateUnit(ObjectNode unitJson)
        throws MetadataValidationException {

        validateStartAndEndDates(unitJson);

        validateInternalSchema(unitJson);

        validateArchiveUnitProfile(unitJson);
    }

    public void validateInternalSchema(ObjectNode archiveUnit)
        throws MetadataValidationException {

        try {

            ObjectNode archiveUnitWithManagement = hotfixManagementFieldForSchemaValidation(archiveUnit);

            // Validate internal / built-in unit schema
            this.builtInSchemaValidator.validateJson(archiveUnitWithManagement);

        } catch (JsonSchemaValidationException e) {
            throw new MetadataValidationException(MetadataValidationErrorCode.SCHEMA_VALIDATION_FAILURE,
                "Invalid unit format : " + e.getMessage(), e);
        }

    }

    public void validateArchiveUnitProfile(ObjectNode archiveUnit)
        throws MetadataValidationException {

        ObjectNode archiveUnitWithManagement = hotfixManagementFieldForSchemaValidation(archiveUnit);

        // Validate external schema (archive unit profile), if any
        JsonNode archiveUnitProfileNode = archiveUnitWithManagement.get(SedaConstants.TAG_ARCHIVE_UNIT_PROFILE);
        if (archiveUnitProfileNode != null) {
            String aupId = archiveUnitProfileNode.textValue();
            Optional<ArchiveUnitProfileModel> archiveUnitProfile =
                archiveUnitProfileLoader.loadArchiveUnitProfile(aupId);

            if (!archiveUnitProfile.isPresent()) {
                throw new MetadataValidationException(
                    MetadataValidationErrorCode.UNKNOWN_ARCHIVE_UNIT_PROFILE,
                    "Archive Unit Profile not found");
            }

            validateArchiveUnitProfile(archiveUnitProfile.get());

            try {
                JsonSchemaValidator externalSchemaValidator =
                    schemaValidatorLoader.loadSchemaValidator(archiveUnitProfile.get().getControlSchema());
                externalSchemaValidator.validateJson(archiveUnitWithManagement);

            } catch (JsonSchemaValidationException e) {
                throw new MetadataValidationException(
                    MetadataValidationErrorCode.ARCHIVE_UNIT_PROFILE_SCHEMA_VALIDATION_FAILURE,
                    "Archive unit profile validation failed: " + e.getMessage(), e);
            } catch (InvalidJsonSchemaException e) {
                throw new VitamRuntimeException("Invalid ArchiveUnitProfile", e);
            }
        }
    }

    private void validateArchiveUnitProfile(ArchiveUnitProfileModel archiveUnitProfile)
        throws MetadataValidationException {
        if (archiveUnitProfile.getStatus() != ArchiveUnitProfileStatus.ACTIVE) {
            throw new MetadataValidationException(
                MetadataValidationErrorCode.ARCHIVE_UNIT_PROFILE_SCHEMA_INACTIVE,
                "Unit ArchiveUnitProfile is inactive");
        }

        if (isControlSchemaEmpty(archiveUnitProfile)) {
            throw new MetadataValidationException(
                MetadataValidationErrorCode.EMPTY_ARCHIVE_UNIT_PROFILE_SCHEMA,
                "Archive unit profile does not have a controlSchema");
        }
    }

    private static boolean isControlSchemaEmpty(ArchiveUnitProfileModel archiveUnitProfile) {

        try {
            return archiveUnitProfile.getControlSchema() == null ||
                JsonHandler.isEmpty(archiveUnitProfile.getControlSchema());
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException("Invalid archive unit profile", e);
        }
    }

    @Deprecated
    private ObjectNode hotfixManagementFieldForSchemaValidation(ObjectNode archiveUnit) {

        // FIXME : Dirty hack because AUP validates internal DB schema with an ugly transformation:
        //  "_mgt" field is validated as "Management" for (bad) historical reasons

        // Soft copy
        ObjectNode archiveUnitCopy = JsonHandler.createObjectNode();
        for (String field : IteratorUtils.asIterable(archiveUnit.fieldNames())) {
            archiveUnitCopy.set(field, archiveUnit.get(field));
        }

        // Rename _mgt to Management
        JsonNode mgtNode = archiveUnitCopy.remove("_mgt");
        if (mgtNode != null) {
            archiveUnitCopy.set(MANAGEMENT, mgtNode);
        }

        return archiveUnitCopy;
    }

    public void validateStartAndEndDates(JsonNode archiveUnit) throws MetadataValidationException {

        if (archiveUnit.get(SedaConstants.TAG_RULE_START_DATE) != null &&
            archiveUnit.get(SedaConstants.TAG_RULE_END_DATE) != null) {
            final Date startDate;
            final Date endDate;
            try {
                startDate = LocalDateUtil.getDate(archiveUnit.get(SedaConstants.TAG_RULE_START_DATE).asText());
                endDate = LocalDateUtil.getDate(archiveUnit.get(SedaConstants.TAG_RULE_END_DATE).asText());
            } catch (ParseException e) {
                throw new MetadataValidationException(MetadataValidationErrorCode.INVALID_UNIT_DATE_FORMAT,
                    "Invalid unit start/end dates", e);
            }
            if (endDate.before(startDate)) {
                throw new MetadataValidationException(MetadataValidationErrorCode.INVALID_START_END_DATE,
                    "EndDate is before StartDate");
            }
        }
    }
}
