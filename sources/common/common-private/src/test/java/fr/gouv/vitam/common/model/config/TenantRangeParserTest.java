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

package fr.gouv.vitam.common.model.config;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TenantRangeParserTest {

    @Test
    public void testSingleTenant() {
        List<TenantRange> tenantRanges = TenantRangeParser.parseTenantRanges("0");
        assertThat(tenantRanges).hasSize(1);
        assertThat(tenantRanges.get(0).getMinValue()).isEqualTo(0);
        assertThat(tenantRanges.get(0).getMaxValue()).isEqualTo(0);
    }

    @Test
    public void testTenantRange() {
        List<TenantRange> tenantRanges = TenantRangeParser.parseTenantRanges("5-10");
        assertThat(tenantRanges).hasSize(1);
        assertThat(tenantRanges.get(0).getMinValue()).isEqualTo(5);
        assertThat(tenantRanges.get(0).getMaxValue()).isEqualTo(10);
    }

    @Test
    public void testComplexWithSpacing() {
        List<TenantRange> tenantRanges = TenantRangeParser.parseTenantRanges("\t9,2-3,4, 10 - 10\t , 12  \t");

        assertThat(tenantRanges).hasSize(5);

        assertThat(tenantRanges.get(0).getMinValue()).isEqualTo(9);
        assertThat(tenantRanges.get(0).getMaxValue()).isEqualTo(9);

        assertThat(tenantRanges.get(1).getMinValue()).isEqualTo(2);
        assertThat(tenantRanges.get(1).getMaxValue()).isEqualTo(3);

        assertThat(tenantRanges.get(2).getMinValue()).isEqualTo(4);
        assertThat(tenantRanges.get(2).getMaxValue()).isEqualTo(4);

        assertThat(tenantRanges.get(3).getMinValue()).isEqualTo(10);
        assertThat(tenantRanges.get(3).getMaxValue()).isEqualTo(10);

        assertThat(tenantRanges.get(4).getMinValue()).isEqualTo(12);
        assertThat(tenantRanges.get(4).getMaxValue()).isEqualTo(12);
    }

    @Test
    public void testInvalidTenantRange() {
        assertThatThrownBy(() -> TenantRangeParser.parseTenantRanges("5-2")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testInvalidTenant() {
        assertThatThrownBy(() -> TenantRangeParser.parseTenantRanges("invalid")).isInstanceOf(
            IllegalStateException.class
        );
    }

    @Test
    public void testRangeIntersectionNoIntersections() {
        TenantRange tenantRange1 = new TenantRange(1, 10);
        TenantRange tenantRange2 = new TenantRange(11, 20);
        assertThat(TenantRangeParser.doRangesIntersect(tenantRange1, tenantRange2)).isFalse();
        assertThat(TenantRangeParser.doRangesIntersect(tenantRange2, tenantRange1)).isFalse();
    }

    @Test
    public void testRangeIntersectionOverlap() {
        TenantRange tenantRange1 = new TenantRange(1, 10);
        TenantRange tenantRange2 = new TenantRange(8, 11);
        assertThat(TenantRangeParser.doRangesIntersect(tenantRange1, tenantRange2)).isTrue();
        assertThat(TenantRangeParser.doRangesIntersect(tenantRange2, tenantRange1)).isTrue();
    }

    @Test
    public void testRangeIntersectionIntersectionsSameRanges() {
        TenantRange tenantRange1 = new TenantRange(1, 10);
        TenantRange tenantRange2 = new TenantRange(1, 10);
        assertThat(TenantRangeParser.doRangesIntersect(tenantRange1, tenantRange2)).isTrue();
        assertThat(TenantRangeParser.doRangesIntersect(tenantRange2, tenantRange1)).isTrue();
    }

    @Test
    public void testRangeIntersectionIntersectionsSubRanges() {
        TenantRange tenantRange1 = new TenantRange(1, 10);
        TenantRange tenantRange2 = new TenantRange(5, 8);
        assertThat(TenantRangeParser.doRangesIntersect(tenantRange1, tenantRange2)).isTrue();
        assertThat(TenantRangeParser.doRangesIntersect(tenantRange2, tenantRange1)).isTrue();
    }
}
