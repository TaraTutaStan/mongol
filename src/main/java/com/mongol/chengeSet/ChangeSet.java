package com.iri.backend.tools.chengeSet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChangeSet {

    public String author();  // must be set
    public String id();      // must be set
    public String order();   // must be set
    public boolean runAlways() default false;

}