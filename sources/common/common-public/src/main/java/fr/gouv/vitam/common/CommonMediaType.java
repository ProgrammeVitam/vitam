/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common;

import javax.ws.rs.core.MediaType;

/**
 * CommonMediaType contains a different archive mime type supported by Vitam
 */
public class CommonMediaType extends MediaType {

    private static final String APPLICATION = "application";

    /**
     * A {@code String} constant representing {@value #ZIP} media type.
     */
    public final static String ZIP = "application/zip";

    /**
     * A {@link MediaType} constant representing {@value #ZIP} media type.
     */
    public static final MediaType ZIP_TYPE = new MediaType(APPLICATION, "zip");
    /**
     * A {@code String} constant representing {@value #TAR} media type.
     */
    public final static String TAR = "application/x-tar";
    /**
     * A {@link MediaType} constant representing {@value #TAR} media type.
     */
    public static final MediaType TAR_TYPE = new MediaType(APPLICATION, "x-tar");

    /**
     * A {@code String} constant representing {@value #XGZIP} media type.
     */
    public final static String XGZIP = "application/x-gzip";
    /**
     * A {@link MediaType} constant representing {@value #XGZIP} media type.
     */
    public static final MediaType XGZIP_TYPE = new MediaType(APPLICATION, "x-gzip");

    /**
     * A {@code String} constant representing {@value #XGZIP} media type.
     */
    public final static String GZIP = "application/gzip";
    /**
     * A {@link MediaType} constant representing {@value #GZIP} media type.
     */
    public static final MediaType GZIP_TYPE = new MediaType(APPLICATION, "gzip");

    /**
     * A {@code String} constant representing {@value #BZIP2} media type.
     *
     */
    public static final String BZIP2 = "application/x-bzip2";

    /**
     * A {@link MediaType} constant representing {@value #BZIP2} media type.
     */
    public static final MediaType BZIP2_TYPE = new MediaType(APPLICATION, "x-bzip2");

    /**
     * Creates an instance of {@code MediaType} by the supplied string.
     *
     * @param archivetype the media type string.
     * @return the MediaType.
     * @throws IllegalArgumentException if the supplied string cannot be supported or is {@code null}.
     */
    public static MediaType valueOf(String archivetype) {
        if (archivetype == null) {
            throw new IllegalArgumentException("MimeType not filled");
        }
        final String[] tab = archivetype.split(";");
        // problem mimetype with encoding code ;charset=ISO-8859-1
        String newMimeType = archivetype;
        if (tab != null && tab.length > 1) {
            newMimeType = tab[0];
        }

        switch (newMimeType) {
            case ZIP:
                return ZIP_TYPE;
            case XGZIP:
                return XGZIP_TYPE;
            case GZIP:
                return GZIP_TYPE;
            case TAR:
                return TAR_TYPE;
            case BZIP2:
                return BZIP2_TYPE;

            default:
                throw new IllegalArgumentException("Unsupported media type:" + archivetype);
        }
    }

    /**
     * Creates mime type code {@code String} of Media type.
     *
     * @param mediaType {@link MediaType}
     * @return A {@code String} constant representing media type
     */
    public static String mimeTypeOf(MediaType mediaType) {
        if (mediaType == null) {
            throw new IllegalArgumentException("mediaType must not be null.");
        }
        return mediaType.getType() + "/" + mediaType.getSubtype();

    }


    /**
     * Checks archive type if is supported by Vitam.
     *
     * @since 0.10.0
     *
     * @param mimeType the mime type to check
     * @return boolean : true if archive type supported by Vitam.
     */
    public static boolean isSupportedFormat(String mimeType) {

        switch (mimeType) {
            case CommonMediaType.ZIP:
            case CommonMediaType.XGZIP:
            case CommonMediaType.GZIP:
            case CommonMediaType.TAR:
            case CommonMediaType.BZIP2:
                return true;
            default:
                return false;
        }

    }


}
