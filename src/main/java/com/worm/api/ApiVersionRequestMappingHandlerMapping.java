package com.worm.api;

import com.worm.properties.ApiVersionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/**
 * API版本控制请求映射处理器
 * <p>
 * 自定义 Spring MVC 的 RequestMappingHandlerMapping，用于实现基于注解的 API 版本控制功能。
 * 通过扫描控制器类和方法上的 {@link ApiVersion} 注解，自动为接口添加版本匹配条件。
 * </p>
 *
 * <h3>工作原理：</h3>
 * <ol>
 *     <li>Spring MVC 接收请求后，通过 HandlerMapping 查找对应的处理器</li>
 *     <li>本类通过重写 getCustomTypeCondition 和 getCustomMethodCondition 方法，为处理器添加版本条件</li>
 *     <li>{@link ApiVersionRequestCondition} 根据版本条件判断是否匹配当前请求</li>
 * </ol>
 *
 * <h3>版本匹配优先级：</h3>
 * <ul>
 *     <li>方法级别的 @ApiVersion 优先于类级别</li>
 *     <li>如果未标注注解，则使用配置的默认版本号</li>
 *     <li>只处理配置的扫描包路径下的控制器</li>
 * </ul>
 *
 * @author worm
 * @see ApiVersion
 * @see ApiVersionRequestCondition
 * @see ApiVersionProperties
 */
@Slf4j
public class ApiVersionRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    /**
     * API版本配置属性
     * <p>
     * 包含扫描包路径、默认版本号、版本类型等配置信息
     */
    private final ApiVersionProperties apiVersionProperties;

    /**
     * 构造函数
     *
     * @param apiVersionProperties API版本配置属性
     */
    public ApiVersionRequestMappingHandlerMapping(ApiVersionProperties apiVersionProperties) {
        this.apiVersionProperties = apiVersionProperties;
    }

    /**
     * 获取类级别的自定义请求条件
     * <p>
     * 当控制器类位于配置的扫描包路径下时，查找类上的 @ApiVersion 注解，
     * 并创建对应的版本匹配条件。
     * </p>
     *
     * @param handlerType 控制器类的类型
     * @return 版本匹配条件，如果不在扫描包路径下则返回父类的默认实现
     */
    @Override
    protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        // 检查类是否在配置的扫描包路径下
        if (!inScanPackage(handlerType.getName())) {
            log.debug("类[{}]不在版本控制扫描包路径下，跳过版本条件处理", handlerType.getName());
            return super.getCustomTypeCondition(handlerType);
        }

        log.info("扫描到控制器类[{}]位于版本控制包路径下，开始查找版本注解", handlerType.getName());

        // 查找类上面的 @ApiVersion 注解
        ApiVersion apiVersion = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);

        // 如果未找到注解，使用配置的默认版本号
        if (apiVersion == null) {
            log.debug("类[{}]上未找到@ApiVersion注解，使用默认版本: {}",
                    handlerType.getName(), apiVersionProperties.getVersion());
            return buildCondition(apiVersionProperties.getVersion());
        }
        log.info("类[{}]上找到@ApiVersion注解，版本: {}", handlerType.getName(), apiVersion.value());
        return buildCondition(apiVersion.value());
    }

    /**
     * 获取方法级别的自定义请求条件
     * <p>
     * 当控制器方法所在的类位于配置的扫描包路径下时，查找方法上的 @ApiVersion 注解，
     * 并创建对应的版本匹配条件。方法级别的注解会覆盖类级别的注解。
     * </p>
     *
     * @param method 控制器方法
     * @return 版本匹配条件，如果不在扫描包路径下则返回父类的默认实现
     */
    @Override
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        // 检查方法所在的类是否在配置的扫描包路径下
        if (!inScanPackage(method.getDeclaringClass().getName())) {
            log.debug("方法[{}]所在的类不在版本控制扫描包路径下，跳过版本条件处理", method.getName());
            return super.getCustomMethodCondition(method);
        }

        log.info("扫描到方法[{}.{}]位于版本控制包路径下，开始查找版本注解",
                method.getDeclaringClass().getSimpleName(), method.getName());

        // 查找方法上面的 @ApiVersion 注解
        ApiVersion apiVersion = AnnotationUtils.findAnnotation(method, ApiVersion.class);

        // 如果未找到注解，使用配置的默认版本号
        if (apiVersion == null) {
            log.debug("方法[{}]上未找到@ApiVersion注解，使用默认版本: {}",
                    method.getName(), apiVersionProperties.getVersion());
            return buildCondition(apiVersionProperties.getVersion());
        }
        log.info("方法[{}]上找到@ApiVersion注解，版本: {}", method.getName(), apiVersion.value());
        return buildCondition(apiVersion.value());
    }

    /**
     * 判断类全限定名是否命中任一扫描包路径（忽略大小写的前缀匹配）。
     *
     * 为什么：原实现依赖 Hutool CharSequenceUtil.startWithIgnoreCase —— 干什么：以 JDK
     * 小写转换 + startsWith 实现同等语义，扫描包为空时直接不命中。
     *
     * @param className 类全限定名
     * @return 命中任一扫描包路径返回 true
     */
    private boolean inScanPackage(String className) {
        List<String> scanPackages = apiVersionProperties.getScanPackage();
        if (scanPackages == null || scanPackages.isEmpty()) {
            return false;
        }
        return scanPackages.stream()
                .filter(pack -> pack != null && !pack.trim().isEmpty())
                // 为什么：包名为 ASCII，固定 Locale.ROOT 避免土耳其语等 Locale 下 'I' 大小写转换异常 —— 干什么：统一转小写后做前缀匹配
                .anyMatch(pack -> className.toLowerCase(Locale.ROOT).startsWith(pack.toLowerCase(Locale.ROOT)));
    }

    /**
     * 按版本字符串构建版本请求条件。
     *
     * @param version 版本号字符串（注解值或配置默认值）
     * @return 版本请求条件
     */
    private ApiVersionRequestCondition buildCondition(String version) {
        return new ApiVersionRequestCondition(
                apiVersionProperties,
                ApiVersionDomain.getApiVersionDomain().apply(version)
        );
    }
}
