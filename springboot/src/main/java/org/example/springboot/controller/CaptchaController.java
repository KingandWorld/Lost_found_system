package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.example.springboot.common.Result;
import org.example.springboot.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码控制器 — 生成真实图片验证码
 */
@Tag(name = "验证码")
@RestController
@RequestMapping("/captcha")
public class CaptchaController {
    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);

    @Resource
    private SystemConfigService systemConfigService;

    // ==================== 缓存 ====================
    private static final ConcurrentHashMap<String, CaptchaEntry> CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE = 1000;
    private static final int DEFAULT_EXPIRE_SEC = 300;

    // ==================== 频率限制 ====================
    private static final ConcurrentHashMap<String, long[]> IP_WINDOW = new ConcurrentHashMap<>();
    private static final int MAX_PER_MINUTE = 10;

    private static class CaptchaEntry {
        int answer;
        long expireAt;
        CaptchaEntry(int a, long e) { answer = a; expireAt = e; }
    }

    // ==================== API ====================

    @GetMapping("/enabled")
    public Result<Boolean> isEnabled() {
        return Result.success("true".equals(systemConfigService.getConfigValue("captcha.enabled")));
    }

    @GetMapping("/generate")
    public Result<Map<String, Object>> generate(HttpServletRequest request) {
        String ip = getIp(request);
        if (!checkRate(ip)) return Result.error("请求过于频繁，请稍后再试");
        if (CACHE.size() >= MAX_CACHE) cleanExpired();

        // 生成算式
        Random r = new Random();
        int a = r.nextInt(15) + 1;
        int b = r.nextInt(15) + 1;
        int op = r.nextInt(3);
        String expr;
        int answer;
        switch (op) {
            case 0: expr = a + " + " + b + " = ?"; answer = a + b; break;
            case 1:
                if (a < b) { int t = a; a = b; b = t; }
                expr = a + " - " + b + " = ?"; answer = a - b; break;
            default: expr = a + " × " + b + " = ?"; answer = a * b; break;
        }

        String key = UUID.randomUUID().toString().substring(0, 8);
        int expireSec = getExpireSec();
        CACHE.put(key, new CaptchaEntry(answer, System.currentTimeMillis() + expireSec * 1000L));

        // 生成图片
        String base64 = generateImage(expr);

        Map<String, Object> result = new HashMap<>();
        result.put("captchaKey", key);
        result.put("image", "data:image/png;base64," + base64);
        return Result.success(result);
    }

    public static boolean verify(String key, int userAnswer) {
        if (key == null || key.isEmpty()) return false;
        CaptchaEntry e = CACHE.get(key);
        if (e == null) return false;
        if (System.currentTimeMillis() > e.expireAt) { CACHE.remove(key); return false; }
        boolean ok = e.answer == userAnswer;
        if (ok) { CACHE.remove(key); }  // 只有正确才删除，答错允许重试
        return ok;
    }

    // ==================== 图片生成 ====================

    private String generateImage(String text) {
        int w = 160, h = 50;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Random r = new Random();

        // 背景色
        g.setColor(new Color(245 + r.nextInt(10), 245 + r.nextInt(10), 250 + r.nextInt(5)));
        g.fillRect(0, 0, w, h);

        // 干扰线
        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(180 + r.nextInt(60), 180 + r.nextInt(60), 210 + r.nextInt(40), 120));
            g.setStroke(new BasicStroke(1.2f + r.nextFloat()));
            g.drawLine(r.nextInt(w), r.nextInt(h), r.nextInt(w), r.nextInt(h));
        }

        // 干扰点
        for (int i = 0; i < 40; i++) {
            g.setColor(new Color(150 + r.nextInt(90), 150 + r.nextInt(90), 200 + r.nextInt(50)));
            g.fillOval(r.nextInt(w), r.nextInt(h), 2, 2);
        }

        // 文字 — 逐字绘制，每个字独立变换
        char[] chars = text.toCharArray();
        Font baseFont = new Font("Arial", Font.BOLD, 22);
        int totalWidth = 0;
        for (char c : chars) {
            totalWidth += g.getFontMetrics(baseFont).charWidth(c);
        }
        int startX = (w - totalWidth) / 2;

        for (int i = 0; i < chars.length; i++) {
            Font f = baseFont.deriveFont(
                r.nextBoolean() ? Font.BOLD : Font.PLAIN,
                20f + r.nextInt(6)  // 20-25 大小随机
            );
            g.setFont(f);

            // 颜色
            g.setColor(new Color(30 + r.nextInt(60), 50 + r.nextInt(80), 140 + r.nextInt(80)));

            // 旋转
            AffineTransform old = g.getTransform();
            double angle = (r.nextDouble() - 0.5) * 0.3; // ±15°
            int cx = startX + g.getFontMetrics(f).charWidth(chars[i]) / 2;
            int cy = h / 2;
            g.rotate(angle, startX + 8, cy + r.nextInt(6) - 3);
            g.drawString(String.valueOf(chars[i]), startX, 30 + r.nextInt(8));

            // 等宽字符间距都设为~12px（防止不同字符宽度差异导致的布局问题）
            startX += 12;

            g.setTransform(old);
        }

        g.dispose();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", bos);
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception ex) {
            log.error("生成验证码图片失败", ex);
            return "";
        }
    }

    // ==================== 定时清理 ====================

    @Scheduled(fixedRate = 60000)
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        CACHE.entrySet().removeIf(e -> now > e.getValue().expireAt);
        IP_WINDOW.entrySet().removeIf(e -> now - e.getValue()[0] > 60000);
    }

    // ==================== 内部 ====================

    private boolean checkRate(String ip) {
        long now = System.currentTimeMillis();
        long[] window = IP_WINDOW.computeIfAbsent(ip, k -> new long[]{now, 0});
        if (now - window[0] > 60000) { window[0] = now; window[1] = 0; }
        if (++window[1] > MAX_PER_MINUTE) return false;
        return true;
    }

    private int getExpireSec() {
        try {
            int s = Integer.parseInt(systemConfigService.getConfigValue("captcha.expire.seconds"));
            return Math.max(60, Math.min(s, 1800));
        } catch (NumberFormatException e) { return DEFAULT_EXPIRE_SEC; }
    }

    private String getIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = req.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = req.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}
