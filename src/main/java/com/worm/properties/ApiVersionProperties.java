package com.worm.properties;

import com.worm.api.enums.EnumVersionType;
import com.worm.config.ApiVersionAutoConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * API版本控制配置属性
 * <p>
 * 用于配置API版本控制的各种参数，包括启用状态、扫描包路径、
 * 默认版本号、版本传递方式等。
 * </p>
 *
 * <h3>配置前缀：</h3>
 * <code>worm.api.version</code>
 *
 * <h3>配置示例：</h3>
 * <pre>
 * # application.yml
 * worm:
 *   api:
 *     version:
 *       enable: true                      # 是否启用版本控制
 *       version: V1.0.0                   # 默认版本号
 *       version-type: header              # 版本传递方式
 *       header-key: X-API-VERSION         # 头部方式的key名称
 *       scan-package:                     # 需要扫描的包路径列表
 *         - com.example.controller
 *         - com.example.api
 * </pre>
 *
 * <h3>属性说明：</h3>
 * <ul>
 *     <li><b>enable</b>：是否启用API版本控制功能，<b>默认开启（true）</b>；如需关闭请显式设置 worm.api.version.enable=false</li>
 *     <li><b>scanPackage</b>：需要扫描的包路径列表，只有这些包下的控制器才会进行版本控制</li>
 *     <li><b>version</b>：默认版本号，当未指定@ApiVersion注解时使用</li>
 *     <li><b>versionType</b>：版本传递方式，支持HEADER和PATH</li>
 *     <li><b>headerKey</b>：使用HEADER方式时的请求头名称</li>
 * </ul>
 *
 * @author worm
 * @see EnumVersionType
 * @see ApiVersionAutoConfiguration
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "worm.api.version")
@NoArgsConstructor
public class ApiVersionProperties {
    
    /**
     * API版本号初始值常量
     * <p>
     * 默认版本号为 V1.0.0，格式为 V{主版本}.{次版本}.{补丁版本}
     * </p>
     */
    public static final String API_VERSION_INIT = "V1.0.0";

    /**
     * API版本请求头的默认key名称
     * <p>
     * 当使用HEADER方式传递版本号时，默认使用此key
     * </p>
     */
    private static final String API_VERSION_HEADER_KEY = "X-API-VERSION";
    
    /**
     * 是否启用API版本控制
     * <p>
     * 默认开启（true）。启用后会对扫描包路径下的控制器进行版本控制处理；
     * 如需关闭，需显式设置 worm.api.version.enable=false。
     * </p>
     */
    private Boolean enable = true;
    
    /**
     * 需要扫描的包路径列表
     * <p>
     * 只有在这些包路径下的控制器才会被纳入版本控制范围。
     * 支持配置多个包路径，默认为空列表（未配置则任何控制器都不做版本控制）。
     *
     * 为什么：原默认值 com.swcares.aiot 是内部工程的包名，对通用开源 starter 属于无关的隐式行为
     * —— 干什么：默认改为空列表，由消费方显式声明扫描范围，避免误拦截
     * </p>
     */
    private List<String> scanPackage = Collections.emptyList();
    
    /**
     * 默认API版本号
     * <p>
     * 当控制器或方法未指定 @ApiVersion 注解时，使用此默认版本号。
     * 格式为 V{主版本}.{次版本}.{补丁版本}，如 V1.0.0
     * </p>
     */
    private String version = API_VERSION_INIT;
    
    /**
     * API版本传递方式
     * <p>
     * 支持两种方式：
     * <ul>
     *     <li>{@link EnumVersionType#HEADER}：通过HTTP请求头传递版本号</li>
     *     <li>{@link EnumVersionType#PATH}：通过URL路径传递版本号</li>
     * </ul>
     * 默认为HEADER方式
     * </p>
     */
    private EnumVersionType versionType = EnumVersionType.HEADER;
    
    /**
     * 版本号请求头的key名称
     * <p>
     * 当 versionType 为 HEADER 时，使用此配置作为请求头的key。
     * 默认为 X-API-VERSION
     * </p>
     */
    private String headerKey = API_VERSION_HEADER_KEY;
}