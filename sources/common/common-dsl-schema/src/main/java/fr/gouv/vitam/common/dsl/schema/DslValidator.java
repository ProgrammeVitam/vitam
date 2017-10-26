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
package fr.gouv.vitam.common.dsl.schema;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.dsl.schema.meta.Schema;

/**
 * Dsl validator for DSL queries.
 * <p>
 * Provides validation for :
 * <ul>
 * <li>select multiple query dsl</li>
 * <li>select single query dsl</li>
 * <li>get by id query dsl</li>
 * <li>update by id query dsl</li>
 * </ul>
 */
public class DslValidator {

    /**
     * List of schemas
     */
    private final Map<DslSchema, Schema> schemas;

    /**
     * Constructor
     * 
     * @throws IOException thrown when a schema file is not found or invalid
     */
    public DslValidator() throws IOException {
        schemas = new HashMap<>();
        // FIXME find a way to use JsonHandler's mapper if possible
        ObjectMapper objectMapper = new ObjectMapper();
        for (DslSchema dslSchema : DslSchema.values()) {
            try (final InputStream schemaSource = PropertiesUtils.getResourceAsStream(dslSchema.getFilename())) {
                final Schema schema = Schema.load(objectMapper, schemaSource);
                schemas.put(dslSchema, schema);
            }
        }
    }

    /**
     * Validate select multiple dsl query.
     * 
     * @param dsl dsl query to validate
     * @throws IllegalArgumentException dsl empty
     * @throws ValidationException thrown if dsl is not valid
     */
    public void validateSelectSingle(JsonNode dsl) throws ValidationException {
        new Validator(schemas.get(DslSchema.SELECT_SINGLE)).validate(dsl);
    }

    /**
     * Validate select single dsl query.
     * 
     * @param dsl dsl query to validate
     * @throws IllegalArgumentException dsl empty
     * @throws ValidationException thrown if dsl is not valid
     */
    public void validateSelectMultiple(JsonNode dsl) throws ValidationException {
        new Validator(schemas.get(DslSchema.SELECT_MULTIPLE)).validate(dsl);
    }

    /**
     * Validate get by id dsl query.
     * 
     * @param dsl dsl query to validate
     * @throws IllegalArgumentException dsl empty
     * @throws ValidationException thrown if dsl is not valid
     */
    public void validateGetById(JsonNode dsl) throws ValidationException {
        new Validator(schemas.get(DslSchema.GET_BY_ID)).validate(dsl);
    }

    /**
     * Validate update by id dsl query.
     * 
     * @param dsl dsl query to validate
     * @throws IllegalArgumentException dsl empty
     * @throws ValidationException thrown if dsl is not valid
     */
    public void validateUpdateById(JsonNode dsl) throws ValidationException {
        new Validator(schemas.get(DslSchema.UPDATE_BY_ID)).validate(dsl);
    }
}
