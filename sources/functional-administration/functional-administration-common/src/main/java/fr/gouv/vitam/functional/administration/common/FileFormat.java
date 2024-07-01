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
package fr.gouv.vitam.functional.administration.common;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * FileFormat define the file referential format for Vitam
 */

public class FileFormat extends VitamDocument<FileFormat> {

    /**
     * FileFormat PUID key
     */
    public static final String PUID = "PUID";

    /**
     * FileFormat Pronom version
     */
    public static final String VERSION_PRONOM = "VersionPronom";

    /**
     * FileFormat Version
     */
    public static final String VERSION = "Version";

    /**
     * FileFormat Creation date
     */
    public static final String CREATED_DATE = "CreatedDate";

    /**
     * Last update date
     */
    public static final String UPDATE_DATE = "UpdateDate";

    /**
     * manage document priority
     */
    public static final String HAS_PRIORITY_OVER_FILE_FORMAT_ID = "HasPriorityOverFileFormatID";
    /**
     * FileFormat Mime Type
     */
    public static final String MIME_TYPE = "MimeType";

    /**
     * FileFormat name
     */
    public static final String NAME = "Name";

    /**
     * FileFormat group
     */
    public static final String GROUP = "Group";

    /**
     * FileFormat alert
     */
    public static final String ALERT = "Alert";
    /**
     * FileFormat comment
     */
    public static final String COMMENT = "Comment";
    /**
     * FileFormat extension
     */
    public static final String EXTENSION = "Extension";

    /**
     * Serial
     */
    private static final long serialVersionUID = 7794456688851515535L;

    /**
     * empty constructor
     */
    public FileFormat() {
        // Empty
    }

    /**
     * constructor with Mongo Document
     *
     * @param document as Document of bson
     */
    public FileFormat(Document document) {
        super(document);
    }

    /**
     * @param content in format JsonNode to create
     */
    public FileFormat(JsonNode content) {
        super(content);
    }

    /**
     * @param content in format String to create
     */
    public FileFormat(String content) {
        super(content);
    }

    @Override
    public VitamDocument<FileFormat> newInstance(JsonNode content) {
        return new FileFormat(content);
    }

    /**
     * setPUID
     *
     * @param puid as String
     * @return FileFormat with puid setted
     */
    public FileFormat setPUID(String puid) {
        append(PUID, puid);
        return this;
    }

    /**
     * setExtension
     *
     * @param extension as a list of String
     * @return FileFormat with extension setted
     */
    public FileFormat setExtension(List<String> extension) {
        if (!extension.isEmpty()) {
            final List<String> ext = new ArrayList<>();
            ext.addAll(extension);
            append(EXTENSION, ext);
        }
        return this;
    }

    /**
     * setName
     *
     * @param name as String
     * @return FileFormat with name setted
     */
    public FileFormat setName(String name) {
        append(NAME, name);
        return this;
    }

    /**
     * setMimeType
     *
     * @param mimeType as String
     * @return FileFormat with mimeType setted
     */
    public FileFormat setMimeType(String mimeType) {
        append(MIME_TYPE, mimeType);
        return this;
    }

    /**
     * setVersion
     *
     * @param version as String
     * @return FileFormat with version setted
     */
    public FileFormat setVersion(String version) {
        append(VERSION, version);
        return this;
    }

    /**
     * setPriorityOverIdList
     *
     * @param priorityOverIdList as a list of String
     * @return FileFormat
     */
    public FileFormat setPriorityOverIdList(List<String> priorityOverIdList) {
        if (!priorityOverIdList.isEmpty()) {
            final List<String> priorityList = new ArrayList<>();
            priorityList.addAll(priorityOverIdList);
            append(HAS_PRIORITY_OVER_FILE_FORMAT_ID, priorityList);
        }
        return this;
    }

    /**
     * setCreatedDate
     *
     * @param createdDate as String
     * @return FileFormat with createdDate setted
     */
    public FileFormat setCreatedDate(String createdDate) {
        append(CREATED_DATE, createdDate);
        return this;
    }

    /**
     * setPronomVersion
     *
     * @param pronomVersion as String
     * @return FileFormat with pronomVersion setted
     */
    public FileFormat setPronomVersion(String pronomVersion) {
        append(VERSION_PRONOM, pronomVersion);
        return this;
    }

    /**
     * setComment
     *
     * @param comment as String
     * @return FileFormat with pronomVersion setted
     */
    public FileFormat setComment(String comment) {
        append(COMMENT, comment);
        return this;
    }

    /**
     * setAlert
     *
     * @param alert as boolean
     * @return FileFormat with pronomVersion setted
     */
    public FileFormat setAlert(boolean alert) {
        append(ALERT, alert);
        return this;
    }

    /**
     * setGroup
     *
     * @param group as String
     * @return FileFormat with pronomVersion setted
     */
    public FileFormat setGroup(String group) {
        append(GROUP, group);
        return this;
    }
}
