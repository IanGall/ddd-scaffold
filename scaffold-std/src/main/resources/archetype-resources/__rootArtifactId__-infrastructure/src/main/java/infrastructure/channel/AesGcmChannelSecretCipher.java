#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infrastructure.channel;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/** 渠道密钥 AES-256-GCM 加密示例，主密钥由部署环境注入。 */
public final class AesGcmChannelSecretCipher {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmChannelSecretCipher(String base64MasterKey) {
        byte[] bytes = Base64.getDecoder().decode(base64MasterKey);
        if (bytes.length != 32) throw new IllegalArgumentException("主密钥必须为256位");
        this.key = new SecretKeySpec(bytes, "AES");
    }

    public EncryptedSecret encrypt(String plaintext, String aad) {
        try {
            byte[] iv = new byte[12]; random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            return new EncryptedSecret(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)), iv);
        } catch (Exception exception) {
            throw new IllegalStateException("渠道密钥加密失败", exception);
        }
    }

    public record EncryptedSecret(byte[] ciphertext, byte[] iv) { }
}
