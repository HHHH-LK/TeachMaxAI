// 🔧 方案4：创建独立的TypeHandler配置类（最推荐）
package com.aiproject.smartcampus.config;

import com.aiproject.smartcampus.commons.utils.TextTypeHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * TypeHandler注册配置
 * 使用CommandLineRunner在应用启动完成后注册
 */
@Component
@Order(100) // 确保在其他组件初始化后执行
@Slf4j
public class TypeHandlerRegistrar implements CommandLineRunner {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Override
    public void run(String... args) throws Exception {
        registerCustomTypeHandlers();
    }

    private void registerCustomTypeHandlers() {
        try {
            TypeHandlerRegistry registry = sqlSessionFactory.getConfiguration().getTypeHandlerRegistry();
            
            // 注册TextTypeHandler
            registry.register(TextTypeHandler.class);
            
            log.info("✅ Custom TypeHandlers registered successfully:");
            log.info("   - TextTypeHandler for handling TEXT/BLOB fields");
            
        } catch (Exception e) {
            log.error("❌ Failed to register custom TypeHandlers: {}", e.getMessage(), e);
        }
    }
}
