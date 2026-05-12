package com.cashier;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 超市收银系统启动类
 *
 * @author cashier
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableCaching
@MapperScan("com.cashier.module.*.mapper")
public class CashierApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashierApplication.class, args);
        System.out.println("========================================");
        System.out.println("  超市收银系统启动成功！");
        System.out.println("  接口文档：http://localhost:8080/doc.html");
        System.out.println("========================================");
    }
}
