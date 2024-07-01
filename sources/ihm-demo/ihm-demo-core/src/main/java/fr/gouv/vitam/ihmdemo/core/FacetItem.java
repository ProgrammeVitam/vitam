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
package fr.gouv.vitam.ihmdemo.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.database.facet.model.FacetOrder;
import fr.gouv.vitam.common.model.FacetDateRangeItem;
import fr.gouv.vitam.common.model.FacetFiltersItem;
import fr.gouv.vitam.common.model.FacetType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Description of facet item model. <br/>
 */
public class FacetItem {

    /**
     * Name.
     */
    @JsonProperty("name")
    private String name;

    /**
     * Facet type.
     */
    @JsonProperty("facetType")
    private FacetType facetType;

    /**
     * Field.
     */
    @JsonProperty("field")
    private String field;

    /**
     * Subobject.
     */
    @JsonProperty("subobject")
    private String subobject;

    /**
     * Size.
     */
    @JsonProperty("size")
    private Integer size;

    /**
     * FacetOrder.
     */
    @JsonProperty("order")
    private FacetOrder order;

    /**
     * Date format.
     */
    @JsonProperty("format")
    private String format;

    /**
     * Date ranges.
     */
    @JsonProperty("ranges")
    private List<FacetDateRangeItem> ranges;

    /**
     * Filters.
     */
    @JsonProperty("filters")
    private List<FacetFiltersItem> filters;

    private static final Map<String, String> nestedFields = Stream.of(
        new String[][] {
            { "#qualifiers.versions.FormatIdentification.FormatLitteral", "#qualifiers.versions" },
            { "#qualifiers.versions.DataObjectVersion", "#qualifiers.versions" },
        }
    ).collect(
        Collectors.collectingAndThen(
            Collectors.toMap(data -> data[0], data -> data[1]),
            Collections::<String, String>unmodifiableMap
        )
    );

    /**
     * Constructor.
     */
    public FacetItem() {
        super();
    }

    public FacetItem(
        String name,
        FacetType facetType,
        String field,
        Integer size,
        FacetOrder order,
        String format,
        List<FacetDateRangeItem> ranges,
        List<FacetFiltersItem> filters,
        Optional<String> subobject
    ) {
        this.name = name;
        this.facetType = facetType;
        this.field = field;
        this.size = size;
        this.order = order;
        this.format = format;
        this.ranges = ranges;
        this.filters = filters;
    }

    /**
     * getName
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * setName
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * getField
     *
     * @return
     */
    public String getField() {
        return field;
    }

    /**
     * setField
     *
     * @param field
     */
    public void setField(String field) {
        this.field = field;
        if (nestedFields.containsKey(field)) {
            this.subobject = nestedFields.get(field);
        }
    }

    /**
     * getSize
     *
     * @return
     */
    public Integer getSize() {
        return size;
    }

    /**
     * setSize
     *
     * @param size
     */
    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * getFormat
     *
     * @return
     */
    public String getFormat() {
        return format;
    }

    /**
     * setFormat
     *
     * @param format
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * getRanges
     *
     * @return
     */
    public List<FacetDateRangeItem> getRanges() {
        return ranges;
    }

    /**
     * setRanges
     *
     * @param ranges
     */
    public void setRanges(List<FacetDateRangeItem> ranges) {
        this.ranges = ranges;
    }

    /**
     * getFilters
     *
     * @return
     */
    public List<FacetFiltersItem> getFilters() {
        return filters;
    }

    /**
     * setFilters
     *
     * @param filters
     */
    public void setFilters(List<FacetFiltersItem> filters) {
        this.filters = filters;
    }

    /**
     * getFacetType
     *
     * @return
     */
    public FacetType getFacetType() {
        return facetType;
    }

    /**
     * setFacetType
     *
     * @param facetType
     */
    public void setFacetType(FacetType facetType) {
        this.facetType = facetType;
    }

    public FacetOrder getOrder() {
        return order;
    }

    public void setOrder(FacetOrder order) {
        this.order = order;
    }

    public String getSubobject() {
        return subobject;
    }
}
