# api-version-spring-boot-starter

Spring Boot 2 API 版本控制 Starter：`@ApiVersion` 注解 + 自定义 `RequestMappingHandlerMapping`，支持请求头/路径两种版本传递方式，多版本接口共存，大版本优先匹配。

[English](README_EN.md) | 中文

## 简介

接口升级要么新起一套 `/v2/...` URL（老客户端全量改），要么在方法里手写 `if (version >= 2)`（逻辑散落）。本 starter 把版本路由收敛为一件事：

1. **注解声明版本** — `@ApiVersion("V2.0.0")` 标注在类或方法上，版本元数据与业务代码同仓；
2. **请求驱动匹配** — 版本号经请求头（`X-API-VERSION`）或 URL 路径传递，Spring MVC 按「请求版本 ≥ 控制器版本」路由，**取满足条件的最大版本**执行；
3. **两级合并** — 类级注解 + 方法级注解自动合并，取较大版本，无需重复声明。

适合灰度发布、多客户端（App 新旧版本）共存、对外 API 渐进演进场景。

## 特性

- **零代码侵入路由** — 不写拦截器/过滤器，经 `WebMvcRegistrations` 替换默认 `RequestMappingHandlerMapping`
- **类/方法两级注解** — 方法级覆盖类级，合并规则为「取较大版本」
- **两种版本传递方式** — `HEADER`（默认，URL 干净）与 `PATH`（`/v2/users`，直观可缓存）
- **向下兼容匹配** — 请求版本 ≥ 控制器版本即命中；多个控制器同时满足时优先最大版本
- **默认版本兜底** — 未标注注解的控制器/方法使用配置的默认版本；请求未带版本时回退默认版本
- **扫描包精确控制** — 仅对 `worm.api.version.scan-package` 下的控制器生效，不影响其它包
- **Web 条件装配** — 仅 Servlet Web 环境装配；`worm.api.version.enable=false` 一键关闭

## 环境要求

| 依赖 | 版本 |
| --- | --- |
| JDK | 8+ |
| Maven | 3.6+ |
| Spring Boot | 2.7.x |

本仓库为 JDK 8 / Spring Boot 2.7 基线，编译产物为 Java 8 字节码，可直接用于 Spring Boot 2.x 项目。

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.worm</groupId>
    <artifactId>api-version-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置扫描包与版本传递方式

```yaml
worm:
  api:
    version:
      enable: true                  # 总开关（默认 true）
      version: V1.0.0               # 默认版本号（未标注注解时使用）
      version-type: header          # 版本传递方式：header / path
      header-key: X-API-VERSION     # header 方式下的请求头名称
      scan-package:                 # 版本控制生效的包路径（必配）
        - com.example.controller
```

### 3. 声明接口版本

```java
@RestController
@RequestMapping("/users")
@ApiVersion("V1.0.0")               // 类级：该类默认版本
public class UserController {

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @ApiVersion("V2.0.0")           // 方法级：覆盖类级，仅 V2.0.0+ 请求可命中
    @GetMapping("/{id}/detail")
    public UserDetail getDetail(@PathVariable Long id) {
        return userService.getDetail(id);
    }
}
```

### 4. 请求与路由行为

HEADER 方式（默认）：

```bash
# 未带版本头 → 以配置默认版本 V1.0.0 匹配 → 命中 getById
curl http://localhost:8080/users/1

# 带 V2.0.0 → getDetail（V2.0.0）可命中；getById（V1.0.0）同样可命中（向下兼容）
curl -H "X-API-VERSION: V2.0.0" http://localhost:8080/users/1/detail

# 带 V1.0.0 请求 V2  ？ 接口 → 请求版本 < 控制器版本，不命中 → 404
curl -H "X-API-VERSION: V1.0.0" http://localhost:8080/users/1/detail
```

PATH 方式（`version-type: path`）：版本号从 URI 中提取，如 `/v2/users/1`；URI 不含版本号时回退配置默认版本。

### 5. 关闭版本控制

```yaml
worm:
  api:
    version:
      enable: false
```

## 配置项完整列表

前缀 `worm.api.version`：

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `enable` | Boolean | `true` | 总开关，关闭后不注册版本控制 HandlerMapping |
| `version` | String | `V1.0.0` | 默认版本号；控制器/方法未标注 `@ApiVersion` 时使用 |
| `version-type` | EnumVersionType | `HEADER` | 版本传递方式：`HEADER`（请求头）/ `PATH`（URL 路径） |
| `header-key` | String | `X-API-VERSION` | `HEADER` 方式下的请求头名称 |
| `scan-package` | List\<String\> | 空 | 版本控制生效的包路径列表（前缀匹配，忽略大小写）；**未配置则任何控制器都不做版本控制** |

## 版本号规则

- 格式：`V{主版本}.{次版本}.{补丁版本}`，不区分大小写（`v1.2.3` 等价 `V1.2.3`）
- 省略写法：`V1` → `V1.0.0`，`V1.2` → `V1.2.0`
- 无法解析时回退：主版本 `1`、次版本 `0`、补丁 `0`
- 比较规则：主版本 → 次版本 → 补丁，依次比较

## 架构与包结构

```text
com.worm
├── config
│   └── ApiVersionAutoConfiguration          // 自动配置（实现 WebMvcRegistrations，替换默认 HandlerMapping）
├── properties
│   └── ApiVersionProperties                 // 配置属性（前缀 worm.api.version）
└── api
    ├── ApiVersion                           // 版本声明注解（类/方法两级）
    ├── ApiVersionDomain                     // 版本解析/比较/格式化
    ├── ApiVersionRequestCondition           // 请求条件：提取请求版本并判定匹配
    ├── ApiVersionRequestMappingHandlerMapping // 自定义 HandlerMapping：扫描包内注解 → 版本条件
    └── enums.EnumVersionType                // 版本传递方式（HEADER/PATH）
```

匹配流程：

```text
请求进入
  → HandlerMapping 查找处理器（仅扫描包内的类/方法附加版本条件）
  → ApiVersionRequestCondition.getMatchingCondition：按 HEADER/PATH 提取请求版本
  → 请求版本 >= 控制器版本 ？ 命中 : 跳过
  → 多个命中时 compareTo 取最大版本执行
```

## FAQ

### 1. 为什么 `scan-package` 默认是空的？

默认空表示「不配置就不拦截」，避免引入 starter 后意外改变既有路由行为；版本控制范围由消费方显式声明。内部历史默认值 `com.swcares.aiot` 是特定工程的包名，已不适用。

### 2. HEADER 和 PATH 怎么选？

HEADER 方式 URL 干净、对业务接口侵入小，推荐内部 API 与微服务间调用；PATH 方式直观、便于缓存与持久化链接，适合对外公开 API。二者经 `version-type` 切换，无需改代码。

### 3. 类级和方法级都标注了会怎样？

`combine` 阶段自动合并，取较大版本——方法级标 `V2.0.0`、类级标 `V1.0.0` 时该方法按 `V2.0.0` 参与匹配。

### 4. 没有匹配到任何版本控制器会怎样？

请求按 Spring MVC 常规流程处理：若无其它处理器则返回 404。版本条件只做「过滤」，不会改变未标注控制器的行为（扫描包外完全不受影响）。

### 5. 如何接入 Spring Boot 3 / jakarta 命名空间版本？

本仓库为 JDK 8 / Spring Boot 2.7（javax）基线的开源版本。jakarta 基线（Spring Boot 3+）需将 `javax.servlet` 引用改为 `jakarta.servlet`，并使用 Boot 3 的 `org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations`。

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 开源，详见 [LICENSE](LICENSE) 文件。
