/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.io.File;

/**
 * Default base class for Data stored as Json or Xml. </br>
 * Any DTO class can use this class to extend from.
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class AbstractJsonType {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractJsonType.class);

    /**
     * Generates Json
     *
     * @return the corresponding JsonNode
     */
    public ObjectNode generateJson() {
        String value;
        try {
            value = JsonHandler.writeAsString(this);
            return (ObjectNode) JsonHandler.getFromString(value);
        } catch (final InvalidParseOperationException e) {
            LOGGER.warn("Cannot parse the Object", e);
        }
        return JsonHandler.createObjectNode();
    }

    /**
     * @return the XML string representation
     * @throws InvalidParseOperationException if parse exception occurred when writing a JsonNode
     */
    public String generateJsonString() throws InvalidParseOperationException {
        return JsonHandler.writeAsString(this);
    }

    /**
     * @param file the file to write
     * @throws InvalidParseOperationException when parse exception occurred when writing a JsonNode
     * @throws IllegalArgumentException if file null
     */
    public void writeJsonToFile(File file) throws InvalidParseOperationException {
        ParametersChecker.checkParameter("File", file);
        JsonHandler.writeAsFile(this, file);
    }

    /**
     * @param file to write
     * @return the associated object
     * @throws InvalidParseOperationException if parse exception occurred when reading file in json object
     * @throws IllegalArgumentException if file null
     */
    public static AbstractJsonType readJsonFile(File file) throws InvalidParseOperationException {
        ParametersChecker.checkParameter("File", file);
        return JsonHandler.getFromFile(file, AbstractJsonType.class);
    }

    /**
     * @param data as String to read
     * @return the associated object
     * @throws InvalidParseOperationException if parse exception occurred when reading file in json object
     * @throws IllegalArgumentException if data null
     */
    public static AbstractJsonType readJsonString(String data) throws InvalidParseOperationException {
        ParametersChecker.checkParameterNullOnly("data", data);
        return JsonHandler.getFromString(data, AbstractJsonType.class);
    }
}
