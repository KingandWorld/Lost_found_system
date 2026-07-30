package org.example.springboot.config;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web配置类，用于自定义Spring MVC的行为
 * 主要功能：
 * 1. 配置全局API路径前缀
 * 2. 配置JWT拦截器及其路径规则
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String API_PREFIX = "/api";

    @Value("${file.base-path:./files}")
    private String fileBasePath;
    
    // 定义不需要JWT验证的路径
    private static final String[] PUBLIC_PATHS = {
        "/api/user/login",      // 登录接口
        "/api/user/forget",     // 忘记密码接口
        "/api/user/add",        // 用户注册接口
        "/api/user/sendCode",   // 发送验证码
        "/api/user/{id}",       // 用户信息查询接口
        "/api/lost-item/page",  // 失物列表（公开浏览）
        "/api/lost-item/{id}",  // 失物详情（公开浏览）
        "/api/lost-item/statistics", // 失物统计
        "/api/found-item/page", // 招领列表（公开浏览）
        "/api/found-item/{id}", // 招领详情（公开浏览）
        "/api/category/list",   // 分类列表（公开浏览）
        "/api/category/{id}",   // 分类详情（公开浏览）
        "/api/email/**",        // 邮件相关接口
        "/api/img/**",          // 图片资源接口（带前缀）
        "/img/**",              // 图片资源接口（不带前缀）
        "/api/file/**",         // 文件资源接口（带前缀）
        "/file/**",             // 文件资源接口（不带前缀）
        
        // Swagger和API文档相关路径
        "/api/v3/api-docs/**",
        "/api/swagger-ui.html",
        "/api/swagger-ui/**",
        "/api/doc.html",
        "/api/webjars/**",
        "/api/favicon.ico"
    };

    /**
     * 配置文件系统静态资源映射
     * 将 /img/**、/file/** 及其 /api 前缀变体映射到文件系统上的存储目录，
     * 使上传的头像、图片等文件可以通过 URL 直接访问。
     *
     * 映射规则（以 fileBasePath=/www/wwwroot/lost-found/files 为例）：
     *   /api/img/xxx.jpg → files/img/xxx.jpg
     *   /img/xxx.jpg     → files/img/xxx.jpg
     *   /api/file/xxx    → files/file/xxx
     *   /file/xxx        → files/file/xxx
     *
     * ⚠️ 注意：Spring 会剥离 URL 中的 /img/ 或 /api/img/ 前缀，
     *   然后将剩余路径拼到 resource location 后面，
     *   因此 location 必须包含 img/ 或 file/ 子目录。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path absolutePath = Paths.get(fileBasePath).toAbsolutePath().normalize();
        String base = absolutePath.toString().replace("\\", "/");
        if (!base.endsWith("/")) {
            base += "/";
        }

        // 图片资源：/img/** 和 /api/img/** → files/img/
        String imgLocation = "file:" + base + "img/";
        registry.addResourceHandler("/img/**", "/api/img/**")
                .addResourceLocations(imgLocation);

        // 其他文件：/file/** 和 /api/file/** → files/file/
        String fileLocation = "file:" + base + "file/";
        registry.addResourceHandler("/file/**", "/api/file/**")
                .addResourceLocations(fileLocation);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 为带有RestController注解的类添加"/api"路径前缀
        // 排除 Knife4j/Swagger 相关的接口（通过包名判断）
        configurer.addPathPrefix("/api", clazz ->
                clazz.isAnnotationPresent(RestController.class) &&
                        !clazz.getPackage().getName().contains("springfox") &&
                        !clazz.getPackage().getName().contains("swagger")&&!clazz.getPackage().getName().contains("doc")
        );
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT认证已移至Spring Security过滤器中处理
        // 此处可以添加其他非认证相关的拦截器
    }
}
