package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;

import org.example.springboot.entity.User;
import org.example.springboot.DTO.UserPasswordUpdateDTO;
import org.example.springboot.enumClass.AccountStatus;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Resource
    private UserMapper userMapper;
    

    
    @Value("${user.defaultPassword}")
    private String DEFAULT_PWD;

    @Resource
    private PasswordEncoder passwordEncoder;

    public User getByEmail(String email) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new ServiceException("邮箱不存在");
        }
        return user;
    }

    public User login(User user) {
        String account = user.getUsername();

        // 检查是否被登录锁定（按输入的账号和实际用户名双重检查）
        Long lockEndTime = LOGIN_LOCK_TIME.get(account);
        if (lockEndTime != null) {
            if (System.currentTimeMillis() < lockEndTime) {
                long remainingMinutes = (lockEndTime - System.currentTimeMillis()) / 1000 / 60 + 1;
                throw new ServiceException("账户已被锁定，请在 " + remainingMinutes + " 分钟后重试");
            } else {
                // 锁定已过期，清除锁定状态
                LOGIN_LOCK_TIME.remove(account);
                LOGIN_FAIL_COUNT.remove(account);
            }
        }

        User dbUser;
        // 判断输入是邮箱还是用户名，优先尝试用户名登录，失败后尝试邮箱登录
        boolean isEmail = account != null && account.contains("@");
        try {
            dbUser = getByUsername(account);
        } catch (ServiceException e) {
            if (isEmail) {
                // 用户名查找失败，尝试邮箱登录
                try {
                    dbUser = getByEmail(account);
                    // 邮箱登录成功，修正锁定检查用的key
                    account = dbUser.getUsername();
                } catch (ServiceException e2) {
                    // 邮箱也不存在，统一返回用户名或密码错误
                    recordLoginFailure(account);
                    throw new ServiceException("用户名或密码错误");
                }
            } else {
                // 用户名查找失败且不是邮箱，不暴露用户是否存在
                recordLoginFailure(account);
                throw new ServiceException("用户名或密码错误");
            }
        }

        // 验证密码
        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            recordLoginFailure(account);
            int remainingAttempts = MAX_LOGIN_FAILS - LOGIN_FAIL_COUNT.getOrDefault(account, 0);
            if (remainingAttempts > 0) {
                throw new ServiceException("用户名或密码错误，还剩 " + remainingAttempts + " 次尝试机会");
            } else {
                throw new ServiceException("用户名或密码错误，账户已被锁定");
            }
        }

        // 登录成功，清除失败记录（按输入账号和实际用户名）
        LOGIN_FAIL_COUNT.remove(account);
        LOGIN_LOCK_TIME.remove(account);
        LOGIN_FAIL_COUNT.remove(dbUser.getUsername());
        LOGIN_LOCK_TIME.remove(dbUser.getUsername());

        // 完善的账户状态检查
        AccountStatus accountStatus = AccountStatus.fromValue(dbUser.getStatus());
        if (!accountStatus.canLogin()) {
            throw new ServiceException(accountStatus.getDescription() + ": " + accountStatus.getTransitionAdvice());
        }

        // 生成token
        String token = JwtTokenUtils.genToken(String.valueOf(dbUser.getId()), dbUser.getPassword());
        dbUser.setToken(token);

        // 记录登录时间（可选）
        // updateLastLoginTime(dbUser.getId());

        return dbUser;
    }

    /**
     * 记录登录失败，达到最大次数后锁定账户
     */
    private void recordLoginFailure(String username) {
        int fails = LOGIN_FAIL_COUNT.getOrDefault(username, 0) + 1;
        LOGIN_FAIL_COUNT.put(username, fails);
        if (fails >= MAX_LOGIN_FAILS) {
            LOGIN_LOCK_TIME.put(username, System.currentTimeMillis() + LOGIN_LOCK_DURATION_MS);
            LOGGER.warn("账户 {} 因登录失败 {} 次已被锁定 {} 分钟", username, fails, LOGIN_LOCK_DURATION_MS / 1000 / 60);
        }
    }

    public List<User> getUserByRole(String roleCode) {
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getRoleCode, roleCode)
        );
        if (users.isEmpty()) {
            throw new ServiceException("未找到该角色的用户");
        }
        return users;
    }

    public void createUser(User user) {
        // 检查用户名是否存在
        if (userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, user.getUsername())
            ) != null) {
            throw new ServiceException("用户名已存在");
        }
        
        // 检查邮箱是否被使用
        if (userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, user.getEmail())
            ) != null) {
            throw new ServiceException("邮箱已被使用");
        }


        user.setPassword(StringUtils.isNotBlank(user.getPassword()) ? user.getPassword() : DEFAULT_PWD);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        if (userMapper.insert(user) <= 0) {
            throw new ServiceException("用户创建失败");
        }
    }

    /**
     * 更新用户状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status, String reason) {
        User user = getUserById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        // 验证状态值
        AccountStatus newStatus = AccountStatus.fromValue(status);
        AccountStatus currentStatus = AccountStatus.fromValue(user.getStatus());

        // 检查状态转换是否合法
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new ServiceException("不允许从 " + currentStatus.getDescription() + " 转换到 " + newStatus.getDescription());
        }

        user.setStatus(status);
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("状态更新失败");
        }

        // 记录状态变更日志（可选）
        // logStatusChange(userId, currentStatus, newStatus, reason);
    }

    /**
     * 检查用户状态转换是否合法
     */
    private boolean isValidStatusTransition(AccountStatus from, AccountStatus to) {
        switch (from) {
            case PENDING_REVIEW:
                return to == AccountStatus.ACTIVE || to == AccountStatus.REVIEW_FAILED;
            case REVIEW_FAILED:
                return to == AccountStatus.PENDING_REVIEW || to == AccountStatus.DISABLED;
            case ACTIVE:
                return to == AccountStatus.DISABLED || to == AccountStatus.LOCKED || to == AccountStatus.EXPIRED;
            case DISABLED:
                return to == AccountStatus.ACTIVE;
            case LOCKED:
                return to == AccountStatus.ACTIVE || to == AccountStatus.DISABLED;
            case EXPIRED:
                return to == AccountStatus.ACTIVE;
            default:
                return false;
        }
    }

    /**
     * 检查用户是否可以执行操作
     */
    public void checkUserCanOperate(User user) {
        if (user == null) {
            throw new ServiceException("用户未登录");
        }

        AccountStatus status = AccountStatus.fromValue(user.getStatus());
        if (!status.canOperate()) {
            throw new ServiceException("账户状态异常，无法执行操作: " + status.getTransitionAdvice());
        }
    }

    public void updateUser(Long id, User user) {
        // 检查用户是否存在
        if (userMapper.selectById(id) == null) {
            throw new ServiceException("要更新的用户不存在");
        }
        
        // 检查新用户名是否与其他用户重复
        if (user.getUsername() != null) {
            User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, user.getUsername())
            );
            if (existUser != null && !existUser.getId().equals(id)) {
                throw new ServiceException("新用户名已被使用");
            }
        }
        
        user.setId(id);
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("用户更新失败");
        }
    }

    public User getByUsername(String username) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        );
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    public void deleteBatch(List<Integer> ids) {
        if (userMapper.deleteByIds(ids) <= 0) {
            throw new ServiceException("批量删除失败");
        }
    }

    public List<User> getUserList() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<>());
        if (users.isEmpty()) {
            throw new ServiceException("未找到任何用户");
        }
        return users;
    }

    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    public Page<User> getUsersByPage(String username,  String name, String roleCode, Integer currentPage, Integer size) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like(User::getUsername, username);
        }

        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like(User::getName, name);
        }
        if (StringUtils.isNotBlank(roleCode)) {
            queryWrapper.eq(User::getRoleCode, roleCode);
        }
        
        return userMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }

    public void updatePassword(Long id, UserPasswordUpdateDTO update) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(update.getOldPassword(), user.getPassword())) {
            throw new ServiceException("原密码错误");
        }

        // 新旧密码不能相同
        if (passwordEncoder.matches(update.getNewPassword(), user.getPassword())) {
            throw new ServiceException("新密码不能与旧密码相同");
        }

        // 更新新密码
        user.setPassword(passwordEncoder.encode(update.getNewPassword()));
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("密码修改失败");
        }
    }

    /**
     * 验证码缓存（邮箱 -> 验证码）
     * 生产环境应替换为 Redis 等分布式缓存
     */
    private static final Map<String, String> VERIFICATION_CODE_CACHE = new ConcurrentHashMap<>();

    /**
     * 验证码有效期（毫秒）
     */
    private static final long CODE_EXPIRATION_MS = 5 * 60 * 1000; // 5分钟

    /**
     * 验证码发送时间缓存（邮箱 -> 发送时间戳）
     */
    private static final Map<String, Long> CODE_SEND_TIME_CACHE = new ConcurrentHashMap<>();

    /**
     * 登录失败次数缓存（用户名 -> 失败次数）
     */
    private static final Map<String, Integer> LOGIN_FAIL_COUNT = new ConcurrentHashMap<>();

    /**
     * 登录锁定时间缓存（用户名 -> 锁定截止时间戳）
     */
    private static final Map<String, Long> LOGIN_LOCK_TIME = new ConcurrentHashMap<>();

    /**
     * 最大登录失败次数
     */
    private static final int MAX_LOGIN_FAILS = 5;

    /**
     * 登录锁定时间（毫秒）
     */
    private static final long LOGIN_LOCK_DURATION_MS = 15 * 60 * 1000; // 15分钟

    /**
     * 发送密码重置验证码到用户邮箱
     * @param email 用户邮箱
     */
    public void sendVerificationCode(String email) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new ServiceException("该邮箱未注册");
        }

        // 防止频繁发送（60秒内不能重复发送）
        Long lastSendTime = CODE_SEND_TIME_CACHE.get(email);
        if (lastSendTime != null && System.currentTimeMillis() - lastSendTime < 60 * 1000) {
            throw new ServiceException("验证码已发送，请60秒后再试");
        }

        // 生成6位随机验证码
        String code = String.format("%06d", new Random().nextInt(999999));
        VERIFICATION_CODE_CACHE.put(email, code);
        CODE_SEND_TIME_CACHE.put(email, System.currentTimeMillis());

        // TODO: 接入真实邮件服务发送验证码
        // 生产环境中应替换为实际的邮件发送逻辑
        LOGGER.info("发送密码重置验证码到邮箱 {}: {}", email, code);

        // 开发环境下将验证码打印到日志，方便测试
        System.out.println("=============================================");
        System.out.println("【密码重置验证码】邮箱: " + email + "，验证码: " + code + "（有效期5分钟）");
        System.out.println("=============================================");
    }

    public void forgetPassword(String email, String newPassword, String verificationCode) {
        // 验证验证码
        String cachedCode = VERIFICATION_CODE_CACHE.get(email);
        if (cachedCode == null) {
            throw new ServiceException("请先获取验证码");
        }
        if (!cachedCode.equals(verificationCode)) {
            throw new ServiceException("验证码错误");
        }

        // 验证码过期检查
        Long sendTime = CODE_SEND_TIME_CACHE.get(email);
        if (sendTime != null && System.currentTimeMillis() - sendTime > CODE_EXPIRATION_MS) {
            VERIFICATION_CODE_CACHE.remove(email);
            CODE_SEND_TIME_CACHE.remove(email);
            throw new ServiceException("验证码已过期，请重新获取");
        }

        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
        );
        if (user == null) {
            throw new ServiceException("邮箱不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("密码重置失败");
        }

        // 重置成功后清除验证码
        VERIFICATION_CODE_CACHE.remove(email);
        CODE_SEND_TIME_CACHE.remove(email);
    }

    public void deleteUserById(Long id) {
        if (userMapper.deleteById(id) <= 0) {
            throw new ServiceException("删除失败");
        }
    }

    /**
     * 管理员重置用户密码（无需验证码）
     * @param userId 用户ID
     * @param newPassword 新密码
     */
    public void adminResetPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("密码重置失败");
        }
    }
}
