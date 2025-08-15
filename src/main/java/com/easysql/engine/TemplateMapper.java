package com.easysql.engine;

import com.easysql.engine.model.Template;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;

public class TemplateMapper {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public static Template fromJson(String json) throws IOException {
        return MAPPER.readValue(json, Template.class);
    }

    public static Template fromResource(String resourcePath) throws IOException {
        try (InputStream is = resourceStream(resourcePath)) {
            if (is == null) throw new IOException("resource not found: " + resourcePath);
            return MAPPER.readValue(is, Template.class);
        }
    }

    private static InputStream resourceStream(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = TemplateMapper.class.getClassLoader();
        return cl.getResourceAsStream(path.startsWith("/") ? path.substring(1) : path);
    }
}