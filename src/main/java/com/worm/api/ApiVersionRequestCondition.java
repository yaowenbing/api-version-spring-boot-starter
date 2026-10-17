package com.worm.api;


import com.worm.properties.ApiVersionProperties;
import com.worm.api.enums.EnumVersionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import javax.servlet.http.HttpServletRequest;
import java.util.function.Function;

/**
 * API版本请求条件匹配器
 * <p>
 * 实现Spring MVC的RequestCondition接口，用于根据API版本进行请求匹配。
 * 支持通过URL路径或请求头传递版本号，实现多版本API共存。
 * </p>
 *
 * @author worm
 */
@Slf4j
@Getter
@AllArgsConstructor
public class ApiVersionRequestCondition implements RequestCondition<ApiVersionRequestCondition> {

    /** API版本配置属性 */
    private ApiVersionProperties apiVersionProperties;

    /** API版本域对象，包含版本号信息 */
    private ApiVersionDomain apiVersionDomain;


    /**
     * 一个http接口上有多个条件规则时，用于合并
     * 和另外一个请求匹配条件合并，具体合并逻辑由实现类提供
     * <p>
     * 当类和方法上都包含 @ApiVersion注解时，通过ApiVersionRequestCondition.combine完成注解的合并，提取版本号，并与注解上的版本号进行比对，判断版本号是否符合要求。
     *
     * @param other :
     * @return :
     */
    @Override
    public ApiVersionRequestCondition combine(ApiVersionRequestCondition other) {
        log.info("执行combine;this======{},other======{}", this.apiVersionDomain, other.getApiVersionDomain());
        //  采用定义版本比较大的优先原则，就是方法上的定义与类上面的定义进行比较，那个大就用那个版本号
        return this.apiVersionDomain.compareTo(other.apiVersionDomain) > 0 ?
                new ApiVersionRequestCondition(apiVersionProperties, this.apiVersionDomain) :
                new ApiVersionRequestCondition(apiVersionProperties, other.apiVersionDomain);
    }


    /**
     * 控制了只有版本小于等于请求参数中的版本的 ApiCondition 才满足规则
     * 即找到所有小于等于输入版本号的控制器
     * <p>
     * 检查当前请求匹配条件和指定请求request是否匹配，如果不匹配返回null，
     * 如果匹配，生成一个新的请求匹配条件，该新的请求匹配条件是当前请求匹配条件
     * 针对指定请求request的剪裁。
     * 举个例子来讲，如果当前请求匹配条件是一个路径匹配条件，包含多个路径匹配模板，
     * 并且其中有些模板和指定请求request匹配，那么返回的新建的请求匹配条件将仅仅
     * 包含和指定请求request匹配的那些路径匹配模板。
     * <p>
     * 请求路径的方式有浸入，我个人不推荐，我推荐在请求头中添加版本信息，对业务接口的侵入小一点
     * <p>
     * 通过ApiVersionCondition 重写 RequestCondition 的URL匹配逻辑，将提取请求URL中的版本号与注解上定义的版本号进行对比，以此来判断某个请求应落在哪个控制器上。
     */
    @Nullable
    @Override
    public ApiVersionRequestCondition getMatchingCondition(HttpServletRequest request) {
        log.info("路径匹配:{}", request.getRequestURI());
        EnumVersionType versionType = apiVersionProperties.getVersionType();
        // 请求的版本
        ApiVersionDomain versionDomain;
        switch (versionType) {
            case PATH:
                versionDomain = this.getMatchingConditionPath().apply(request.getRequestURI());
                break;
            case HEADER:
                versionDomain = this.getMatchingConditionHeader().apply(request.getHeader(apiVersionProperties.getHeaderKey()));
                break;
            default:
                versionDomain = new ApiVersionDomain();
                break;
        }
        // 获取所有小于等于版本的接口(所有小于输入的接口版本的所有处理器)，即：如果请求的版本号大于配置版本号， 则满足
        if (versionDomain != null && versionDomain.compareTo(this.apiVersionDomain) >= 0) {
            log.info("找到处理的版本：{}", this.apiVersionDomain);
            return this;
        }
        return null;
    }

    /**
     * 通过路径信息获取版本数据
     */
    private Function<String, ApiVersionDomain> getMatchingConditionPath() {
        return uri -> {
            // 请求的版本
            ApiVersionDomain versionDomain;
            // 为什么：原实现依赖 Hutool ReUtil.contains，为保持零非 Spring 体系依赖 —— 干什么：以 JDK Matcher.find 判定 URI 是否含版本号
            boolean match = ApiVersionDomain.VERSION_PATTERN.matcher(uri).find();
            if (match) {
                // 请求路径的方式
                versionDomain = ApiVersionDomain.getApiVersionDomain().apply(uri);
            } else {
                // 请求头设置默认值
                versionDomain = ApiVersionDomain.getApiVersionDomain().apply(apiVersionProperties.getVersion());
            }
            return versionDomain;
        };
    }

    /**
     * 通过头部信息获取版本数据
     */
    private Function<String, ApiVersionDomain> getMatchingConditionHeader() {
        return apiVersion -> {
            // 请求的版本
            ApiVersionDomain versionDomain;
            // 为什么：原实现依赖 Hutool CharSequenceUtil.isNotBlank —— 干什么：以 JDK trim+isEmpty 判定请求头版本号是否为空白
            if (apiVersion != null && !apiVersion.trim().isEmpty()) {
                // 请求头的方式
                versionDomain = ApiVersionDomain.getApiVersionDomain().apply(apiVersion);
            } else {
                // 请求头设置默认值
                versionDomain = ApiVersionDomain.getApiVersionDomain().apply(apiVersionProperties.getVersion());
            }
            return versionDomain;
        };
    }

    /**
     * 当有多个ApiCondition满足这个请求时，选择最大的版本
     * 即担忧多个控制器时，我们找最大版本的控制器
     */
    @Override
    public int compareTo(ApiVersionRequestCondition other, HttpServletRequest request) {
        log.info("比较找到的所有版本，找出最大的版本号:this======{},other======{}", this.apiVersionDomain, other.apiVersionDomain);
        // 获取最大版本的接口，即 优先匹配最新的版本号
        return other.apiVersionDomain.compareTo(this.apiVersionDomain);
    }
}
