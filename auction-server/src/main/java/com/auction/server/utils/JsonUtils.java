package com.auction.server.utils;

import com.google.gson.Gson;

/**
 * Proxy tương thích ngược gọi sang com.auction.common.util.JsonUtils.
 */
public class JsonUtils {

    public static String toJson(Object obj) {
        return com.auction.common.util.JsonUtils.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return com.auction.common.util.JsonUtils.fromJson(json, classOfT);
    }

    public static Gson getGson() {
        return com.auction.common.util.JsonUtils.getGson();
    }
}
