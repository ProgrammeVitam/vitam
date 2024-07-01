/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.objectgroup;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static java.util.Optional.empty;

/**
 * Object Mapping for object group used in AccessInternalModuleImp
 */
public class ObjectGroupResponse {

    public static final String QUALIFIERS = "#qualifiers";
    public static final String ID = "#id";
    public static final String TENANT = "#tenant";
    public static final String FILE_INFO = "FileInfo";
    public static final String UNIT_UPS = "#unitups";
    public static final String ALL_UNIT_UPS = "#allunitups";
    public static final String NB_OBJECTS = "#nbobjects";
    public static final String OPERATIONS = "#operations";
    public static final String OPI = "#opi";
    public static final String ORIGINATING_AGENCY = "#originating_agency";
    public static final String ORIGINATING_AGENCIES = "#originating_agencies";
    public static final String VERSION = "#version";
    public static final String TYPE = "#type";
    public static final String STORAGE = "#storage";

    @JsonProperty(QUALIFIERS)
    private List<QualifiersModel> qualifiers;

    @JsonProperty(ID)
    private String id;

    @JsonProperty(TENANT)
    private int tenant;

    @JsonProperty(FILE_INFO)
    private FileInfoModel fileInfo;

    @JsonProperty(UNIT_UPS)
    private List<String> up;

    @JsonProperty(ALL_UNIT_UPS)
    private List<String> us;

    @JsonProperty(NB_OBJECTS)
    private int nbc;

    @JsonProperty(OPERATIONS)
    private List<String> ops;

    @JsonProperty(OPI)
    private String opi;

    @JsonProperty(ORIGINATING_AGENCY)
    private String originatingAgency;

    @JsonProperty(ORIGINATING_AGENCIES)
    private List<String> originatingAgencies;

    @JsonProperty(VERSION)
    private String version;

    @JsonProperty(TYPE)
    private String type;

    @JsonProperty(STORAGE)
    private StorageRacineModel storage;

    public StorageRacineModel getStorage() {
        return storage;
    }

    public void setStorage(StorageRacineModel storage) {
        this.storage = storage;
    }

    public List<QualifiersModel> getQualifiers() {
        return qualifiers;
    }

    public void setQualifiers(List<QualifiersModel> qualifiers) {
        this.qualifiers = qualifiers;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTenant() {
        return tenant;
    }

    public void setTenant(int tenant) {
        this.tenant = tenant;
    }

    public FileInfoModel getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfoModel fileInfo) {
        this.fileInfo = fileInfo;
    }

    public List<String> getUp() {
        return up;
    }

    public void setUp(List<String> up) {
        this.up = up;
    }

    public List<String> getUs() {
        return us;
    }

    public void setUs(List<String> us) {
        this.us = us;
    }

    public int getNbc() {
        return nbc;
    }

    public void setNbc(int nbc) {
        this.nbc = nbc;
    }

    public List<String> getOps() {
        return ops;
    }

    public void setOps(List<String> ops) {
        this.ops = ops;
    }

    public String getOriginatingAgency() {
        return originatingAgency;
    }

    public void setOriginatingAgency(String originatingAgency) {
        this.originatingAgency = originatingAgency;
    }

    public List<String> getOriginatingAgencies() {
        return originatingAgencies;
    }

    public void setOriginatingAgencies(List<String> originatingAgencies) {
        this.originatingAgencies = originatingAgencies;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOpi() {
        return opi;
    }

    public void setOpi(String opi) {
        this.opi = opi;
    }

    @JsonIgnore
    public Optional<VersionsModel> getLastVersionsModel(String qualifier) {
        Optional<QualifiersModel> first = getQualifiersModel(qualifier);

        if (first.isPresent()) {
            return first.get().getVersions().stream().max(comparing(VersionsModel::getDataVersion));
        }
        return empty();
    }

    @JsonIgnore
    public Optional<VersionsModel> getFirstVersionsModel(String qualifier) {
        Optional<QualifiersModel> modelOptional = getQualifiersModel(qualifier);

        if (modelOptional.isPresent()) {
            return modelOptional.get().getVersions().stream().min(comparing(VersionsModel::getDataVersion));
        }
        return empty();
    }

    private Optional<QualifiersModel> getQualifiersModel(String qualifier) {
        return qualifiers.stream().filter(q -> q.getQualifier().equals(qualifier)).findFirst();
    }
}
