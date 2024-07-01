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
package fr.gouv.vitam.common;

import com.google.common.io.BaseEncoding;

/**
 * Base16, Base32 and Base64 codecs
 */
public final class BaseXx {

    private static final String ARGUMENT_NULL_NOT_ALLOWED = "argument null not allowed";
    private static final BaseEncoding BASE64_URL_WITHOUT_PADDING = BaseEncoding.base64Url().omitPadding();
    private static final BaseEncoding BASE64_URL_WITH_PADDING = BaseEncoding.base64Url();
    private static final BaseEncoding BASE64 = BaseEncoding.base64();
    private static final BaseEncoding BASE32 = BaseEncoding.base32().lowerCase().omitPadding();
    private static final BaseEncoding BASE16 = BaseEncoding.base16().lowerCase().omitPadding();

    private BaseXx() {
        // empty
    }

    /**
     * @param bytes to transform
     * @return the Base 16 representation
     * @throws IllegalArgumentException if argument is not compatible
     */
    public static final String getBase16(byte[] bytes) {
        ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, bytes);
        return BASE16.encode(bytes);
    }

    /**
     * @param bytes to transform
     * @return the Base 32 representation
     * @throws IllegalArgumentException if argument is not compatible
     */
    public static final String getBase32(byte[] bytes) {
        ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, bytes);
        return BASE32.encode(bytes);
    }

    /**
     * @param bytes to transform
     * @return the Base 64 Without Padding representation (used only for url)
     * @throws IllegalArgumentException if argument is not compatible
     */
    public static final String getBase64UrlWithoutPadding(byte[] bytes) {
        ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, bytes);
        return BASE64_URL_WITHOUT_PADDING.encode(bytes);
    }

    /**
     * @param bytes to transform
     * @return the Base 64 With Padding representation (used only for url)
     * @throws IllegalArgumentException if argument is not compatible
     */
    public static final String getBase64UrlWithPadding(byte[] bytes) {
        ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, bytes);
        return BASE64_URL_WITH_PADDING.encode(bytes);
    }

    /**
     * @param bytes to transform
     * @return the Base 64 With Padding representation
     * @throws IllegalArgumentException if argument is not compatible
     */
    public static final String getBase64(byte[] bytes) {
        ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, bytes);
        return BASE64.encode(bytes);
    }

    /**
     * @param base16 to transform
     * @return the byte from Base 16
     * @throws IllegalArgumentException if argument is not compatible
     */
    public static final byte[] getFromBase16(String base16) {
        ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, base16);
        return BASE16.decode(base16);
    }

    /**
     * @param base32 to transform
     * @return the byte from Base 32
     * @throws IllegalArgumentException if argument is not compatible
     */
    public static final byte[] getFromBase32(String base32) {
        ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, base32);
        return BASE32.decode(base32);
    }

    /**
     * @param base64 to transform
     * @return the byte from Base 64 Without Padding (used only for url)
     * @throws IllegalArgumentException if argument is not compatible
     */
    public static final byte[] getFromBase64UrlWithoutPadding(String base64) {
        ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, base64);
        return BASE64_URL_WITHOUT_PADDING.decode(base64);
    }

    /**
     * @param base64Padding to transform
     * @return the byte from Base 64 With Padding (used only for url)
     * @throws IllegalArgumentException if argument is not compatible
     */
    public static final byte[] getFromBase64UrlPadding(String base64Padding) {
        ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, base64Padding);
        return BASE64_URL_WITH_PADDING.decode(base64Padding);
    }

    /**
     * @param base64Padding to transform
     * @return the byte from Base 64 With Padding
     * @throws IllegalArgumentException if argument is not compatible
     */
    public static final byte[] getFromBase64(String base64Padding) {
        ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, base64Padding);
        return BASE64.decode(base64Padding);
    }
}
