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
package fr.gouv.vitam.common.model.unit;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static fr.gouv.vitam.common.utils.SupportedSedaVersions.UNIFIED_NAMESPACE;

/**
 * Model for  LocationGroup defined in sead.xsd
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LocationGroup", namespace = UNIFIED_NAMESPACE)
public class LocationGroupModel {

    @JsonProperty("Geogname")
    @XmlElement(name = "Geogname", namespace = UNIFIED_NAMESPACE)
    private String geogname;

    @JsonProperty("Address")
    @XmlElement(name = "Address", namespace = UNIFIED_NAMESPACE)
    private String address;

    @JsonProperty("PostalCode")
    @XmlElement(name = "PostalCode", namespace = UNIFIED_NAMESPACE)
    private String postalCode;

    @JsonProperty("City")
    @XmlElement(name = "City", namespace = UNIFIED_NAMESPACE)
    private String city;

    @JsonProperty("Region")
    @XmlElement(name = "Region", namespace = UNIFIED_NAMESPACE)
    private String region;

    @JsonProperty("Country")
    @XmlElement(name = "Country", namespace = UNIFIED_NAMESPACE)
    private String country;

    public LocationGroupModel() {}

    public String getGeogname() {
        return geogname;
    }

    public void setGeogname(String geogname) {
        this.geogname = geogname;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
