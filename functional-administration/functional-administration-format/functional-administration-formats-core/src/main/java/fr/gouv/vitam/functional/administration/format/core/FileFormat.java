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

package fr.gouv.vitam.functional.administration.format.core;

import java.util.List;

/** FileFormat
 *  define the file referential format for Vitam 
 */

public class FileFormat {
    private String puid;
    private String name;
    private String version;
    private List<String> mimeType;
    private List<String> extension;
    private List<String> priviousVersion;
    private String createdDate;
    private String pronomVersion;
    private String comment;
    private boolean alertCheck = false;
    private String groupIdentity = null;

    /**
     * constructor
     */
    public FileFormat() {}

    /** setPUID
     * @param puid as String
     * @return FileFormat with puid setted
     */
    public FileFormat setPUID(String puid) {
        this.puid = puid;
        return this;
    }

    /** setExtension
     * @param extension as a list of String
     * @return FileFormat with extension setted
     */
    public FileFormat setExtension(List<String> extension) {
        this.extension = extension;
        return this;
    }

    /** setName
     * @param name as String
     * @return FileFormat with name setted
     */    
    public FileFormat setName(String name) {
        this.name = name;
        return this;
    }

    /** setMimeType
     * @param mimeType as String
     * @return FileFormat with mimeType setted
     */    
    public FileFormat setMimeType(List<String> mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    /** setVersion
     * @param version as String
     * @return FileFormat with version setted
     */    
    public FileFormat setVersion(String version) {
        this.version = version;
        return this;
    }

    /** setPriviousVersion
     * @param priviousVersion as a list of String
     * @return FileFormat with priviousVersion setted
     */
    public FileFormat setPriviousVersion(List<String> priviousVersion) {
        this.priviousVersion = priviousVersion;
        return this;
    }

    /** setCreatedDate
     * @param createdDate as String
     * @return FileFormat with createdDate setted
     */
    public FileFormat setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    /** setPronomVersion
     * @param pronomVersion as String
     * @return FileFormat with pronomVersion setted
     */    
    public FileFormat setPronomVersion(String pronomVersion) {
        this.pronomVersion = pronomVersion;
        return this;
    }
    
    /** getPUID
     * @return the puid value of file format 
     */
    public String getPUID() {
        return this.puid;
    }

    
    /** getMimeType
     * @return the mimeType value list of file format 
     */
    public List<String> getMimeType() {
        return this.mimeType;
    }

    /** getName
     * @return the name value of file format 
     */        
    public String getName() {
        return this.name;
    }

    /** getExtension
     * @return the extension value list of file format 
     */    
    public List<String> getExtension() {
        return this.extension;
    }

    /** getVersion
     * @return the version value of file format 
     */    
    public String getVersion() {
        return this.version;
    }

    /** getPriviousVersion
     * @return the priviousVersion value of file format 
     */    
    public List<String> getPriviousVersion() {
        return this.priviousVersion;
    }

    /** getCreatedDate
     * @return the createdDate value of file format 
     */    
    public String getCreatedDate() {
        return this.createdDate;
    }

    /** getPronomVersion
     * @return the pronomVersion value of file format 
     */        
    public String getPronomVersion() {
        return this.pronomVersion;
    }

    /** getComment
     * @return the comment value of file format 
     */            
    public String getComment() {
        return this.comment;
    }

    /** getAlertCheck
     * @return the alertCheck value of file format 
     */            
    public boolean getAlertCheck() {
        return this.alertCheck;
    }

    /** getGroupIdentity
     * @return the groupIdentity value of file format 
     */            
    public String getGroupIdentity() {
        return this.groupIdentity;
    }

}
