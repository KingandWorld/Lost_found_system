package org.example.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springboot.common.Result;
import org.example.springboot.entity.FoundItem;
import org.example.springboot.entity.LostItem;
import org.example.springboot.entity.User;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 失物招领系统 API 集成测试
 *
 * 使用 MockMvc 对全部 REST 接口进行测试，覆盖正常流程和异常场景。
 * 注意：测试需要在 Spring Boot 环境下运行，需要数据库连接。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ApiIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** 管理员登录后获取的 Token */
    private static String adminToken;

    /** 普通用户登录后获取的 Token */
    private static String userToken;

    /** 新注册用户的 ID */
    private static Long newUserId;

    // ================================================================
    // 工具方法
    // ================================================================

    /** 从响应 JSON 中提取 code */
    private String extractCode(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("code").asText();
    }

    /** 从响应 JSON 中提取 data 中的指定字段 */
    private String extractDataField(MvcResult result, String field) throws Exception {
        String json = result.getResponse().getContentAsString();
        var data = objectMapper.readTree(json).get("data");
        return data != null && data.has(field) ? data.get(field).asText() : null;
    }

    /** 验证响应 code = "200" */
    private void assertSuccess(MvcResult result) throws Exception {
        Assertions.assertEquals("200", extractCode(result),
                "期望响应 code=200，实际响应: " + result.getResponse().getContentAsString());
    }

    /** 验证响应 code = 错误码 */
    private void assertError(MvcResult result, String expectedCode) throws Exception {
        Assertions.assertEquals(expectedCode, extractCode(result),
                "期望响应 code=" + expectedCode + "，实际响应: " + result.getResponse().getContentAsString());
    }

    // ================================================================
    // 1. 用户管理测试
    // ================================================================

    @Test
    @Order(1)
    @DisplayName("1-1 管理员登录成功")
    void testAdminLogin() throws Exception {
        String body = """
                {"username": "admin", "password": "123456"}
                """;

        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        assertSuccess(result);
        adminToken = extractDataField(result, "token");
        Assertions.assertNotNull(adminToken, "管理员登录后应返回 Token");
        log.info("管理员登录成功，token={}", adminToken.substring(0, 20) + "...");
    }

    @Test
    @Order(2)
    @DisplayName("1-2 普通用户登录成功")
    void testUserLogin() throws Exception {
        String body = """
                {"username": "user1", "password": "123456"}
                """;

        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        assertSuccess(result);
        userToken = extractDataField(result, "token");
        Assertions.assertNotNull(userToken, "普通用户登录后应返回 Token");
    }

    @Test
    @Order(3)
    @DisplayName("1-3 错误密码登录应返回错误")
    void testLoginWrongPassword() throws Exception {
        String body = """
                {"username": "admin", "password": "wrong_password"}
                """;

        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        assertError(result, "-1");
    }

    @Test
    @Order(4)
    @DisplayName("1-4 获取当前用户")
    void testGetCurrentUser() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/user/current")
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();

        assertSuccess(result);
    }

    @Test
    @Order(5)
    @DisplayName("1-5 分页查询用户")
    void testPageUsers() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/user/page")
                        .param("currentPage", "1")
                        .param("size", "10")
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(6)
    @DisplayName("1-6 获取所有用户")
    void testGetAllUsers() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/user/all")
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(7)
    @DisplayName("1-7 根据 ID 查询用户")
    void testGetUserById() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/user/1")
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(8)
    @DisplayName("1-8 按用户名查询")
    void testGetUserByUsername() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/user/username/admin")
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(9)
    @DisplayName("1-9 注册新用户")
    void testRegisterUser() throws Exception {
        String ts = String.valueOf(System.currentTimeMillis());
        String body = String.format("""
                {
                    "username": "junit_test_%s",
                    "password": "123456",
                    "name": "JUnit测试用户",
                    "phone": "13800138002",
                    "email": "junit_%s@example.com"
                }
                """, ts, ts);

        MvcResult result = mockMvc.perform(post("/api/user/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(10)
    @DisplayName("1-10 重复用户名注册应失败")
    void testRegisterDuplicateUser() throws Exception {
        // admin 已存在，再次注册应失败
        String body = """
                {
                    "username": "admin",
                    "password": "123456",
                    "name": "重复管理员",
                    "phone": "13800138003",
                    "email": "admin_dup@example.com"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/user/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        assertError(result, "-1");
    }

    @Test
    @Order(11)
    @DisplayName("1-11 更新用户信息")
    void testUpdateUser() throws Exception {
        String body = """
                {"name": "管理员-已更新", "phone": "13900139099"}
                """;

        MvcResult result = mockMvc.perform(put("/api/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    // ================================================================
    // 2. 物品分类管理测试
    // ================================================================

    @Test
    @Order(20)
    @DisplayName("2-1 获取分类列表（公开）")
    void testGetCategoryList() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/category/list"))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(21)
    @DisplayName("2-2 分页查询分类（公开）")
    void testPageCategory() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/category/page")
                        .param("currentPage", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(22)
    @DisplayName("2-3 根据 ID 查询分类（公开）")
    void testGetCategoryById() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/category/1"))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(23)
    @DisplayName("2-4 添加分类")
    void testAddCategory() throws Exception {
        String body = """
                {"name": "JUnit测试分类", "sort": 99}
                """;

        MvcResult result = mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    // ================================================================
    // 3. 失物信息管理测试
    // ================================================================

    @Test
    @Order(30)
    @DisplayName("3-1 分页查询失物（公开）")
    void testPageLostItem() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/lost-item/page")
                        .param("currentPage", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(31)
    @DisplayName("3-2 查询失物详情（公开）")
    void testGetLostItemById() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/lost-item/1"))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(32)
    @DisplayName("3-3 获取统计数据（公开）")
    void testGetLostItemStatistics() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/lost-item/statistics"))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(33)
    @DisplayName("3-4 发布失物（管理员）")
    void testAddLostItem() throws Exception {
        String body = """
                {
                    "title": "JUnit测试-丢失的平板",
                    "description": "在图书馆自习室丢失一台iPad，深空灰色，配有蓝色保护壳，描述足够详细以通过验证。",
                    "categoryId": 2,
                    "lostPlace": "图书馆五楼",
                    "lostTime": "2026-06-28T14:30:00",
                    "contactName": "管理员",
                    "contactPhone": "13800138001"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/lost-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(34)
    @DisplayName("3-5 发布失物（标题过短-应失败）")
    void testAddLostItemShortTitle() throws Exception {
        String body = """
                {
                    "title": "短",
                    "description": "这是一段足够长的描述文字，用于测试标题过短的验证逻辑。",
                    "categoryId": 2,
                    "lostPlace": "测试地点",
                    "lostTime": "2026-06-28T14:30:00",
                    "contactName": "测试员",
                    "contactPhone": "13800138001"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/lost-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertError(result, "-1");
    }

    @Test
    @Order(35)
    @DisplayName("3-6 发布失物（描述过短-应失败）")
    void testAddLostItemShortDescription() throws Exception {
        String body = """
                {
                    "title": "完整标题测试",
                    "description": "短",
                    "categoryId": 2,
                    "lostPlace": "测试地点",
                    "lostTime": "2026-06-28T14:30:00",
                    "contactName": "测试员",
                    "contactPhone": "13800138001"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/lost-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertError(result, "-1");
    }

    @Test
    @Order(36)
    @DisplayName("3-7 发布失物（无效手机号-应失败）")
    void testAddLostItemInvalidPhone() throws Exception {
        String body = """
                {
                    "title": "完整标题测试",
                    "description": "这是一段足够长的描述文字，用于测试手机号验证。",
                    "categoryId": 2,
                    "lostPlace": "测试地点",
                    "lostTime": "2026-06-28T14:30:00",
                    "contactName": "测试员",
                    "contactPhone": "12345"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/lost-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertError(result, "-1");
    }

    @Test
    @Order(37)
    @DisplayName("3-8 按分类筛选失物")
    void testFilterLostByCategory() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/lost-item/page")
                        .param("categoryId", "1")
                        .param("currentPage", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(38)
    @DisplayName("3-9 按状态筛选失物")
    void testFilterLostByStatus() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/lost-item/page")
                        .param("status", "0")
                        .param("currentPage", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    // ================================================================
    // 4. 招领信息管理测试
    // ================================================================

    @Test
    @Order(40)
    @DisplayName("4-1 分页查询招领（公开）")
    void testPageFoundItem() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/found-item/page")
                        .param("currentPage", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(41)
    @DisplayName("4-2 查询招领详情（公开）")
    void testGetFoundItemById() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/found-item/1"))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(42)
    @DisplayName("4-3 获取我的招领信息")
    void testMyFoundItems() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/found-item/my")
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(43)
    @DisplayName("4-4 发布招领信息（普通用户）")
    void testAddFoundItem() throws Exception {
        String body = """
                {
                    "title": "JUnit测试-捡到充电宝",
                    "description": "在食堂捡到白色充电宝一个，2万毫安时，请失主联系认领。",
                    "categoryId": 4,
                    "foundPlace": "第三食堂二楼",
                    "foundTime": "2026-06-28T12:00:00",
                    "contactName": "李四",
                    "contactPhone": "13900139001"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/found-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("token", userToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(44)
    @DisplayName("4-5 按标题搜索招领")
    void testSearchFoundByTitle() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/found-item/page")
                        .param("title", "手机")
                        .param("currentPage", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    // ================================================================
    // 5. 认领申请管理测试
    // ================================================================

    @Test
    @Order(50)
    @DisplayName("5-1 查询我的申请")
    void testMyClaimApplications() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/claim/my")
                        .param("currentPage", "1")
                        .param("size", "10")
                        .header("token", userToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(51)
    @DisplayName("5-2 查询待审核申请")
    void testMyAuditApplications() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/claim/audit")
                        .param("currentPage", "1")
                        .param("size", "10")
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(52)
    @DisplayName("5-3 管理员查询全部申请")
    void testAllClaimApplications() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/claim/page")
                        .param("currentPage", "1")
                        .param("size", "10")
                        .header("token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    // ================================================================
    // 6. 通知管理测试
    // ================================================================

    @Test
    @Order(60)
    @DisplayName("6-1 获取通知列表")
    void testGetNotifications() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/notification/list")
                        .param("currentPage", "1")
                        .param("size", "10")
                        .header("token", userToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(61)
    @DisplayName("6-2 获取未读通知数")
    void testGetUnreadCount() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/notification/unread-count")
                        .header("token", userToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    @Test
    @Order(62)
    @DisplayName("6-3 标记全部已读")
    void testMarkAllRead() throws Exception {
        MvcResult result = mockMvc.perform(put("/api/notification/read-all")
                        .header("token", userToken))
                .andExpect(status().isOk())
                .andReturn();
        assertSuccess(result);
    }

    // ================================================================
    // 7. 异常场景测试
    // ================================================================

    @Test
    @Order(70)
    @DisplayName("7-1 无 Token 访问需认证接口应返回 401")
    void testNoTokenAccess() throws Exception {
        mockMvc.perform(get("/api/user/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(71)
    @DisplayName("7-2 无效 Token 访问应返回 401")
    void testInvalidToken() throws Exception {
        mockMvc.perform(get("/api/user/current")
                        .header("token", "invalid_token_12345"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(72)
    @DisplayName("7-3 查询不存在的失物ID")
    void testNotFoundLostItem() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/lost-item/99999"))
                .andExpect(status().isOk())
                .andReturn();
        assertError(result, "-1");
    }

    @Test
    @Order(73)
    @DisplayName("7-4 查询不存在的招领ID")
    void testNotFoundFoundItem() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/found-item/99999"))
                .andExpect(status().isOk())
                .andReturn();
        assertError(result, "-1");
    }

    @Test
    @Order(74)
    @DisplayName("7-5 空参数登录")
    void testEmptyLogin() throws Exception {
        String body = """
                {"username": "", "password": ""}
                """;

        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        assertError(result, "-1");
    }
}
