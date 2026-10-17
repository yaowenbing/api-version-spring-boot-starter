package com.worm.properties;

import com.worm.api.enums.EnumVersionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiVersionProperties 默认值测试。
 *
 * 为什么：默认值是零配置接入的行为基线，尤其是开关与扫描包直接影响是否拦截控制器
 * —— 干什么：钉死各字段默认取值
 */
class ApiVersionPropertiesTest {

    @Test
    @DisplayName("默认值：开关启用、默认版本 V1.0.0、HEADER 方式、X-API-VERSION、扫描包为空")
    void defaults() {
        ApiVersionProperties properties = new ApiVersionProperties();

        assertThat(properties.getEnable()).isTrue();
        assertThat(properties.getVersion()).isEqualTo(ApiVersionProperties.API_VERSION_INIT);
        assertThat(properties.getVersionType()).isEqualTo(EnumVersionType.HEADER);
        assertThat(properties.getHeaderKey()).isEqualTo("X-API-VERSION");
        assertThat(properties.getScanPackage()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("setter：各字段可覆盖")
    void settersOverrideDefaults() {
        ApiVersionProperties properties = new ApiVersionProperties();
        properties.setEnable(false);
        properties.setVersion("V2.0.0");
        properties.setVersionType(EnumVersionType.PATH);
        properties.setScanPackage(java.util.Collections.singletonList("com.example"));

        assertThat(properties.getEnable()).isFalse();
        assertThat(properties.getVersion()).isEqualTo("V2.0.0");
        assertThat(properties.getVersionType()).isEqualTo(EnumVersionType.PATH);
        assertThat(properties.getScanPackage()).containsExactly("com.example");
    }
}
