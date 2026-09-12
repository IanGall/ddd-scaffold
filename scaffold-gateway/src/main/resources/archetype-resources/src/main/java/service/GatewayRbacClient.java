package ${package}.service;

import cn.iantech.api.IPlatformAccountService;
import cn.iantech.api.model.rbac.PlatformCreateAccountReq;
import cn.iantech.api.model.rbac.RbacAccountDTO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import static cn.iantech.common.constant.Constants.ResponseCode.RPC_ERROR;

/**
 * 网关到标准工程的平台主账号 RPC 客户端。
 */
@Component
public class GatewayRbacClient {

    @DubboReference(version = "1.0.0", protocol = "tri", timeout = 3000, retries = 0, check = false)
    private IPlatformAccountService platformAccountService;

    public RbacAccountDTO createAccount(PlatformCreateAccountReq request) {
        return RpcCallGuard.call(() -> platformAccountService.createAccount(request), RPC_ERROR);
    }
}
