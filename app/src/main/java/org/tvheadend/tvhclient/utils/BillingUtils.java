package org.tvheadend.tvhclient.utils;

import android.content.Context;
import android.util.Base64;

import org.tvheadend.tvhclient.BuildConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BillingUtils {
    private static final String TAG = BillingUtils.class.getSimpleName();

    // TODO public key missing

    private BillingUtils() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Returns the public key that is required to make in-app purchases. The key
     * which is ciphered is located in the assets folder.
     *
     * @return Value that is in the public key file
     */
    public static String getPublicKey(Context context) {
        StringBuilder sb = new StringBuilder();
        try {
            String keyData;
            InputStream is = context.getAssets().open("public_key");
            BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((keyData = in.readLine()) != null) {
                sb.append(keyData);
            }
            in.close();
        } catch (Exception ex) {
            // NOP
        }

        final String SALT = BuildConfig.PUBLIC_KEY_SALT;

        // Note that this is not plain public key but public key encoded with
        // x() method. As symmetric ciphering is used in x() the same method is
        // used for both ciphering and deciphering. Additionally result of the
        // ciphering is converted to Base64 string => for deciphering with need
        // to convert it back. Generally, x(fromBase64(toBase64(x(PK, salt))),
        // salt) == PK To cipher use toX(), to decipher - fromX()
        return fromX(sb.toString(), SALT);
    }

    /**
     * Method deciphers previously ciphered message
     *
     * @param message ciphered message
     * @param salt    salt which was used for ciphering
     * @return deciphered message
     */
    private static String fromX(String message, String salt) {
        return x(new String(Base64.decode(message, 0)), salt);
    }

    /**
     * Symmetric algorithm used for ciphering/deciphering. Note that in your application
     * you probably want to modify the algorithm used for ciphering/deciphering.
     *
     * @param message message
     * @param salt    salt
     * @return ciphered/deciphered message
     */
    private static String x(String message, String salt) {
        final char[] m = message.toCharArray();
        final char[] s = salt.toCharArray();

        final int ml = m.length;
        final int sl = s.length;
        final char[] result = new char[ml];

        for (int i = 0; i < ml; i++) {
            result[i] = (char) (m[i] ^ s[i % sl]);
        }
        return new String(result);
    }
}
