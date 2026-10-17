package com.worm.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiVersionDomain 测试：版本解析、比较与格式化。
 *
 * 为什么：版本解析是比较与匹配的输入，解析规则（默认值、大小写、非法输入）一旦漂移
 * 会导致路由落到错误控制器 —— 干什么：逐场景钉死解析结果、比较方向与 toString 格式
 */
class ApiVersionDomainTest {

    @Test
    @DisplayName("解析：完整三段版本")
    void parseFullVersion() {
        ApiVersionDomain domain = ApiVersionDomain.getApiVersionDomain().apply("V1.2.3");

        assertThat(domain.getLargeVersion()).isEqualTo(1);
        assertThat(domain.getMinorVersion()).isEqualTo(2);
        assertThat(domain.getPatchVersion()).isEqualTo(3);
        assertThat(domain.toString()).isEqualTo("V1.2.3");
    }

    @Test
    @DisplayName("解析：小写 v 前缀同样识别")
    void parseLowerCaseVersion() {
        ApiVersionDomain domain = ApiVersionDomain.getApiVersionDomain().apply("v2.1.0");

        assertThat(domain.getLargeVersion()).isEqualTo(2);
        assertThat(domain.getMinorVersion()).isEqualTo(1);
        assertThat(domain.getPatchVersion()).isEqualTo(0);
    }

    @Test
    @DisplayName("解析：仅主版本号时次/补丁默认为 0")
    void parseMajorOnly() {
        ApiVersionDomain domain = ApiVersionDomain.getApiVersionDomain().apply("V3");

        assertThat(domain.getLargeVersion()).isEqualTo(3);
        assertThat(domain.getMinorVersion()).isZero();
        assertThat(domain.getPatchVersion()).isZero();
        assertThat(domain.toString()).isEqualTo("V3.0.0");
    }

    @Test
    @DisplayName("解析：两段版本时补丁默认为 0")
    void parseMajorAndMinor() {
        ApiVersionDomain domain = ApiVersionDomain.getApiVersionDomain().apply("V2.5");

        assertThat(domain.getLargeVersion()).isEqualTo(2);
        assertThat(domain.getMinorVersion()).isEqualTo(5);
        assertThat(domain.getPatchVersion()).isZero();
    }

    @Test
    @DisplayName("解析：无法匹配时主版本默认 1、次/补丁默认 0")
    void parseInvalidVersionFallsBackToDefaults() {
        ApiVersionDomain domain = ApiVersionDomain.getApiVersionDomain().apply("not-a-version");

        assertThat(domain.getLargeVersion()).isEqualTo(1);
        assertThat(domain.getMinorVersion()).isZero();
        assertThat(domain.getPatchVersion()).isZero();
    }

    @Test
    @DisplayName("比较：按主 -> 次 -> 补丁依次比较")
    void compareToOrdersByMajorMinorPatch() {
        ApiVersionDomain v1 = new ApiVersionDomain(1, 0, 0);
        ApiVersionDomain v1Same = new ApiVersionDomain(1, 0, 0);
        ApiVersionDomain v1MinorHigher = new ApiVersionDomain(1, 1, 0);
        ApiVersionDomain v1PatchHigher = new ApiVersionDomain(1, 0, 1);
        ApiVersionDomain v2 = new ApiVersionDomain(2, 0, 0);

        assertThat(v1.compareTo(v1Same)).isZero();
        assertThat(v2.compareTo(v1)).isPositive();
        assertThat(v1.compareTo(v2)).isNegative();
        assertThat(v1MinorHigher.compareTo(v1)).isPositive();
        assertThat(v1PatchHigher.compareTo(v1)).isPositive();
        // 补丁差异优先于……不，补丁最低优先级：V1.0.1 < V1.1.0
        assertThat(v1PatchHigher.compareTo(v1MinorHigher)).isNegative();
    }

    @Test
    @DisplayName("无参构造：三段版本均为 0")
    void noArgsConstructorDefaultsToZero() {
        ApiVersionDomain domain = new ApiVersionDomain();

        assertThat(domain.getLargeVersion()).isZero();
        assertThat(domain.getMinorVersion()).isZero();
        assertThat(domain.getPatchVersion()).isZero();
        assertThat(domain.toString()).isEqualTo("V0.0.0");
    }
}
