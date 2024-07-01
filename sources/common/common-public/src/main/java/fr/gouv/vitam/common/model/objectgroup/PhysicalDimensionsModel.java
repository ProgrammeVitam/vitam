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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * PhysicalDimensions for physical Object
 */
public class PhysicalDimensionsModel {

    @JsonProperty("Width")
    private MeasurementModel width;

    @JsonProperty("Height")
    private MeasurementModel height;

    @JsonProperty("Depth")
    private MeasurementModel depth;

    @JsonProperty("Shape")
    private String shape;

    @JsonProperty("Diameter")
    private MeasurementModel diameter;

    @JsonProperty("Length")
    private MeasurementModel length;

    @JsonProperty("Thickness")
    private MeasurementModel thickness;

    @JsonProperty("Weight")
    private MeasurementModel weight;

    @JsonProperty("NumberOfPage")
    private Integer numberOfPage;

    protected Map<String, Object> any;

    public MeasurementModel getWidth() {
        return width;
    }

    public void setWidth(MeasurementModel width) {
        this.width = width;
    }

    public MeasurementModel getHeight() {
        return height;
    }

    public void setHeight(MeasurementModel height) {
        this.height = height;
    }

    public MeasurementModel getDepth() {
        return depth;
    }

    public void setDepth(MeasurementModel depth) {
        this.depth = depth;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public MeasurementModel getDiameter() {
        return diameter;
    }

    public void setDiameter(MeasurementModel diameter) {
        this.diameter = diameter;
    }

    public MeasurementModel getLength() {
        return length;
    }

    public void setLength(MeasurementModel length) {
        this.length = length;
    }

    public MeasurementModel getThickness() {
        return thickness;
    }

    public void setThickness(MeasurementModel thickness) {
        this.thickness = thickness;
    }

    public MeasurementModel getWeight() {
        return weight;
    }

    public void setWeight(MeasurementModel weight) {
        this.weight = weight;
    }

    public Integer getNumberOfPage() {
        return numberOfPage;
    }

    public void setNumberOfPage(Integer numberOfPage) {
        this.numberOfPage = numberOfPage;
    }

    public Map<String, Object> getAny() {
        if (any == null) {
            any = new HashMap<>();
        }
        return any;
    }

    public void setAny(Map<String, Object> any) {
        this.any = any;
    }
}
