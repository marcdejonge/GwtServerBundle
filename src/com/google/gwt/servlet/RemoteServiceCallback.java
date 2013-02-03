package com.google.gwt.servlet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

public interface RemoteServiceCallback {
    Object handleCall(HttpServletRequest request, Method method, Object... params) throws IllegalArgumentException,
                                                                                  IllegalAccessException,
                                                                                  InvocationTargetException;
}
