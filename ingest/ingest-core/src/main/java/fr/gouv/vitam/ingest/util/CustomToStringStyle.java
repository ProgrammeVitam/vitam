package fr.gouv.vitam.ingest.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by bsui on 12/05/16.
 */
public class CustomToStringStyle extends ToStringStyle {

    /**
     *
     */
    private static final long serialVersionUID = -6477884518987519835L;

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, Object value) {
        if (value instanceof Date) {
            value = new SimpleDateFormat("yyyy-MM-dd").format(value);
        }
        buffer.append(value);
    }
}
