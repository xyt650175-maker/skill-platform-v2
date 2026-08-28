package com.company.llmaif.common.security;

/**
 * 企业凭证加密边界。生产环境的密钥应由企业 KMS/密钥管理服务注入，业务代码
 * 只处理密文，严禁将 Git 令牌或明文密钥写入数据库、日志和浏览器响应。
 */
public interface CredentialCipher {
    String encrypt(String plaintext);
    String decrypt(String ciphertext);
}
