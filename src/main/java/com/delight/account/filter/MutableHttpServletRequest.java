package com.delight.account.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public class MutableHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders = new HashMap<>();
    private final Map<String, Object> customAttributes = new HashMap<>();

    public MutableHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public void putHeader(String name, String value) {
        customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        String headerValue = customHeaders.get(name);
        if (headerValue != null) {
            return headerValue;
        }
        return ((HttpServletRequest) getRequest()).getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new HashSet<>(customHeaders.keySet());
        Enumeration<String> enums = ((HttpServletRequest) getRequest()).getHeaderNames();
        while (enums.hasMoreElements()) {
            names.add(enums.nextElement());
        }
        return Collections.enumeration(names);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (customHeaders.containsKey(name)) {
            return Collections.enumeration(List.of(customHeaders.get(name)));
        }
        return ((HttpServletRequest) getRequest()).getHeaders(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        customAttributes.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        Object attributeValue = customAttributes.get(name);
        if (attributeValue != null) {
            return attributeValue;
        }
        return super.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> names = new HashSet<>(customAttributes.keySet());
        Enumeration<String> enums = super.getAttributeNames();
        while (enums.hasMoreElements()) {
            names.add(enums.nextElement());
        }
        return Collections.enumeration(names);
    }

    @Override
    public void removeAttribute(String name) {
        customAttributes.remove(name);
        super.removeAttribute(name);
    }
}

