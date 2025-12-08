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
package fr.gouv.vitam.collect.internal.core.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;

public class CollectJsonMetadataLine {

    public static final TypeReference<CollectJsonMetadataLine> TYPE_REFERENCE = new TypeReference<>() {};

    public static final String FILE_FIELD = "File";
    public static final String ID_FIELD = "_id";
    public static final String OBJECT_FILES_FIELD = "ObjectFiles";
    public static final String SELECTOR_FIELD = "Selector";
    public static final String UNIT_CONTENT_FIELD = "UnitContent";

    @JsonProperty(FILE_FIELD)
    private String file;

    @JsonProperty(ID_FIELD)
    private String id;

    @JsonProperty(OBJECT_FILES_FIELD)
    private String objectFiles;

    @JsonProperty(SELECTOR_FIELD)
    private CollectJsonMetadataSelector selector;

    @JsonProperty(UNIT_CONTENT_FIELD)
    private ObjectNode unitContent;

    public CollectJsonMetadataLine() {
        // Empty constructor for serialization
    }

    public CollectJsonMetadataLine(
        String file,
        String id,
        String objectFiles,
        CollectJsonMetadataSelector selector,
        ObjectNode unitContent
    ) {
        if (StringUtils.isNotEmpty(file) && StringUtils.isNotEmpty(id)) {
            throw new IllegalArgumentException("Cannot have both parameters file and id");
        }
        this.file = file;
        this.id = id;
        this.objectFiles = objectFiles;
        this.selector = selector;
        this.unitContent = unitContent;
    }

    public String getFile() {
        return file;
    }

    public CollectJsonMetadataLine setFile(String file) {
        this.file = file;
        return this;
    }

    public String getId() {
        return id;
    }

    public CollectJsonMetadataLine setId(String id) {
        this.id = id;
        return this;
    }

    public String getObjectFiles() {
        return objectFiles;
    }

    public CollectJsonMetadataLine setObjectFiles(String objectFiles) {
        this.objectFiles = objectFiles;
        return this;
    }

    public CollectJsonMetadataSelector getSelector() {
        return selector;
    }

    public CollectJsonMetadataLine setSelector(CollectJsonMetadataSelector selector) {
        this.selector = selector;
        return this;
    }

    public ObjectNode getUnitContent() {
        return unitContent;
    }

    public CollectJsonMetadataLine setUnitContent(ObjectNode unitContent) {
        this.unitContent = unitContent;
        return this;
    }
}
