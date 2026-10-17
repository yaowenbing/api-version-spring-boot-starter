package com.worm.api;

import com.worm.properties.ApiVersionProperties;

import java.lang.annotation.*;

/**
 * API版本控制注解
 * <p>
 * 用于标识接口的版本号，支持在类或方法上使用。
 * 当标注在类上时，该类下所有接口方法都使用此版本；
 * 当标注在方法上时，该方法使用指定的版本号。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * // 类级别版本控制
 * {@code @RestController}
 * {@code @ApiVersion("V2.0.0")}
 * {@code @RequestMapping("/api")}
 * public class UserController {
 *     // 所有方法默认使用 V2.0.0 版本
 * }
 *
 * // 方法级别版本控制
 * {@code @GetMapping("/users")}
 * {@code @ApiVersion("V1.5.0")}
 * public List&lt;User&gt; getUsers() {
 *     // 该方法使用 V1.5.0 版本
 * }
 * </pre>
 *
 * <h3>版本号格式：</h3>
 * <ul>
 *     <li>格式：V{主版本}.{次版本}.{补丁版本}，如 V1.0.0、V2.1.3</li>
 *     <li>主版本号：不兼容的API变更</li>
 *     <li>次版本号：向后兼容的功能新增</li>
 *     <li>补丁版本号：向后兼容的问题修复</li>
 * </ul>
 *
 * @author worm
 * @see ApiVersionProperties
 * @see ApiVersionDomain
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {
    
    /**
     * 接口版本号
     * <p>
     * 版本号格式为 V{主版本}.{次版本}.{补丁版本}，例如：V1.0.0、V2.1.3
     * 如果未指定，则使用配置文件中定义的默认版本号
     * </p>
     *
     * @return 版本号字符串，默认为配置的初始版本 V1.0.0
     */
    String value() default ApiVersionProperties.API_VERSION_INIT;
}