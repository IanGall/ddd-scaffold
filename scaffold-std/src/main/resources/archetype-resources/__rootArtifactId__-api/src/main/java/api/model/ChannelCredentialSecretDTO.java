#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.model;

import lombok.Data;
import java.io.Serializable;

@Data
public class ChannelCredentialSecretDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String channelCode;
    private String channelSecret;
    private Long secretVersion;
}
