package com.example.clockin.volley;

public interface CertificateConfig {
    public static final String KEY_STORE_TYPE_BKS = "bks";
    public static final String KEY_STORE_TYPE_P12 = "PKCS12";
    public static final String keyStoreFileName = "client.key.p12";
    public static final String keyStorePassword = "";//"123456";
    public static final String trustStoreFileName = "my_ca.cer";
    public static final String trustStorePassword = "";//"123456";
    public static final String alias = null;//"client";
}