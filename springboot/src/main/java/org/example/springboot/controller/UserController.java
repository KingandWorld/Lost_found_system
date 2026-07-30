package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.User;
import org.example.springboot.DTO.UserPasswordUpdateDTO;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.service.SystemConfigService;
import org.example.springboot.service.UserService;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name="用户管理接口")
@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    @Resource
    private UserService userService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private SystemConfigService systemConfigService;

    @Operation(summary = "根据id获取用户信息")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        // 如果用户不存在会抛出异常，由全局异常处理器处理
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    @Operation(summary = "根据username获取用户信息")
    @GetMapping("/username/{username}")
    public Result<?> getUserByUsername(@PathVariable String username) {
        // 不存在的用户会抛出异常
        User user = userService.getByUsername(username);
        return Result.success(user);
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, Object> params) {
        // 验证码检查
        if ("true".equals(systemConfigService.getConfigValue("captcha.enabled"))) {
            String captchaKey = (String) params.get("captchaKey");
            Object answerObj = params.get("captchaAnswer");
            if (captchaKey == null || answerObj == null) {
                return Result.error("请输入验证码");
            }
            int userAnswer = answerObj instanceof Integer ? (Integer) answerObj : Integer.parseInt(answerObj.toString());
            if (!CaptchaController.verify(captchaKey, userAnswer)) {
                return Result.error("验证码错误或已过期");
            }
        }

        User user = new User();
        user.setUsername((String) params.get("username"));
        user.setPassword((String) params.get("password"));
        User loginUser = userService.login(user);
        return Result.success(loginUser);
    }

    @Operation(summary = "密码修改")
    @PutMapping("/password/{id}")
    public Result<?> updatePassword(@PathVariable Long id, @RequestBody UserPasswordUpdateDTO userPasswordUpdate) {
        // 密码修改失败会抛出异常
        userService.updatePassword(id, userPasswordUpdate);
        return Result.success("密码修改成功");
    }

    @Operation(summary = "发送密码重置验证码")
    @GetMapping("/sendCode")
    public Result<?> sendVerificationCode(@RequestParam String email) {
        try {
            userService.sendVerificationCode(email);
            return Result.success("验证码已发送到您的邮箱，请查收（开发环境请在控制台查看）");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "忘记密码（需提供邮箱验证码）")
    @PostMapping("/forget")
    public Result<?> forgetPassword(@RequestParam String email, @RequestParam String newPassword, @RequestParam String verificationCode) {
        try {
            userService.forgetPassword(email, newPassword, verificationCode);
            return Result.success("密码重置成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Result<?> getUsersByPage(
            @RequestParam(defaultValue = "") String username,

            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String roleCode,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<User> page = userService.getUsersByPage(username,  name, roleCode,  currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "根据角色获取用户列表")
    @GetMapping("/role/{roleCode}")
    public Result<?> getUserByRole(@PathVariable String roleCode) {
        List<User> users = userService.getUserByRole(roleCode);
        return Result.success(users);
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/deleteBatch")
    public Result<?> deleteBatch(@RequestParam List<Integer> ids) {
        userService.deleteBatch(ids);
        return Result.success("批量删除成功");
    }

    @Operation(summary = "获取所有用户")
    @GetMapping("/all")
    public Result<?> getUserList() {
        List<User> list = userService.getUserList();
        return Result.success(list);
    }

    @Operation(summary = "创建新用户")
    @PostMapping("/add")
    public Result<?> createUser(@RequestBody Map<String, Object> params) {
        // 验证码检查
        if ("true".equals(systemConfigService.getConfigValue("captcha.enabled"))) {
            String captchaKey = (String) params.get("captchaKey");
            Object answerObj = params.get("captchaAnswer");
            if (captchaKey == null || answerObj == null) {
                return Result.error("请输入验证码");
            }
            int userAnswer = answerObj instanceof Integer ? (Integer) answerObj : Integer.parseInt(answerObj.toString());
            if (!CaptchaController.verify(captchaKey, userAnswer)) {
                return Result.error("验证码错误或已过期");
            }
        }

        // 检查用户协议是否勾选
        Object agreementObj = params.get("agreementAccepted");
        boolean agreementAccepted = agreementObj instanceof Boolean ? (Boolean) agreementObj :
                                     (agreementObj instanceof String ? "true".equals(agreementObj) : false);
        if (!agreementAccepted) {
            return Result.error("请阅读并同意用户协议");
        }

        User user = new User();
        user.setUsername((String) params.get("username"));
        user.setPassword((String) params.get("password"));
        user.setEmail((String) params.get("email"));
        user.setPhone((String) params.get("phone"));
        user.setName((String) params.get("name"));
        user.setRoleCode((String) params.getOrDefault("roleCode", "USER"));
        userService.createUser(user);
        return Result.success("创建成功");
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        // 更新失败会抛出具体原因的异常
        userService.updateUser(id, user);
        return Result.success("更新成功");
    }

    @Operation(summary = "根据id删除用户")
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteUserById(@PathVariable Long id) {
        // 删除失败会抛出异常
        userService.deleteUserById(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/current")
    public Result<?> getCurrentUser() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("获取当前用户信息失败，请重新登录");
        }
        return Result.success(currentUser);
    }
    @Operation(summary = "修改用户状态")
    @PutMapping("/status/{userId}")
    public Result<?> updateStatus(@PathVariable Long userId, @RequestParam Integer status) {
        User user = userMapper.selectById(userId);
        user.setStatus(status);
        userService.updateUser(userId,user);
        return Result.success();

    }

    @Operation(summary = "管理员重置用户密码（无需验证码）")
    @PutMapping("/admin-reset-password/{id}")
    public Result<?> adminResetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        try {
            userService.adminResetPassword(id, newPassword);
            return Result.success("密码重置成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
