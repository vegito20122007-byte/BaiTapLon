package com.auction.server.dao.impl;

import com.auction.common.model.User;
import com.auction.common.util.JsonUtils;
import com.auction.server.dao.UserDao;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe Persistent DAO cho User (lưu trữ bộ nhớ và tự động đồng bộ file data/users.json).
 */
public class InMemoryUserDao implements UserDao {
    private static final Logger log = LoggerFactory.getLogger(InMemoryUserDao.class);
    private static final String DATA_FILE = "data/users.json";

    private final Map<Long, User> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public InMemoryUserDao() {
        loadFromFile();
    }

    private synchronized void loadFromFile() {
        try {
            Path path = Paths.get(DATA_FILE);
            if (Files.exists(path)) {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                if (content != null && !content.trim().isEmpty()) {
                    Type listType = new TypeToken<List<User>>() {}.getType();
                    List<User> list = JsonUtils.getGson().fromJson(content, listType);
                    if (list != null) {
                        long maxId = 0;
                        for (User u : list) {
                            if (u.getId() != null) {
                                storage.put(u.getId(), u);
                                if (u.getId() > maxId) {
                                    maxId = u.getId();
                                }
                            }
                        }
                        idGenerator.set(maxId + 1);
                        log.info("Đã nạp {} người dùng từ {}", storage.size(), DATA_FILE);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Không thể đọc dữ liệu người dùng từ {}: {}", DATA_FILE, e.getMessage());
        }
    }

    private synchronized void persistToFile() {
        try {
            Path path = Paths.get(DATA_FILE);
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            List<User> list = new ArrayList<>(storage.values());
            String json = JsonUtils.toJson(list);
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Lỗi lưu dữ liệu người dùng vào {}: {}", DATA_FILE, e.getMessage());
        }
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        storage.put(user.getId(), user);
        persistToFile();
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return storage.values().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean deleteById(Long id) {
        boolean removed = storage.remove(id) != null;
        if (removed) {
            persistToFile();
        }
        return removed;
    }
}
