package com.auction.server;

import com.auction.common.enums.UserRole;
import com.auction.common.exception.AuthenticationException;
import com.auction.common.model.User;
import com.auction.server.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
    }

    @Test
    @DisplayName("Đăng ký tài khoản mới thành công")
    void testRegisterSuccess() throws Exception {
        String username = "new_bidder_" + System.currentTimeMillis();
        User user = userService.register(username, "pass123", "Người Đấu Giá Mới", "test@auction.vn", UserRole.BIDDER);

        assertNotNull(user);
        assertNotNull(user.getId());
        assertEquals(username, user.getUsername());
        assertEquals(UserRole.BIDDER, user.getRole());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi đăng ký trùng tên đăng nhập")
    void testRegisterDuplicateUsernameThrowsException() {
        assertThrows(AuthenticationException.class, () -> {
            userService.register("seller1", "pwd", "Trùng Tên", "email@test.com", UserRole.SELLER);
        });
    }

    @Test
    @DisplayName("Đăng nhập thành công trả về token phiên")
    void testLoginSuccess() throws Exception {
        String token = userService.login("bidder1", "123456");
        assertNotNull(token);

        User loggedInUser = userService.getUserByToken(token);
        assertNotNull(loggedInUser);
        assertEquals("bidder1", loggedInUser.getUsername());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi đăng nhập sai mật khẩu hoặc tài khoản không tồn tại")
    void testLoginFailureThrowsException() {
        assertThrows(AuthenticationException.class, () -> {
            userService.login("bidder1", "wrong_pass");
        });

        assertThrows(AuthenticationException.class, () -> {
            userService.login("non_existing_user", "123456");
        });
    }

    @Test
    @DisplayName("Đăng xuất thành công thu hồi token")
    void testLogout() throws Exception {
        String token = userService.login("seller1", "123456");
        assertNotNull(userService.getUserByToken(token));

        userService.logout(token);
        assertNull(userService.getUserByToken(token));
    }
}
