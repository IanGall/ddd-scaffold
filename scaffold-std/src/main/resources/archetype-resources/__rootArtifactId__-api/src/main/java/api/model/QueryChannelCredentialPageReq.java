#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.model;

import cn.iantech.common.model.PageRequest;
import lombok.Data;

@Data
public class QueryChannelCredentialPageReq extends PageRequest {
    private static final long serialVersionUID = 1L;
    private String channelCode;
    private String channelName;
    private Boolean status;
}
