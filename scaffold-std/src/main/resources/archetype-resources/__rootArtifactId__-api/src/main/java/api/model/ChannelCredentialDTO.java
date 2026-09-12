#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChannelCredentialDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String channelCode;
    private String channelName;
    private Long secretVersion;
    private Boolean status;
    private LocalDateTime lastRotatedAt;
}
