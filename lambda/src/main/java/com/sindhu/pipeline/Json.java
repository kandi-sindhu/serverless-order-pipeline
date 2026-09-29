package com.sindhu.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.UncheckedIOException;

/** Shared, thread-safe Jackson mapper. Created once per Lambda container. */
final class Json {

    static final ObjectMapper MAPPER = new ObjectMapper();

    private Json() {
    }

    static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
