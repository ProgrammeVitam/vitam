package fr.gouv.vitam.common.json;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.XmlHandler;

/**
 * Default base class for Data stored as Json or Xml
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
public abstract class AbstractJsonType {
    /**
     * @return the corresponding JsonNode
     */
    public ObjectNode generateJson() {
        String value;
        try {
            value = JsonHandler.writeAsString(this);
            return (ObjectNode) JsonHandler.getFromString(value);
        } catch (InvalidParseOperationException e) {
        }
        return JsonHandler.createObjectNode();
    }

    /**
     * @return the XML string representation
     * @throws InvalidParseOperationException
     */
    public String generateJsonString() throws InvalidParseOperationException {
        return JsonHandler.writeAsString(this);
    }

    /**
     * @return the XML string representation
     * @throws InvalidParseOperationException
     */
    public String generateXmlString() throws InvalidParseOperationException {
        return XmlHandler.writeAsString(this);
    }

    /**
     * @param file
     * @throws InvalidParseOperationException
     */
    public void writeJsonToFile(File file) throws InvalidParseOperationException {
        JsonHandler.writeAsFile(this, file);
    }

    /**
     * @param file
     * @throws InvalidParseOperationException
     */
    public void writeXmlToFile(File file) throws InvalidParseOperationException {
        XmlHandler.writeAsFile(this, file);
    }

    /**
     * @param file
     * @return the associated object
     * @throws InvalidParseOperationException
     */
    public static AbstractJsonType readJsonFile(File file)
            throws InvalidParseOperationException {
        return (AbstractJsonType) JsonHandler.getFromFile(file, AbstractJsonType.class);
    }

    /**
     * @param file
     * @return the associated object
     * @throws InvalidParseOperationException
     */
    public static AbstractJsonType readXmlFile(File file)
            throws InvalidParseOperationException {
        return (AbstractJsonType) XmlHandler.getFromFile(file, AbstractJsonType.class);
    }

    /**
     * @param data
     * @return the associated object
     * @throws InvalidParseOperationException
     */
    public static AbstractJsonType readJsonString(String data)
            throws InvalidParseOperationException {
        return (AbstractJsonType) JsonHandler.getFromString(data, AbstractJsonType.class);
    }

    /**
     * @param data
     * @return the associated object
     * @throws InvalidParseOperationException
     */
    public static AbstractJsonType readXmlString(String data)
            throws InvalidParseOperationException {
        return (AbstractJsonType) XmlHandler.getFromString(data, AbstractJsonType.class);
    }
}
