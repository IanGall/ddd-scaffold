#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infrastructure.channel;

import cn.iantech.redis.IRedisService;
import java.time.Duration;

/** Redis SET NX 防重放存储。Redis 异常必须向上抛出，认证链路保持 fail-closed。 */
public final class RedisChannelReplayStore {
    private static final String PREFIX = "channel:hmac:replay:";
    private final IRedisService redisService;

    public RedisChannelReplayStore(IRedisService redisService) { this.redisService = redisService; }

    public boolean markIfAbsent(String replayKey, Duration ttl) {
        return redisService.setIfAbsent(PREFIX + replayKey, "1", ttl);
    }
}
