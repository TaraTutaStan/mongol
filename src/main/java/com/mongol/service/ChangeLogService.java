package com.iri.backend.tools.service;


import com.iri.backend.tools.chengeSet.ChangeEntry;
import com.iri.backend.tools.chengeSet.ChangeLog;
import com.iri.backend.tools.chengeSet.ChangeSet;
import com.iri.backend.tools.utils.ChangeLogComparator;
import com.iri.backend.tools.utils.ChangeSetComparator;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class ChangeLogService {

    private final String changeLogsBasePackage;

    public ChangeLogService(String changeLogsBasePackage) {
        this.changeLogsBasePackage = changeLogsBasePackage;
    }

    public List<Class<?>> getChangeLogs() {
        Reflections reflections = new Reflections(changeLogsBasePackage);
        Set<Class<?>> changeLogs = reflections.getTypesAnnotatedWith(ChangeLog.class);
        return changeLogs
                .stream()
                .sorted(new ChangeLogComparator())
                .collect(Collectors.toList());
    }


    public List<Method> getChangeSets(final Class<?> type) {
        return asList(type.getDeclaredMethods())
                .stream()
                .sorted(new ChangeSetComparator())
                .collect(Collectors.toList());
    }

    public boolean isRunAlways(Method method) {
        if (method.isAnnotationPresent(ChangeSet.class)) {
            ChangeSet annotation = method.getAnnotation(ChangeSet.class);
            return annotation.runAlways();
        } else {
            return false;
        }
    }

    public ChangeEntry extractChangeEntry(Method method) {
        if (method.isAnnotationPresent(ChangeSet.class)) {
            ChangeSet annotation = method.getAnnotation(ChangeSet.class);

            return new ChangeEntry(
                    annotation.id(),
                    annotation.author(),
                    new Date(),
                    method.getDeclaringClass().getName(),
                    method.getName());
        } else {
            return null;
        }
    }

}
