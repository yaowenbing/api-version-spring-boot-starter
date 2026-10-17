package com.worm.api;

import com.worm.api.enums.EnumVersionType;
import com.worm.properties.ApiVersionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiVersionRequestCondition 测试：combine 合并、版本匹配与优先级比较。
 *
 * 为什么：请求条件是版本路由的判定核心，合并方向或匹配阈值错误会直接把请求路由到错误版本
 * —— 干什么：以 HEADER/PATH 两种传递方式逐场景钉死匹配结果
 */
class ApiVersionRequestConditionTest {

    private ApiVersionProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ApiVersionProperties();
        properties.setVersion("V1.0.0");
        properties.setVersionType(EnumVersionType.HEADER);
        properties.setHeaderKey("X-API-VERSION");
    }

    private ApiVersionRequestCondition condition(String version) {
        return new ApiVersionRequestCondition(properties, ApiVersionDomain.getApiVersionDomain().apply(version));
    }

    @Test
    @DisplayName("combine：类/方法版本冲突时取较大版本")
    void combinePicksLargerVersion() {
        ApiVersionRequestCondition classLevel = condition("V1.0.0");
        ApiVersionRequestCondition methodLevel = condition("V2.0.0");

        ApiVersionRequestCondition combined = classLevel.combine(methodLevel);

        assertThat(combined.getApiVersionDomain().toString()).isEqualTo("V2.0.0");
    }

    @Test
    @DisplayName("combine：方法版本更低时保留类版本")
    void combineKeepsLargerClassVersion() {
        ApiVersionRequestCondition classLevel = condition("V3.0.0");
        ApiVersionRequestCondition methodLevel = condition("V2.0.0");

        ApiVersionRequestCondition combined = classLevel.combine(methodLevel);

        assertThat(combined.getApiVersionDomain().toString()).isEqualTo("V3.0.0");
    }

    @Test
    @DisplayName("HEADER 方式：请求头版本 >= 控制器版本时匹配")
    void headerModeMatchesWhenRequestVersionNotLower() {
        ApiVersionRequestCondition condition = condition("V1.0.0");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-VERSION", "V2.0.0");

        assertThat(condition.getMatchingCondition(request)).isSameAs(condition);
    }

    @Test
    @DisplayName("HEADER 方式：请求头版本 < 控制器版本时不匹配")
    void headerModeNotMatchWhenRequestVersionLower() {
        ApiVersionRequestCondition condition = condition("V2.0.0");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-VERSION", "V1.0.0");

        assertThat(condition.getMatchingCondition(request)).isNull();
    }

    @Test
    @DisplayName("HEADER 方式：请求头缺失时回退配置默认版本")
    void headerModeFallsBackToConfiguredVersion() {
        // 控制器版本 V1.0.0，配置默认版本 V1.0.0，请求头缺失 → 以默认版本匹配
        ApiVersionRequestCondition condition = condition("V1.0.0");
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(condition.getMatchingCondition(request)).isSameAs(condition);
    }

    @Test
    @DisplayName("HEADER 方式：请求头为空白时回退配置默认版本")
    void headerModeFallsBackWhenHeaderBlank() {
        ApiVersionRequestCondition condition = condition("V1.0.0");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-VERSION", "   ");

        assertThat(condition.getMatchingCondition(request)).isSameAs(condition);
    }

    @Test
    @DisplayName("PATH 方式：URI 含版本号时按 URI 版本匹配")
    void pathModeMatchesByUriVersion() {
        properties.setVersionType(EnumVersionType.PATH);
        ApiVersionRequestCondition condition = condition("V1.0.0");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v2/users");

        assertThat(condition.getMatchingCondition(request)).isSameAs(condition);
    }

    @Test
    @DisplayName("PATH 方式：URI 不含版本号时回退配置默认版本")
    void pathModeFallsBackToConfiguredVersion() {
        properties.setVersionType(EnumVersionType.PATH);
        ApiVersionRequestCondition condition = condition("V1.0.0");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users");

        assertThat(condition.getMatchingCondition(request)).isSameAs(condition);
    }

    @Test
    @DisplayName("compareTo：请求版本更高的条件排序在前（返回负值）")
    void compareToPrefersHigherVersion() {
        ApiVersionRequestCondition lower = condition("V1.0.0");
        ApiVersionRequestCondition higher = condition("V2.0.0");
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 契约：compareTo 返回 other.compareTo(this)，即 this 版本更高时结果为负（this 排序在前，优先匹配）
        assertThat(higher.compareTo(lower, request)).isNegative();
        assertThat(lower.compareTo(higher, request)).isPositive();
    }
}
