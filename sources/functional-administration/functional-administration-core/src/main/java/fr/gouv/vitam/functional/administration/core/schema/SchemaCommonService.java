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
package fr.gouv.vitam.functional.administration.core.schema;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaInputModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaOrigin;
import fr.gouv.vitam.functional.administration.common.VitamErrorUtils;
import fr.gouv.vitam.functional.administration.common.schema.Schema;
import fr.gouv.vitam.functional.administration.core.format.model.FunctionalOperationModel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This Common service for managing schema
 */
public class SchemaCommonService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SchemaValidationService.class);
    public static final String SCHEMA_COLLECTION = "Schema";
    private static final String SCHEMA_REPORT = "SCHEMA_REPORT";

    public static JsonNode buildDslQueryForExtractingSchema(Set<Integer> tenantIds, List<String> schemaPaths)
        throws InvalidCreateOperationException {
        if (CollectionUtils.isEmpty(tenantIds)) {
            LOGGER.error(" the tenant list should not be empty");
            throw new IllegalArgumentException("the tenant list should not be empty");
        }
        Select select = new Select();
        BooleanQuery andQuery = QueryHelper.and();
        andQuery.add(QueryHelper.eq("Origin", "EXTERNAL"));
        andQuery.add(QueryHelper.eq("Collection", "Unit"));
        andQuery.add(
            QueryHelper.in(VitamFieldsHelper.tenant(), tenantIds.stream().mapToLong(Integer::longValue).toArray())
        );
        if (!CollectionUtils.isEmpty(schemaPaths)) {
            andQuery.add(QueryHelper.in(SchemaModel.TAG_PATH, schemaPaths.toArray(String[]::new)));
        }
        select.setQuery(andQuery);
        return select.getFinalSelect();
    }

    public static String extractLeafFromPath(String schemaPath) {
        final String leafElt;
        if (schemaPath != null && schemaPath.contains(".")) {
            leafElt = StringUtils.substringAfterLast(schemaPath, ".");
        } else {
            leafElt = schemaPath;
        }
        return leafElt;
    }

    /**
     * Map Schema intput to db entity
     */
    public static List<Schema> mapSchemaFromInputParameters(
        List<SchemaInputModel> externalSchemaInputList,
        Map<String, OntologyModel> ontologyEltsMapByIdentifier
    ) {
        List<Schema> schemaModelElementsBuilt = new ArrayList<>();
        for (SchemaInputModel schemaEltInputElt : externalSchemaInputList) {
            //Common parameters
            Schema schemaDb = new Schema();
            schemaDb.setOrigin(SchemaOrigin.EXTERNAL.name());
            schemaDb.setCollection(MetadataType.UNIT.getName());
            schemaDb.setCardinality(schemaEltInputElt.getCardinality().name());
            schemaDb.setPath(schemaEltInputElt.getPath());
            String shortName = schemaEltInputElt.getShortName();
            if (StringUtils.isBlank(shortName)) {
                String leaf = SchemaCommonService.extractLeafFromPath(schemaEltInputElt.getPath());
                shortName = ontologyEltsMapByIdentifier.get(leaf).getShortName();
            }
            schemaDb.setShortName(shortName);
            schemaDb.setDescription(schemaEltInputElt.getDescription());
            schemaDb.setCreationdate(LocalDateUtil.nowFormatted());
            schemaDb.setLastupdate(LocalDateUtil.nowFormatted());
            boolean isLeaf = !Boolean.TRUE.equals(schemaEltInputElt.isObject());
            schemaDb.setIsObject(!isLeaf);
            schemaModelElementsBuilt.add(schemaDb);
        }
        return schemaModelElementsBuilt;
    }

    public static VitamError getVitamError(String vitamCode, String error, StatusCode statusCode) {
        return VitamErrorUtils.getVitamError(vitamCode, error, SCHEMA_COLLECTION, statusCode);
    }

    /**
     * generate Ok Report
     *
     * @param schemaList the list of created schema
     * @return the error report inputStream
     */

    public static SchemaImportReport fillSchemaImportReportOK(
        SchemaImportReport schemaImportReport,
        List<Schema> schemaList,
        GUID eip
    ) {
        if (schemaImportReport == null) {
            schemaImportReport = initSchemaImportReport(eip);
        }
        schemaImportReport.setStatusCode(StatusCode.OK);
        if (!CollectionUtils.isEmpty(schemaList)) {
            schemaImportReport.setCreatedSchemaPaths(
                schemaList.stream().map(schema -> schema.getPath()).collect(Collectors.toSet())
            );
        }

        return schemaImportReport;
    }

    /**
     * generate Error Report
     *
     * @param errorPathList the list of created schema
     * @return the error report inputStream
     */

    public static SchemaImportReport fillSchemaImportReportError(
        SchemaImportReport schemaImportReport,
        Set<String> errorPathList,
        StatusCode status,
        GUID eip
    ) {
        if (schemaImportReport == null) {
            schemaImportReport = initSchemaImportReport(eip);
        }
        schemaImportReport.setStatusCode(status);
        if (!CollectionUtils.isEmpty(errorPathList)) {
            schemaImportReport.setErrorSchemaPaths(errorPathList);
        }

        return schemaImportReport;
    }

    public static SchemaImportReport initSchemaImportReport(GUID eip) {
        SchemaImportReport schemaImportReport = new SchemaImportReport();
        FunctionalOperationModel operation = new FunctionalOperationModel();

        operation.setEvType(SCHEMA_REPORT);
        operation.setEvDateTime(LocalDateUtil.nowFormatted());
        operation.setEvId(eip.toString());
        return schemaImportReport;
    }

    /**
     * Build dsl query from ontology identifiers
     *
     * @param ontologyIdentifiers
     * @return
     * @throws InvalidCreateOperationException
     */
    public static JsonNode buildOntologyQueryDslByIdentifiers(Set<String> ontologyIdentifiers)
        throws InvalidCreateOperationException {
        Select select = new Select();
        select.setQuery(QueryHelper.in(OntologyModel.TAG_IDENTIFIER, ontologyIdentifiers.toArray(String[]::new)));
        return select.getFinalSelect();
    }
}
