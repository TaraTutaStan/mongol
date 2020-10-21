package com.iri.backend.tools.utils;

import com.iri.backend.tools.chengeSet.ChangeLog;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Comparator;

public class ChangeLogComparator implements Comparator<Class<?>>, Serializable {
    @Override
    public int compare(Class<?> o1, Class<?> o2) {
        ChangeLog c1 = o1.getAnnotation(ChangeLog.class);
        ChangeLog c2 = o2.getAnnotation(ChangeLog.class);

        String val1 = StringUtils.isBlank(c1.order()) ? o1.getCanonicalName() : c1.order();
        String val2 = StringUtils.isBlank(c2.order()) ? o2.getCanonicalName() : c2.order();

        if (val1 == null && val2 == null) {
            return 0;
        } else if (val1 == null) {
            return -1;
        } else if (val2 == null) {
            return 1;
        }

        return val1.compareTo(val2);
    }
}
