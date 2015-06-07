package com.firebase.security.token;




import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;

/**
 * JWT encoder.
 *
 * @author vikrum
 */
public class JWTEncoder {

    private static final String TOKEN_SEP = ".";
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private static final String HMAC_256 = "HmacSHA256";

    /**
     * Encode and sign a set of claims.
     *
     * @param claims
     * @param secret
     * @return
     */
    public static String encode(JSONObject claims, String secret) {
        String encodedHeader = getCommonHeader();
        String encodedClaims = encodeJson(claims);

        String secureBits = new StringBuilder(encodedHeader).append(TOKEN_SEP).append(encodedClaims).toString();

        String sig = sign(secret, secureBits);

        return new StringBuilder(secureBits).append(TOKEN_SEP).append(sig).toString();
    }

    private static String sign(String secret, String secureBits) {
        try {
            Mac sha256_HMAC = Mac.getInstance(HMAC_256);
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(UTF8_CHARSET), HMAC_256);
            sha256_HMAC.init(secret_key);
            byte sig[] = sha256_HMAC.doFinal(secureBits.getBytes(UTF8_CHARSET));
            return encodeBase64URLSafeString(sig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getCommonHeader() {
        try {
            JSONObject headerJson = new JSONObject();
            headerJson.put("typ", "JWT");
            headerJson.put("alg", "HS256");
            return encodeJson(headerJson);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static String encodeJson(JSONObject jsonData) {
        return encodeBase64URLSafeString(jsonData.toString().getBytes(UTF8_CHARSET));
    }

    public static String encodeBase64URLSafeString(byte[] source){
        String encodedString = new String(Base64.encode(source, Base64.DEFAULT));
        String safeString = encodedString.replace('+','-').replace('/','_');
        return safeString;
    }
}
