package com.company.llmaif.common.security;

import com.company.llmaif.common.AgentException;
import com.company.llmaif.config.LlmaifProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** AES-GCM（带完整性校验）的凭证密文实现。 */
@Component
@RequiredArgsConstructor
public class AesGcmCredentialCipher implements CredentialCipher {
    private static final String VERSION = "v1";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final LlmaifProperties properties;

    @Override
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return VERSION + ":" + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new AgentException("企业 Git 凭证加密失败");
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        try {
            String[] values = StringUtils.defaultString(ciphertext).split(":", 3);
            if (values.length != 3 || !VERSION.equals(values[0])) throw new AgentException("Git 凭证密文格式无效");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, Base64.getDecoder().decode(values[1])));
            return new String(cipher.doFinal(Base64.getDecoder().decode(values[2])), StandardCharsets.UTF_8);
        } catch (AgentException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentException("Git 凭证无法解密，请联系管理员检查企业密钥配置");
        }
    }

    private SecretKeySpec key() throws Exception {
        String material = properties.getSecurity().getCredentialEncryptionKey();
        if (StringUtils.isBlank(material)) throw new AgentException("未配置 GIT_CREDENTIAL_ENCRYPTION_KEY，禁止保存企业 Git 凭证");
        return new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8)), "AES");
    }
}
