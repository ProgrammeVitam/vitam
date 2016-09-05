/**
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

package fr.gouv.vitam.functional.administration.common;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;

/** 
 * FileFormat
 *  define the file referential format for Vitam 
 */

public class FileFormat extends VitamDocument<FileFormat> {

    public static final String PUID = "PUID";
    private static final String VERSION_PRONOM = "VersionPronom";
    private static final String VERSION = "Version";
    private static final String CREATED_DATE = "CreatedDate";
    private static final String HAS_PRIORITY_OVER_FILE_FORMAT_ID = "HasPriorityOverFileFormatID";
    private static final String MIME_TYPE = "MIMEType";
    private static final String NAME = "Name";
    private static final String EXTENSION = "Extension";
    private static final long serialVersionUID = 7794456688851515535L;


    /**
     * empty constructor
     */
    public FileFormat() {}

    /**
     * constructor with Mongo Document
     * @param document as Document of bson
     */
    public FileFormat(Document document) {
        super(document);
    }

    /** setPUID
     * @param puid as String
     * @return FileFormat with puid setted
     */
    public FileFormat setPUID(String puid) {
        this.append(PUID, puid);
        return this;
    }

    /** setExtension
     * @param extension as a list of String
     * @return FileFormat with extension setted
     */
    public FileFormat setExtension(List<String> extension) {
        if (!extension.isEmpty()) {
            List<String> ext = new ArrayList<>();
            ext.addAll(extension);
            this.append(EXTENSION, ext);
        }
        return this;
    }

    /** setName
     * @param name as String
     * @return FileFormat with name setted
     */    
    public FileFormat setName(String name) {
        this.append(NAME, name);
        return this;
    }

    /** setMimeType
     * @param mimeType as String
     * @return FileFormat with mimeType setted
     */    
    public FileFormat setMimeType(List<String> mimeType) {
        this.append(MIME_TYPE, mimeType);
        return this;
    }

    /** setVersion
     * @param version as String
     * @return FileFormat with version setted
     */    
    public FileFormat setVersion(String version) {
        this.append(VERSION, version);
        return this;
    }

    /** setPriorityOverIdList
     * @param priorityOverIdList as a list of String
     * @return FileFormat 
     */
    public FileFormat setPriorityOverIdList(List<String> priorityOverIdList) {
        if (!priorityOverIdList.isEmpty()) {
            List<String> priorityList = new ArrayList<>();
            priorityList.addAll(priorityOverIdList);
            this.append(HAS_PRIORITY_OVER_FILE_FORMAT_ID, priorityList);
        }
        return this;
    }

    /** setCreatedDate
     * @param createdDate as String
     * @return FileFormat with createdDate setted
     */
    public FileFormat setCreatedDate(String createdDate) {
        this.append(CREATED_DATE, createdDate);
        return this;
    }

    /** setPronomVersion
     * @param pronomVersion as String
     * @return FileFormat with pronomVersion setted
     */    
    public FileFormat setPronomVersion(String pronomVersion) {
        this.append(VERSION_PRONOM, pronomVersion);
        return this;
    }

}
