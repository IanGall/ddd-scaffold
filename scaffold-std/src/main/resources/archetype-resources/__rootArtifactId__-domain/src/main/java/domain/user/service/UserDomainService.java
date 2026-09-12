#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.user.service;

import org.springframework.stereotype.Service;

/**
 * 用户领域服务示例，承载单一业务域规则，不负责事务编排。
 */
@Service
public class UserDomainService {

    public String queryUserInfo(String request) {
        return "查询用户信息：" + request;
    }
}
