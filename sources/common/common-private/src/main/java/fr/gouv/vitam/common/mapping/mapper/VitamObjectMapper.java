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
package fr.gouv.vitam.common.mapping.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.KeyType;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.culture.archivesdefrance.seda.v2.OrganizationDescriptiveMetadataType;
import fr.gouv.culture.archivesdefrance.seda.v2.TextType;
import fr.gouv.vitam.common.mapping.deserializer.IdentifierTypeDeserializer;
import fr.gouv.vitam.common.mapping.deserializer.KeywordTypeDeserializer;
import fr.gouv.vitam.common.mapping.deserializer.LevelTypeDeserializer;
import fr.gouv.vitam.common.mapping.deserializer.OrganizationDescriptiveMetadataTypeDeserializer;
import fr.gouv.vitam.common.mapping.deserializer.TextByLangDeserializer;
import fr.gouv.vitam.common.mapping.deserializer.TextTypeDeSerializer;
import fr.gouv.vitam.common.mapping.serializer.IdentifierTypeSerializer;
import fr.gouv.vitam.common.mapping.serializer.KeywordTypeSerializer;
import fr.gouv.vitam.common.mapping.serializer.LevelTypeSerializer;
import fr.gouv.vitam.common.mapping.serializer.OrganizationDescriptiveMetadataTypeSerializer;
import fr.gouv.vitam.common.mapping.serializer.TextByLangSerializer;
import fr.gouv.vitam.common.mapping.serializer.TextTypeSerializer;
import fr.gouv.vitam.common.mapping.serializer.XMLGregorianCalendarSerializer;
import fr.gouv.vitam.common.model.unit.TextByLang;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 */
public class VitamObjectMapper {

    private static final ObjectMapper deserializationObjectMapper = new ObjectMapper();

    private static final ObjectMapper serializationObjectMapper = new ObjectMapper();

    static {
        deserializationObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
        deserializationObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        deserializationObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        deserializationObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        deserializationObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SimpleModule module = new SimpleModule();

        module.addDeserializer(TextByLang.class, new TextByLangDeserializer());
        module.addDeserializer(LevelType.class, new LevelTypeDeserializer());
        module.addDeserializer(IdentifierType.class, new IdentifierTypeDeserializer());
        module.addDeserializer(OrganizationDescriptiveMetadataType.class,
            new OrganizationDescriptiveMetadataTypeDeserializer(deserializationObjectMapper));
        module.addDeserializer(TextType.class, new TextTypeDeSerializer());
        module.addDeserializer(KeyType.class, new KeywordTypeDeserializer());

        deserializationObjectMapper.registerModule(module);
    }

    static {
        serializationObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
        serializationObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        serializationObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        serializationObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        SimpleModule module = new SimpleModule();
        module.addSerializer(TextType.class, new TextTypeSerializer());
        module.addSerializer(LevelType.class, new LevelTypeSerializer());
        module.addSerializer(IdentifierType.class, new IdentifierTypeSerializer());
        module.addSerializer(OrganizationDescriptiveMetadataType.class,
            new OrganizationDescriptiveMetadataTypeSerializer());
        module.addSerializer(XMLGregorianCalendar.class, new XMLGregorianCalendarSerializer());
        module.addSerializer(TextByLang.class, new TextByLangSerializer());
        module.addSerializer(KeyType.class, new KeywordTypeSerializer());

        serializationObjectMapper.registerModule(module);
        JavaTimeModule module1 = new JavaTimeModule();
        serializationObjectMapper.registerModule(module1);
    }

    private VitamObjectMapper() {
    }

    public static ObjectMapper buildDeserializationObjectMapper() {
        return deserializationObjectMapper;
    }

    public static ObjectMapper buildSerializationObjectMapper() {
        return serializationObjectMapper;
    }

}
