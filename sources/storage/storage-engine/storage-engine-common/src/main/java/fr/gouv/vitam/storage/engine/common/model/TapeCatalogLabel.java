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
package fr.gouv.vitam.storage.engine.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.guid.GUIDFactory;

public class TapeCatalogLabel {

    public static final String ID = "_id";
    public static final String CODE = "code";
    public static final String ALTERNATIVE_CODE = "alternative_code";
    public static final String BUCKET = "bucket";
    public static final String TYPE = "type";
    public static final String TAG_CREATION_DATE = "creationDate";

    @JsonProperty(ID)
    private String id = GUIDFactory.newGUID().getId();

    @JsonProperty(CODE)
    private String code;

    @JsonProperty(ALTERNATIVE_CODE)
    private String alternativeCode;

    @JsonProperty(BUCKET)
    private String bucket;

    @JsonProperty(TYPE)
    private String type;

    @JsonProperty(TAG_CREATION_DATE)
    private String created = LocalDateUtil.nowFormatted();

    public TapeCatalogLabel() {}

    public TapeCatalogLabel(String id, String code) {
        this.id = id;
        this.code = code;
    }

    public String getId() {
        return id;
    }

    public TapeCatalogLabel setId(String id) {
        this.id = id;
        return this;
    }

    public String getCode() {
        return code;
    }

    public TapeCatalogLabel setCode(String code) {
        this.code = code;
        return this;
    }

    public String getAlternativeCode() {
        return alternativeCode;
    }

    public TapeCatalogLabel setAlternativeCode(String alternativeCode) {
        this.alternativeCode = alternativeCode;
        return this;
    }

    public String getBucket() {
        return bucket;
    }

    public TapeCatalogLabel setBucket(String bucket) {
        this.bucket = bucket;
        return this;
    }

    public String getType() {
        return type;
    }

    public TapeCatalogLabel setType(String type) {
        this.type = type;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public TapeCatalogLabel setCreated(String created) {
        this.created = created;
        return this;
    }
}
