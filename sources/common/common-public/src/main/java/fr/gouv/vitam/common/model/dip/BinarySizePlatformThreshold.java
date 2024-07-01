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

package fr.gouv.vitam.common.model.dip;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class BinarySizePlatformThreshold {

    public enum SizeUnit {
        BYTE(1L),
        KILOBYTE(1024L),
        MEGABYTE(1024 * 1024L),
        GIGABYTE(1024 * 1024 * 1024L);

        private final long bytes;

        SizeUnit(final long bytes) {
            this.bytes = bytes;
        }

        public long getByteCount() {
            return this.bytes;
        }
    }

    private long limit;
    private SizeUnit sizeUnit;

    public BinarySizePlatformThreshold() {}

    public BinarySizePlatformThreshold(long limit, SizeUnit sizeUnit) {
        this.limit = limit;
        this.sizeUnit = sizeUnit;
    }

    public long getLimit() {
        return ((Number) limit).longValue();
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public SizeUnit getSizeUnit() {
        return sizeUnit;
    }

    public void setSizeUnit(SizeUnit sizeUnit) {
        this.sizeUnit = sizeUnit;
    }

    @JsonIgnore
    public long getThreshold() {
        return getLimit() * getSizeUnit().getByteCount();
    }
}
