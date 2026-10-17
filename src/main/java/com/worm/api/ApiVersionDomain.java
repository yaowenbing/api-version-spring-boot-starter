package com.worm.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API版本领域对象
 * <p>
 * 用于封装和解析API版本号信息，支持版本号的解析、比较和格式化。
 * 采用语义化版本号规范，格式为 V{主版本}.{次版本}.{补丁版本}。
 * </p>
 *
 * <h3>版本号结构：</h3>
 * <ul>
 *     <li>主版本号：不兼容的API变更时递增</li>
 *     <li>次版本号：向后兼容的功能新增时递增</li>
 *     <li>补丁版本号：向后兼容的问题修复时递增</li>
 * </ul>
 *
 * <h3>版本比较规则：</h3>
 * <ol>
 *     <li>先比较主版本号，大的版本优先级高</li>
 *     <li>主版本号相同则比较次版本号</li>
 *     <li>次版本号相同则比较补丁版本号</li>
 * </ol>
 *
 * @author worm
 * @see ApiVersion
 * @see ApiVersionRequestCondition
 */
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ApiVersionDomain implements Comparable<ApiVersionDomain> {

    /**
     * 版本号正则表达式
     * <p>
     * 匹配格式：V1、V1.0、V1.0.0（不区分大小写）
     * 捕获组说明：
     * <ul>
     *     <li>组1：主版本号（必需）</li>
     *     <li>组2：次版本号（可选，默认0）</li>
     *     <li>组3：补丁版本号（可选，默认0）</li>
     * </ul>
     * </p>
     */
    public static final Pattern VERSION_PATTERN = Pattern.compile("[vV](\\d)(?:.(\\d))?(?:.(\\d))?");

    /**
     * 主版本号
     * <p>
     * 表示不兼容的API变更，如删除接口、修改返回结构等
     * </p>
     */
    private int largeVersion;

    /**
     * 次版本号
     * <p>
     * 表示向后兼容的功能新增，如新增接口、新增返回字段等
     * </p>
     */
    private int minorVersion;

    /**
     * 补丁版本号
     * <p>
     * 表示向后兼容的问题修复，如修复Bug、优化性能等
     * </p>
     */
    private int patchVersion;

    /**
     * 比较两个版本号的大小
     * <p>
     * 按照主版本号 -> 次版本号 -> 补丁版本的顺序依次比较
     * </p>
     *
     * @param apiVersionDomain 要比较的版本对象
     * @return 负数表示当前版本较小，0表示相等，正数表示当前版本较大
     */
    @Override
    public int compareTo(ApiVersionDomain apiVersionDomain) {
        // 为什么：三段版本号均为 int 基本类型，JDK 自带的 Integer.compare 即可完成比较，无需第三方工具
        // 干什么：按主版本 -> 次版本 -> 补丁版本依次比较，返回首个非零结果
        int compareLargeVersion = Integer.compare(this.largeVersion, apiVersionDomain.largeVersion);
        if (compareLargeVersion != 0) {
            return compareLargeVersion;
        }

        // 主版本号相同，比较次版本号
        int compareMinorVersion = Integer.compare(this.minorVersion, apiVersionDomain.minorVersion);
        if (compareMinorVersion != 0) {
            return compareMinorVersion;
        }

        // 次版本号相同，比较补丁版本号
        int comparePatchVersion = Integer.compare(this.patchVersion, apiVersionDomain.patchVersion);
        if (comparePatchVersion != 0) {
            return comparePatchVersion;
        }

        // 三个版本号都相同
        return 0;
    }

    /**
     * 将版本对象转换为字符串格式
     * <p>
     * 输出格式：V{主版本}.{次版本}.{补丁版本}，如 V1.0.0
     * </p>
     *
     * @return 格式化的版本字符串
     */
    @Override
    public String toString() {
        // 为什么：固定格式拼接无需模板引擎 —— 干什么：以字符串拼接输出 V{主}.{次}.{补丁}
        return "V" + this.largeVersion + "." + this.minorVersion + "." + this.patchVersion;
    }

    /**
     * 创建版本解析函数
     * <p>
     * 将版本字符串解析为ApiVersionDomain对象，支持以下格式：
     * <ul>
     *     <li>V1 或 v1 - 解析为 V1.0.0</li>
     *     <li>V1.2 或 v1.2 - 解析为 V1.2.0</li>
     *     <li>V1.2.3 或 v1.2.3 - 解析为 V1.2.3</li>
     * </ul>
     * 无法匹配时主版本号默认为1，次版本号与补丁版本号默认为0。
     * </p>
     *
     * @return 版本字符串到ApiVersionDomain对象的转换函数
     */
    public static Function<String, ApiVersionDomain> getApiVersionDomain() {
        return version -> {
            log.debug("开始解析版本字符串: {}", version);

            // 提取主版本号，默认为1
            int largeVersion = Integer.parseInt(Optional.ofNullable(extractGroup(version, 1)).orElse("1"));

            // 提取次版本号，默认为0
            int minorVersion = Integer.parseInt(Optional.ofNullable(extractGroup(version, 2)).orElse("0"));

            // 提取补丁版本号，默认为0
            int patchVersion = Integer.parseInt(Optional.ofNullable(extractGroup(version, 3)).orElse("0"));

            ApiVersionDomain domain = new ApiVersionDomain(largeVersion, minorVersion, patchVersion);
            log.debug("版本字符串解析完成: {} -> {}", version, domain);

            return domain;
        };
    }

    /**
     * 从版本字符串中提取指定捕获组的值。
     *
     * 为什么：原实现依赖 Hutool ReUtil.get，为保持零非 Spring 体系依赖 —— 干什么：以 JDK Matcher
     * 完成首个匹配的捕获组提取；无匹配或捕获组未参与匹配时返回 null（与 ReUtil.get 语义一致）。
     *
     * @param version 版本字符串，允许 null
     * @param group   捕获组序号（1 起）
     * @return 捕获组值；无匹配或组未参与时返回 null
     */
    private static String extractGroup(String version, int group) {
        if (version == null) {
            return null;
        }
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(group);
    }
}
