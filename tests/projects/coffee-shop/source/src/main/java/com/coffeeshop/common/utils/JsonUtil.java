package com.coffeeshop.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.coffeeshop.common.valueobjects.OrderIn;
import com.coffeeshop.common.valueobjects.OrderUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(JsonUtil.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModules(new JavaTimeModule(),new Jdk8Module());

    public static String toJson(final Object object) {
        LOGGER.debug("marshalling {} to JSON", object.toString());
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage());
            return "{ \"error\" : \"" + e.getMessage() + "\" }";
        }
    }

    public static <T> T fromJson(final String json, Class clazz) {
        try {
            return (T) objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static OrderUp fromJsonToOrderUp(String json) {
        try {
            return objectMapper.readValue(json, OrderUp.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static OrderIn fromJsonToOrderIn(String json) {
        try {
            return objectMapper.readValue(json, OrderIn.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
