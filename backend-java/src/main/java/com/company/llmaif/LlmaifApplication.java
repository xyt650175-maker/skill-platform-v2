package com.company.llmaif;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能应用协作平台后端启动类
 * <p>
 * 包扫描路径：com.company.llmaif
 * Mapper 扫描路径：com.company.llmaif.*.dao
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.company.llmaif.**.dao")
public class LlmaifApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmaifApplication.class, args);
        System.out.println("=========================================");
        System.out.println("  LLM AIF Backend started successfully");
        System.out.println("  http://localhost:8080/race-api");
        System.out.println("=========================================");
    }
}
