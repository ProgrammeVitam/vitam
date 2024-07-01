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
package fr.gouv.vitam.functional.administration.common.server;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import org.bson.Document;

import java.io.Serializable;

/**
 * Accession Register Summary document
 */
public class AccessionRegisterSymbolic extends VitamDocument<AccessionRegisterSymbolic> implements Serializable {

    private static final long serialVersionUID = 4726876406866905409L;
    /**
     * the tenant field of accession register
     */
    public static final String TENANT = "_tenant";
    /**
     * the OriginatingAgency field of accession register
     */
    public static final String ORIGINATING_AGENCY = "OriginatingAgency";
    /**
     * the total BinaryObjectSize attached to the originating agency
     */
    public static final String BINARY_OBJECTS_SIZE = "BinaryObjectSize";
    /**
     * the number of ArchiveUnit attached to the originating agency
     */
    public static final String ARCHIVE_UNIT = "ArchiveUnit";
    /**
     * the number of objectGroup attached to the originating agency
     */
    public static final String OBJECT_GROUP = "ObjectGroup";
    /**
     * the number of binaryObject attached to the originating agency
     */
    public static final String BINARY_OBJECT = "BinaryObject";
    /**
     * the creationDate field of accession register
     */
    public static final String CREATION_DATE = "CreationDate";

    /**
     * Empty Constructor
     */
    public AccessionRegisterSymbolic() {}

    /**
     * Constructor
     *
     * @param document in format Document to create AccessionRegisterSummary
     */
    public AccessionRegisterSymbolic(Document document) {
        super(document);
    }

    /**
     * @param content in format JsonNode to create AccessionRegisterSummary
     */
    public AccessionRegisterSymbolic(JsonNode content) {
        super(content);
    }

    /**
     * @param content in format String to create AccessionRegisterSummary
     */
    public AccessionRegisterSymbolic(String content) {
        super(content);
    }

    @Override
    public VitamDocument<AccessionRegisterSymbolic> newInstance(JsonNode content) {
        return new AccessionRegisterSymbolic(content);
    }

    /**
     * @param id as String to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterSymbolic setId(String id) {
        append(VitamDocument.ID, id);
        return this;
    }

    /**
     * @param orgAgency as Sting to set
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSymbolic setOriginatingAgency(String orgAgency) {
        append(ORIGINATING_AGENCY, orgAgency);
        return this;
    }

    /**
     * @param binaryObjectSize to set
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSymbolic setBinaryObjectSize(double binaryObjectSize) {
        append(BINARY_OBJECTS_SIZE, binaryObjectSize);
        return this;
    }

    /**
     * @param archiveUnit to set
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSymbolic setArchiveUnit(long archiveUnit) {
        append(ARCHIVE_UNIT, archiveUnit);
        return this;
    }

    /**
     * @param creationDate to set
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSymbolic setCreationDate(String creationDate) {
        append(CREATION_DATE, creationDate);
        return this;
    }

    /**
     * @param objectGroup to set
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSymbolic setObjectGroup(long objectGroup) {
        append(OBJECT_GROUP, objectGroup);
        return this;
    }

    /**
     * @param binaryObject to set
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSymbolic setBinaryObject(long binaryObject) {
        append(BINARY_OBJECT, binaryObject);
        return this;
    }

    /**
     * @param tenant to set
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSymbolic setTenant(int tenant) {
        append(TENANT, tenant);
        return this;
    }

    /**
     * @return String
     */
    public String getOriginatingAgency() {
        return getString(ORIGINATING_AGENCY);
    }
}
