# api-version-spring-boot-starter

A Spring Boot 2 API version-control starter: `@ApiVersion` annotation + a custom `RequestMappingHandlerMapping`, supporting both header and path version delivery, with multi-version coexistence and highest-version-first matching.

中文 | [English](README_EN.md)

## Introduction

Upgrading an API means either spinning up a whole new set of `/v2/...` URLs (forcing all legacy clients to migrate) or hand-writing `if (version >= 2)` inside methods (logic scattered everywhere). This starter converges version routing into one thing:

1. **Declare versions by annotation** — `@ApiVersion("V2.0.0")` on classes or methods; version metadata lives next to business code;
2. **Request-driven matching** — the version travels via a request header (`X-API-VERSION`) or the URL path; Spring MVC routes by "request version ≥ controller version" and **picks the highest matching version**;
3. **Two-level merge** — class-level and method-level annotations merge automatically, taking the larger version — no repetition needed.

Ideal for canary releases, multi-client coexistence (old/new app versions), and gradual evolution of public APIs.

## Features

- **Zero-code routing** — no interceptors/filters; replaces the default `RequestMappingHandlerMapping` via `WebMvcRegistrations`
- **Class/method two-level annotations** — method level overrides class level; merge rule: take the larger version
- **Two delivery modes** — `HEADER` (default, clean URLs) and `PATH` (`/v2/users`, intuitive)
- **Downward-compatible matching** — a request matches when its version ≥ the controller version; when multiple controllers match, the highest version wins
- **Default version fallback** — unannotated controllers/methods use the configured default version; requests without a version fall back to the default
- **Precise scan-package control** — only controllers under `worm.api.version.scan-package` are affected
- **Web-conditional assembly** — assembled only in Servlet web environments; disable with `worm.api.version.enable=false`

## Requirements

| Dependency | Version |
| --- | --- |
| JDK | 8+ |
| Maven | 3.6+ |
| Spring Boot | 2.7.x |

This repository targets the JDK 8 / Spring Boot 2.7 baseline; build output is Java 8 bytecode, ready to use in Spring Boot 2.x projects.

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>com.worm</groupId>
    <artifactId>api-version-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configure scan package and delivery mode

```yaml
worm:
  api:
    version:
      enable: true                  # master switch (default true)
      version: V1.0.0               # default version (used when no annotation)
      version-type: header          # delivery mode: header / path
      header-key: X-API-VERSION     # header name in HEADER mode
      scan-package:                 # packages under which version control applies (required)
        - com.example.controller
```

### 3. Declare endpoint versions

```java
@RestController
@RequestMapping("/users")
@ApiVersion("V1.0.0")               // class level: default version for the class
public class UserController {

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @ApiVersion("V2.0.0")           // method level: overrides class level; only V2.0.0+ requests match
    @GetMapping("/{id}/detail")
    public UserDetail getDetail(@PathVariable Long id) {
        return userService.getDetail(id);
    }
}
```

### 4. Requests and routing behavior

HEADER mode (default):

```bash
# no version header → matches with the configured default V1.0.0 → hits getById
curl http://localhost:8080/users/1

# with V2.0.0 → getDetail (V2.0.0) matches; getById (V1.0.0) also matches (downward compatible)
curl -H "X-API-VERSION: V2.0.0" http://localhost:8080/users/1/detail

# requesting a V2 endpoint with V1.0.0 → request version < controller version → no match → 404
curl -H "X-API-VERSION: V1.0.0" http://localhost:8080/users/1/detail
```

PATH mode (`version-type: path`): the version is extracted from the URI, e.g. `/v2/users/1`; when the URI contains no version, the configured default is used.

### 5. Disable version control

```yaml
worm:
  api:
    version:
      enable: false
```

## Full Configuration List

Prefix `worm.api.version`:

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `enable` | Boolean | `true` | Master switch; when off, no version-control HandlerMapping is registered |
| `version` | String | `V1.0.0` | Default version; used when a controller/method has no `@ApiVersion` |
| `version-type` | EnumVersionType | `HEADER` | Delivery mode: `HEADER` (request header) / `PATH` (URL path) |
| `header-key` | String | `X-API-VERSION` | Header name in HEADER mode |
| `scan-package` | List\<String\> | empty | Packages under which version control applies (prefix match, case-insensitive); **unconfigured means no controller is version-controlled** |

## Version Rules

- Format: `V{major}.{minor}.{patch}`, case-insensitive (`v1.2.3` equals `V1.2.3`)
- Shorthand: `V1` → `V1.0.0`, `V1.2` → `V1.2.0`
- Unparseable input falls back to: major `1`, minor `0`, patch `0`
- Comparison: major → minor → patch, in order

## Architecture and Package Structure

```text
com.worm
├── config
│   └── ApiVersionAutoConfiguration          // Auto-configuration (implements WebMvcRegistrations, replaces the default HandlerMapping)
├── properties
│   └── ApiVersionProperties                 // Configuration properties (prefix worm.api.version)
└── api
    ├── ApiVersion                           // Version declaration annotation (class/method levels)
    ├── ApiVersionDomain                     // Version parsing/comparison/formatting
    ├── ApiVersionRequestCondition           // Request condition: extracts the request version and decides matching
    ├── ApiVersionRequestMappingHandlerMapping // Custom HandlerMapping: annotations in scan packages → version conditions
    └── enums.EnumVersionType                // Delivery mode (HEADER/PATH)
```

Matching flow:

```text
Request arrives
  → HandlerMapping finds the handler (only classes/methods in scan packages get version conditions)
  → ApiVersionRequestCondition.getMatchingCondition: extracts the request version by HEADER/PATH
  → request version >= controller version ? match : skip
  → when multiple match, compareTo picks the highest version to execute
```

## FAQ

### 1. Why is `scan-package` empty by default?

An empty default means "no interception unless configured", avoiding accidental changes to existing routing behavior after adding the starter; the version-control scope is declared explicitly by the consumer. The legacy internal default `com.swcares.aiot` was a project-specific package and no longer applies.

### 2. HEADER or PATH?

HEADER keeps URLs clean and intrudes minimally — recommended for internal APIs and inter-service calls. PATH is intuitive, cache-friendly, and good for persistent links — suitable for public APIs. Switch via `version-type` without code changes.

### 3. What if both class-level and method-level are annotated?

The `combine` phase merges them automatically, taking the larger version — a method annotated `V2.0.0` in a class annotated `V1.0.0` participates in matching as `V2.0.0`.

### 4. What happens when no version controller matches?

The request follows the normal Spring MVC flow: 404 if no other handler exists. Version conditions only "filter"; they never change the behavior of unannotated controllers (packages outside the scan scope are completely unaffected).

### 5. How do I use this with Spring Boot 3 / the jakarta namespace?

This repository is the open-source JDK 8 / Spring Boot 2.7 (javax) baseline. For the jakarta baseline (Spring Boot 3+), change `javax.servlet` references to `jakarta.servlet` and use Boot 3's `org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations`.

## License

This project is licensed under the [Apache License 2.0](LICENSE). See the [LICENSE](LICENSE) file for details.
