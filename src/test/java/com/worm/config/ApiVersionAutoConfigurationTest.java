package com.worm.config;

import com.worm.properties.ApiVersionProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiVersionAutoConfiguration 自动装配测试。
 *
 * 为什么：starter 的开关语义（matchIfMissing / enable=false）直接决定版本控制是否生效
 * —— 干什么：以 WebApplicationContextRunner 轻量验证各场景
 */
class ApiVersionAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ApiVersionAutoConfiguration.class));

    @Test
    @DisplayName("默认（无配置）：Web 环境下自动装配生效")
    void registeredByDefaultInWebEnvironment() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ApiVersionAutoConfiguration.class);
            assertThat(context).hasSingleBean(ApiVersionProperties.class);
        });
    }

    @Test
    @DisplayName("worm.api.version.enable=false：不装配")
    void disabledByProperty() {
        contextRunner.withPropertyValues("worm.api.version.enable=false").run(context -> {
            assertThat(context).doesNotHaveBean(ApiVersionAutoConfiguration.class);
        });
    }

    @Test
    @DisplayName("worm.api.version.enable=true：显式启用时装配")
    void enabledByProperty() {
        contextRunner.withPropertyValues("worm.api.version.enable=true").run(context -> {
            assertThat(context).hasSingleBean(ApiVersionAutoConfiguration.class);
        });
    }

    @Test
    @DisplayName("getRequestMappingHandlerMapping：返回携带配置属性的自定义映射器")
    void handlerMappingCarriesProperties() {
        contextRunner.withPropertyValues("worm.api.version.version=V2.0.0").run(context -> {
            ApiVersionAutoConfiguration configuration = context.getBean(ApiVersionAutoConfiguration.class);
            assertThat(configuration.getRequestMappingHandlerMapping()).isNotNull();
            assertThat(context.getBean(ApiVersionProperties.class).getVersion()).isEqualTo("V2.0.0");
        });
    }
}
