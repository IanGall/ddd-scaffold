#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api;

import ${package}.api.model.*;
import cn.iantech.common.exception.AppException;
import java.util.List;

public interface IChannelCredentialService {
    ChannelCredentialSecretDTO create(CreateChannelCredentialReq req) throws AppException;
    ChannelCredentialPageDTO queryPage(QueryChannelCredentialPageReq req) throws AppException;
    List<ChannelDataScopeDTO> queryDataScopes(QueryChannelDataScopesReq req) throws AppException;
    List<ChannelDataScopeDTO> replaceDataScopes(ReplaceChannelDataScopesReq req) throws AppException;
}
