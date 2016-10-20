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
package fr.gouv.vitam.ingest.internal.model;

import javax.ws.rs.FormParam;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

import fr.gouv.vitam.ingest.internal.common.util.CustomToStringStyle;

/**
 * Configuration Upload Response for type DTO
 */
@XmlRootElement
public class UploadResponseDTO {

    @FormParam("fileName")
    private String fileName;

    @FormParam("message")
    private String message;

    @FormParam("httpCode")
    private int httpCode;

    @FormParam("vitamCode")
    private String vitamCode;

    @FormParam("vitamStatus")
    private String vitamStatus;

    @FormParam("engineCode")
    private String engineCode;

    @FormParam("engineStatus")
    private String engineStatus;

    /**
     * Constructor UploadResponseDTO
     */
    public UploadResponseDTO() {
        // Empty
    }

    /**
     * Getter FileName
     *
     * @return file name of string
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName file name to set
     * @return this
     */
    public UploadResponseDTO setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    /**
     * Getter Message
     *
     * @return message of type string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Setter Message
     *
     * @param message
     * @return this
     */
    public UploadResponseDTO setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Getter HttpCode
     *
     * @return http code of type integer
     */
    public int getHttpCode() {
        return httpCode;
    }

    /**
     * Setter HttpCode
     *
     * @param httpCode
     * @return this
     */
    public UploadResponseDTO setHttpCode(int httpCode) {
        this.httpCode = httpCode;
        return this;
    }

    /**
     * Getter VitamCode
     *
     * @return Vitam code of type String
     */
    public String getVitamCode() {
        return vitamCode;
    }

    /**
     * Setter Vitam Code
     *
     * @param vitamCode
     * @return this
     */
    public UploadResponseDTO setVitamCode(String vitamCode) {
        this.vitamCode = vitamCode;
        return this;
    }

    /**
     * Getter EngineCode
     *
     * @return Engine code of type String
     */
    public String getEngineCode() {
        return engineCode;
    }

    /**
     * Setter EngineCode
     *
     * @param engineCode
     * @return this
     */
    public UploadResponseDTO setEngineCode(String engineCode) {
        this.engineCode = engineCode;
        return this;
    }


    /**
     * Getter Vitam status
     *
     * @return Vitam Status
     */
    public String getVitamStatus() {
        return vitamStatus;
    }

    /**
     * Setter Vitam Status
     *
     * @param vitamStatus
     * @return this
     */
    public UploadResponseDTO setVitamStatus(String vitamStatus) {
        this.vitamStatus = vitamStatus;
        return this;
    }

    /**
     * Getter EngineStatus
     *
     * @return Status Engine
     */
    public String getEngineStatus() {
        return engineStatus;
    }

    /**
     * Setter EngineStatus
     *
     * @param engineStatus
     * @return this
     */
    public UploadResponseDTO setEngineStatus(String engineStatus) {
        this.engineStatus = engineStatus;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, new CustomToStringStyle());
    }
}
