package com.auction.server.service;

import com.auction.common.enums.UserRole;
import com.auction.common.exception.AuthenticationException;
import com.auction.common.model.Admin;
import com.auction.common.model.Bidder;
import com.auction.common.model.Seller;
import com.auction.common.model.User;
import com.auction.server.dao.UserDao;
import com.auction.server.dao.impl.InMemoryUserDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service xử lý xác thực tài khoản và phiên đăng nhập.
 */
public class UserService {
    private final UserDao userDao;
    private final ConcurrentHashMap<String, User> activeSessions = new ConcurrentHashMap<>();

    public UserService() {
        this.userDao = new InMemoryUserDao();
        seedSampleUsers();
    }

    public UserService(UserDao userDao) {
        this.userDao = userDao;
        seedSampleUsers();
    }

    private void seedSampleUsers() {
        // Dữ liệu mẫu khởi tạo
        if (userDao.findByUsername("seller1").isEmpty()) {
            userDao.save(new Seller(null, "seller1", "123456", "Nguyễn Văn Bán", "seller1@auction.vn", 4.9));
        }
        if (userDao.findByUsername("bidder1").isEmpty()) {
            userDao.save(new Bidder(null, "bidder1", "123456", "Trần Đấu Giá 1", "bidder1@auction.vn", 50000000.0));
        }
        if (userDao.findByUsername("bidder2").isEmpty()) {
            userDao.save(new Bidder(null, "bidder2", "123456", "Lê Đấu Giá 2", "bidder2@auction.vn", 70000000.0));
        }
        if (userDao.findByUsername("admin").isEmpty()) {
            userDao.save(new Admin(null, "admin", "admin123", "Quản Trị Viên", "admin@auction.vn", "IT Support"));
        }
    }

    public synchronized User register(String username, String password, String fullName, String email, UserRole role)
            throws AuthenticationException {
        if (userDao.findByUsername(username).isPresent()) {
            throw new AuthenticationException("Tên đăng nhập '" + username + "' đã được sử dụng!");
        }

        User user;
        if (role == UserRole.SELLER) {
            user = new Seller(null, username, password, fullName, email, 5.0);
        } else if (role == UserRole.ADMIN) {
            user = new Admin(null, username, password, fullName, email, "System");
        } else {
            user = new Bidder(null, username, password, fullName, email, 10000000.0);
        }

        return userDao.save(user);
    }

    public String login(String username, String password) throws AuthenticationException {
        Optional<User> opt = userDao.findByUsername(username);
        if (opt.isEmpty() || !opt.get().getPassword().equals(password)) {
            throw new AuthenticationException("Tên đăng nhập hoặc mật khẩu không chính xác!");
        }

        String token = UUID.randomUUID().toString();
        activeSessions.put(token, opt.get());
        return token;
    }

    public User getUserByToken(String token) {
        if (token == null) {
            return null;
        }
        return activeSessions.get(token);
    }

    public void logout(String token) {
        if (token != null) {
            activeSessions.remove(token);
        }
    }

    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    public boolean deleteUser(Long id) {
        return userDao.deleteById(id);
    }

    public UserDao getUserDao() {
        return userDao;
    }
}
