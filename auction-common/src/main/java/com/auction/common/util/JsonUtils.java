package com.auction.common.util;

import com.auction.common.enums.UserRole;
import com.auction.common.model.Admin;
import com.auction.common.model.ArtItem;
import com.auction.common.model.Bidder;
import com.auction.common.model.ElectronicsItem;
import com.auction.common.model.Item;
import com.auction.common.model.Seller;
import com.auction.common.model.User;
import com.auction.common.model.VehicleItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tiện ích tuần tự hóa / giải tuần tự JSON tập trung cho cả Client và Server.
 * Hỗ trợ chuyển đổi LocalDateTime và Đa hình (Polymorphism) cho User và Item.
 */
public class JsonUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final Gson GSON = new GsonBuilder()
            // TypeAdapter cho LocalDateTime (Khắc phục lỗi Reflection trên Java 17+)
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    src == null ? null : new JsonPrimitive(src.format(FORMATTER)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> {
                if (json == null || json.isJsonNull()) {
                    return null;
                }
                String val = json.getAsString();
                if (val == null || val.trim().isEmpty()) {
                    return null;
                }
                return LocalDateTime.parse(val, FORMATTER);
            })
            // TypeAdapter đa hình cho User (abstract class -> Bidder / Seller / Admin)
            .registerTypeAdapter(User.class, (JsonDeserializer<User>) (json, typeOfT, context) -> {
                if (json == null || json.isJsonNull()) {
                    return null;
                }
                JsonObject obj = json.getAsJsonObject();
                String roleStr = obj.has("role") ? obj.get("role").getAsString() : "BIDDER";
                try {
                    UserRole role = UserRole.valueOf(roleStr.toUpperCase());
                    switch (role) {
                        case SELLER:
                            return context.deserialize(json, Seller.class);
                        case ADMIN:
                            return context.deserialize(json, Admin.class);
                        default:
                            return context.deserialize(json, Bidder.class);
                    }
                } catch (Exception e) {
                    return context.deserialize(json, Bidder.class);
                }
            })
            // TypeAdapter đa hình cho Item (abstract class -> ElectronicsItem / ArtItem / VehicleItem)
            .registerTypeAdapter(Item.class, (JsonDeserializer<Item>) (json, typeOfT, context) -> {
                if (json == null || json.isJsonNull()) {
                    return null;
                }
                JsonObject obj = json.getAsJsonObject();
                String category = obj.has("category") ? obj.get("category").getAsString() : "ELECTRONICS";
                switch (category.toUpperCase()) {
                    case "ART":
                        return context.deserialize(json, ArtItem.class);
                    case "VEHICLE":
                        return context.deserialize(json, VehicleItem.class);
                    default:
                        return context.deserialize(json, ElectronicsItem.class);
                }
            })
            .create();

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return GSON.fromJson(json, classOfT);
    }

    public static Gson getGson() {
        return GSON;
    }
}
