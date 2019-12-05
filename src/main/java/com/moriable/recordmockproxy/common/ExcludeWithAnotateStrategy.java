package com.moriable.recordmockproxy.common;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import java.lang.annotation.Annotation;

public class ExcludeWithAnotateStrategy implements ExclusionStrategy {
    @Override
    public boolean shouldSkipClass(Class<?> clazz){
        return clazz.getAnnotation(Exclude.class) != null;
    }
    @Override
    public boolean shouldSkipField(FieldAttributes f){
        return f.getAnnotation(Exclude.class) != null;
    }
}