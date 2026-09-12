#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.cases.user.service;

import ${package}.domain.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCaseService {

    private final UserDomainService userDomainService;

    @Transactional(readOnly = true)
    public String queryUserInfo(String request) {
        log.info("查询用户信息，请求参数：{}", request);
        return userDomainService.queryUserInfo(request);
    }

}
