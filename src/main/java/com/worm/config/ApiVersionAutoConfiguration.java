package com.worm.config;

import com.worm.api.ApiVersionRequestMappingHandlerMapping;
import com.worm.properties.ApiVersionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * API版本控制自动配置类
 * <p>
 * 自动配置 API 版本控制功能，注册自定义的 RequestMappingHandlerMapping，
 * 使得控制器可以使用 @ApiVersion 注解来声明接口版本。
 * </p>
 *
 * <h3>生效条件：</h3>
 * <ul>
 *     <li>当前应用为 Web 应用（Servlet 环境）</li>
 *     <li>配置项 worm.api.version.enable = true（默认 true，缺失时生效）</li>
 * </ul>
 *
 * <h3>配置示例：</h3>
 * <pre>
 * # application.yml
 * worm:
 *   api:
 *     version:
 *       enable: true                      # 启用版本控制
 *       version: V1.0.0                   # 默认版本号
 *       version-type: header              # 版本传递方式：header/path
 *       header-key: X-API-VERSION         # 头部方式的key
 *       scan-package:                     # 扫描的包路径
 *         - com.example.controller
 * </pre>
 *
 * <h3>功能特性：</h3>
 * <ul>
 *     <li>支持类级别和方法级别的版本声明</li>
 *     <li>支持通过路径或请求头传递版本号</li>
 *     <li>支持版本号比较和匹配</li>
 * </ul>
 *
 * @author worm
 * @see ApiVersionProperties
 * @see ApiVersionRequestMappingHandlerMapping
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({ApiVersionProperties.class})
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "worm.api.version", name = "enable", havingValue = "true", matchIfMissing = true)
public class ApiVersionAutoConfiguration implements WebMvcRegistrations {

    /**
     * API版本配置属性
     */
    private final ApiVersionProperties apiVersionProperties;

    /**
     * 构造函数
     *
     * @param apiVersionProperties API版本配置属性
     */
    public ApiVersionAutoConfiguration(ApiVersionProperties apiVersionProperties) {
        this.apiVersionProperties = apiVersionProperties;
    }

    /**
     * 创建自定义的RequestMappingHandlerMapping
     * <p>
     * 返回 ApiVersionRequestMappingHandlerMapping 实例，用于处理 API 版本控制逻辑。
     * 该方法会在 Spring MVC 初始化时被调用，替换默认的 RequestMappingHandlerMapping。
     * </p>
     *
     * @return 自定义的API版本控制请求映射处理器
     */
    @Override
    public ApiVersionRequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        log.info("【ApiVersionAutoConfiguration】初始化API版本控制组件：ApiVersionRequestMappingHandlerMapping");
        log.info("【ApiVersionAutoConfiguration】API版本控制功能已启用，支持通过@ApiVersion注解声明接口版本");

        return new ApiVersionRequestMappingHandlerMapping(apiVersionProperties);
    }
}
