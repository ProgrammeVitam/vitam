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
package fr.gouv.vitam.common.dsl.schema;

import fr.gouv.vitam.common.dsl.schema.validator.BatchProcessingQuerySchemaValidator;
import fr.gouv.vitam.common.dsl.schema.validator.DslValidator;
import fr.gouv.vitam.common.dsl.schema.validator.GetByIdSchemaValidator;
import fr.gouv.vitam.common.dsl.schema.validator.ReclassificationQuerySchemaValidator;
import fr.gouv.vitam.common.dsl.schema.validator.SelectMultipleSchemaValidator;
import fr.gouv.vitam.common.dsl.schema.validator.SelectSingleSchemaValidator;
import fr.gouv.vitam.common.dsl.schema.validator.UpdateByIdSchemaValidator;
import fr.gouv.vitam.common.dsl.schema.validator.UpdateMultipleSchemaValidator;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.stream.StreamUtils;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Jax-rs scanner for validate Query DSL on external api Endpoint We make priority to 5000 for pass after SanityChecker
 * Filter that we have a priority of 2000
 */
@Priority(Priorities.USER)
public class DslScannerFilter implements ContainerRequestFilter {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DslScannerFilter.class);

    private DslValidator selectMultipleSchemaValidator;
    private DslValidator selectSingleSchemaValidator;
    private DslValidator getByIdSchemaValidator;
    private DslValidator updateByIdSchemaValidator;
    private DslValidator batchProcessingQuerySchemaValidator;
    private DslValidator massUpdateSchemaValidator;
    private DslValidator updateQueryReclassificationSchemaValidator;
    private DslSchema dslSchema;

    public DslScannerFilter(DslSchema dslSchema) throws IOException {
        this.dslSchema = dslSchema;
        this.selectMultipleSchemaValidator = new SelectMultipleSchemaValidator();
        this.selectSingleSchemaValidator = new SelectSingleSchemaValidator();
        this.getByIdSchemaValidator = new GetByIdSchemaValidator();
        this.updateByIdSchemaValidator = new UpdateByIdSchemaValidator();
        this.batchProcessingQuerySchemaValidator = new BatchProcessingQuerySchemaValidator();
        this.updateQueryReclassificationSchemaValidator = new ReclassificationQuerySchemaValidator();
        this.massUpdateSchemaValidator = new UpdateMultipleSchemaValidator();
    }


    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            final InputStream bodyInputStream = requestContext.getEntityStream();
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            StreamUtils.copy(bodyInputStream, bout);
            switch (this.dslSchema) {
                case SELECT_MULTIPLE:
                    selectMultipleSchemaValidator.validate(JsonHandler.getFromBytes(bout.toByteArray()));
                    break;
                case SELECT_SINGLE:
                    selectSingleSchemaValidator.validate(JsonHandler.getFromBytes(bout.toByteArray()));
                    break;
                case BATCH_PROCESSING:
                    batchProcessingQuerySchemaValidator.validate(JsonHandler.getFromBytes(bout.toByteArray()));
                    break;
                case GET_BY_ID:
                    getByIdSchemaValidator.validate(JsonHandler.getFromBytes(bout.toByteArray()));
                    break;
                case UPDATE_BY_ID:
                    updateByIdSchemaValidator.validate(JsonHandler.getFromBytes(bout.toByteArray()));
                    break;
                case RECLASSIFICATION_QUERY:
                    updateQueryReclassificationSchemaValidator.validate(JsonHandler.getFromBytes(bout.toByteArray()));
                    break;
                case MASS_UPDATE:
                    massUpdateSchemaValidator.validate(JsonHandler.getFromBytes(bout.toByteArray()));
                    break;
                default:
                    requestContext.abortWith(
                        VitamCodeHelper.toVitamError(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR,
                            "Dsl schema is not valid.")
                            .toResponse());

            }
            requestContext.setEntityStream(new ByteArrayInputStream(bout.toByteArray()));
        } catch (ValidationException e) {
            LOGGER.warn(e);
            requestContext.abortWith(
                e.getVitamError().toResponse());
        } catch (InvalidParseOperationException e) {
            LOGGER.warn(e);
            requestContext.abortWith(
                VitamCodeHelper.toVitamError(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR,
                    "Can not read Dsl query").toResponse());

        }
    }
}
