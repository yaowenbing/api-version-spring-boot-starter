package com.worm.api.enums;

import com.worm.properties.ApiVersionProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API版本类型枚举
 * <p>
 * 定义API版本号的传递方式，支持通过请求路径或请求头部传递版本信息。
 * </p>
 *
 * <h3>版本传递方式：</h3>
 * <ul>
 *     <li><b>PATH</b>：路径方式，版本号作为URL路径的一部分
 *         <ul>
 *             <li>示例：/v1/users、/v2/orders</li>
 *             <li>优点：直观、便于缓存、SEO友好</li>
 *             <li>适用场景：对外公开API、需要持久化链接</li>
 *         </ul>
 *     </li>
 *     <li><b>HEADER</b>：头部方式，版本号通过HTTP请求头传递
 *         <ul>
 *             <li>示例：Header: X-API-VERSION: V1.0.0</li>
 *             <li>优点：URL简洁、灵活性高</li>
 *             <li>适用场景：内部API、微服务间调用</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * @author worm
 * @see ApiVersionProperties
 */
@AllArgsConstructor
@Getter
public enum EnumVersionType {

    /**
     * 路径方式
     * <p>
     * 版本号作为URL路径的一部分，格式为 /v{版本号}/资源路径
     * 例如：/v1/users、/v2/orders
     * </p>
     */
    PATH,
    
    /**
     * 头部方式
     * <p>
     * 版本号通过HTTP请求头传递，默认头部名称为 X-API-VERSION
     * 例如：X-API-VERSION: V1.0.0
     * </p>
     */
    HEADER

}