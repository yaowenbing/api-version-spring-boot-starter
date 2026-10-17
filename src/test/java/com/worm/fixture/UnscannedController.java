package com.worm.fixture;

/**
 * 扫描包外的测试控制器（位于独立 fixture 包，用于验证版本控制不生效）。
 */
public class UnscannedController {

    @SuppressWarnings("unused")
    public String plainMethod() {
        return "plain";
    }
}
