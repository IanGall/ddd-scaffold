#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.test;

import ${package}.${uAppName}Application;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiTest {

    // 验证生成的标准工程声明了 Spring Boot 启动入口
    @Test
    void shouldDeclareSpringBootEntryPoint() {
        assertTrue(${uAppName}Application.class.isAnnotationPresent(SpringBootApplication.class));
    }

}
