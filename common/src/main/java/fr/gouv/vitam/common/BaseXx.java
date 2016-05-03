package fr.gouv.vitam.common;

import com.google.common.io.BaseEncoding;

/**
 * Base16 and Base32 codecs
 */
public class BaseXx {
    private static final BaseEncoding BASE64 = BaseEncoding.base64Url().omitPadding();
    private static final BaseEncoding BASE32 =
            BaseEncoding.base32().lowerCase().omitPadding();
    private static final BaseEncoding BASE16 =
            BaseEncoding.base16().lowerCase().omitPadding();

    /**
     * @param bytes
     * @return the Base 16 representation
     */
    public static final String getBase16(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        return BASE16.encode(bytes);
    }

    /**
     * @param bytes
     * @return the Base 32 representation
     */
    public static final String getBase32(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        return BASE32.encode(bytes);
    }

    /**
     * @param bytes
     * @return the Base 64 representation
     */
    public static final String getBase64(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        return BASE64.encode(bytes);
    }

    /**
     * @param base16
     * @return the byte from Base 16
     */
    public static final byte[] getFromBase16(String base16) {
        if (base16 == null) {
            return null;
        }
        return BASE16.decode(base16);
    }

    /**
     * @param base32
     * @return the byte from Base 32
     */
    public static final byte[] getFromBase32(String base32) {
        if (base32 == null) {
            return null;
        }
        return BASE32.decode(base32);
    }

    /**
     * @param base64
     * @return the byte from Base 64
     */
    public static final byte[] getFromBase64(String base64) {
        if (base64 == null) {
            return null;
        }
        return BASE64.decode(base64);
    }
}
