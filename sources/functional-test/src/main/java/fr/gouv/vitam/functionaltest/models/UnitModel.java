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

package fr.gouv.vitam.functionaltest.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UnitModel {
    @JsonProperty("ArchiveUnit")
    List<UnitModel> archiveUnit;

    @JsonProperty("Content")
    Map<String, JsonNode> content = new HashMap<>();

    @JsonIgnore
    public static final Comparator<UnitModel> UNIT_MODEL_COMPARATOR = (o1, o2) -> CharSequence.compare(
        Objects.requireNonNullElse(o1.getContent().get(VitamFieldsHelper.id()), TextNode.valueOf("")).asText(),
        Objects.requireNonNullElse(o2.getContent().get(VitamFieldsHelper.id()), TextNode.valueOf("")).asText());


    public UnitModel(List<UnitModel> archiveUnit, Map<String, JsonNode> content) {
        if (Objects.nonNull(archiveUnit)) {
            archiveUnit.sort(UNIT_MODEL_COMPARATOR);
        }
        this.archiveUnit = archiveUnit;
        this.content = content;
    }

    public UnitModel() {
    }

    public List<UnitModel> getArchiveUnit() {
        return archiveUnit;
    }

    public void setArchiveUnit(List<UnitModel> archiveUnit) {
        if (Objects.nonNull(archiveUnit)) {
            archiveUnit.sort(UNIT_MODEL_COMPARATOR);
        }
        this.archiveUnit = archiveUnit;
    }

    public Map<String, JsonNode> getContent() {
        return content;
    }

    public void setContent(Map<String, JsonNode> content) {
        this.content = content;
    }
}
