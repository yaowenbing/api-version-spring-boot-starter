package com.worm.api;

import com.worm.properties.ApiVersionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiVersionRequestMappingHandlerMapping 测试：扫描包判定与注解提取。
 *
 * 为什么：版本条件只在扫描包内生效，扫描判定或注解提取错误会导致版本控制静默失效
 * —— 干什么：以类/方法两级、注解有无、扫描包内外逐场景钉死返回结果
 */
class ApiVersionRequestMappingHandlerMappingTest {

    /**
     * 扫描包内的测试控制器（类上带 @ApiVersion）。
     */
    @ApiVersion("V2.0.0")
    static class ScannedController {

        @SuppressWarnings("unused")
        public String plainMethod() {
            return "plain";
        }

        @SuppressWarnings("unused")
        @ApiVersion("V3.0.0")
        public String versionedMethod() {
            return "versioned";
        }
    }

    private ApiVersionRequestMappingHandlerMapping handlerMapping;

    @BeforeEach
    void setUp() {
        ApiVersionProperties properties = new ApiVersionProperties();
        properties.setScanPackage(java.util.Collections.singletonList(ScannedController.class.getPackage().getName()));
        handlerMapping = new ApiVersionRequestMappingHandlerMapping(properties);
    }

    private Method methodOf(Class<?> type, String name) throws NoSuchMethodException {
        return type.getDeclaredMethod(name);
    }

    @Test
    @DisplayName("类级：扫描包内且带注解 → 以注解版本构建条件")
    void typeConditionUsesClassAnnotation() {
        RequestCondition<?> condition = handlerMapping.getCustomTypeCondition(ScannedController.class);

        assertThat(condition).isInstanceOf(ApiVersionRequestCondition.class);
        assertThat(((ApiVersionRequestCondition) condition).getApiVersionDomain().toString()).isEqualTo("V2.0.0");
    }

    @Test
    @DisplayName("类级：扫描包内无注解 → 以配置默认版本构建条件")
    void typeConditionFallsBackToConfiguredVersion() {
        ApiVersionProperties properties = new ApiVersionProperties();
        properties.setScanPackage(java.util.Collections.singletonList(com.worm.fixture.UnscannedController.class.getPackage().getName()));
        properties.setVersion("V1.5.0");
        ApiVersionRequestMappingHandlerMapping mapping = new ApiVersionRequestMappingHandlerMapping(properties);

        RequestCondition<?> condition = mapping.getCustomTypeCondition(com.worm.fixture.UnscannedController.class);

        assertThat(condition).isInstanceOf(ApiVersionRequestCondition.class);
        assertThat(((ApiVersionRequestCondition) condition).getApiVersionDomain().toString()).isEqualTo("V1.5.0");
    }

    @Test
    @DisplayName("类级：扫描包外 → 返回 null（父类默认实现）")
    void typeConditionOutsideScanPackageIsNull() {
        RequestCondition<?> condition = handlerMapping.getCustomTypeCondition(com.worm.fixture.UnscannedController.class);

        assertThat(condition).isNull();
    }

    @Test
    @DisplayName("方法级：扫描包内带注解 → 以注解版本构建条件（覆盖类级）")
    void methodConditionUsesMethodAnnotation() throws NoSuchMethodException {
        RequestCondition<?> condition = handlerMapping.getCustomMethodCondition(methodOf(ScannedController.class, "versionedMethod"));

        assertThat(condition).isInstanceOf(ApiVersionRequestCondition.class);
        assertThat(((ApiVersionRequestCondition) condition).getApiVersionDomain().toString()).isEqualTo("V3.0.0");
    }

    @Test
    @DisplayName("方法级：扫描包内无注解 → 以配置默认版本构建条件")
    void methodConditionFallsBackToConfiguredVersion() throws NoSuchMethodException {
        RequestCondition<?> condition = handlerMapping.getCustomMethodCondition(methodOf(ScannedController.class, "plainMethod"));

        assertThat(condition).isInstanceOf(ApiVersionRequestCondition.class);
        // 配置默认版本 V1.0.0（ApiVersionProperties 默认值）
        assertThat(((ApiVersionRequestCondition) condition).getApiVersionDomain().toString()).isEqualTo("V1.0.0");
    }

    @Test
    @DisplayName("方法级：扫描包外 → 返回 null（父类默认实现）")
    void methodConditionOutsideScanPackageIsNull() throws NoSuchMethodException {
        RequestCondition<?> condition = handlerMapping.getCustomMethodCondition(
                com.worm.fixture.UnscannedController.class.getDeclaredMethod("plainMethod"));

        assertThat(condition).isNull();
    }

    @Test
    @DisplayName("扫描包为空 → 任何类都不做版本控制")
    void emptyScanPackageMatchesNothing() {
        ApiVersionProperties properties = new ApiVersionProperties();
        properties.setScanPackage(java.util.Collections.emptyList());
        ApiVersionRequestMappingHandlerMapping mapping = new ApiVersionRequestMappingHandlerMapping(properties);

        assertThat(mapping.getCustomTypeCondition(ScannedController.class)).isNull();
    }
}
