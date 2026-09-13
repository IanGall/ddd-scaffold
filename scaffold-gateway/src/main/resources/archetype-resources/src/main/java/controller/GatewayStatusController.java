package ${package}.controller;

import cn.iantech.common.model.Response;
import ${package}.model.GatewayStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static ${package}.model.GatewayResponses.success;

/**
 * 管理端网关状态接口。
 */
@RestController
@RequestMapping("/api/admin/status")
public class GatewayStatusController {

    private final String applicationName;

    public GatewayStatusController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping
    public Response<GatewayStatus> status() {
        return success(new GatewayStatus(applicationName, "UP"));
    }
}
