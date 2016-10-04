package io.sjitech.demo.util;

import io.sjitech.demo.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by wang on 2016/08/25.
 */
public class CommonUtil {

    private static final Logger log = LoggerFactory.getLogger(CommonUtil.class);

    public static String urlEncode(final String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("could not encode url encode string");
        }
    }

    public static String urlDecode(final String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("could not decode url encode string");
        }
    }

    public static String toHexString(byte[] bytes) {
        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < bytes.length; i++) {
//            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            sb.append(Integer.toHexString(0xFF & bytes[i]));
        }

        return sb.toString();
    }

    public static String getTextSha256sum(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            md.update(text.getBytes());

            byte byteData[] = md.digest();

            return toHexString(byteData);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 digest algorithm not available!", e);
            throw new AppException(e);
        }
    }

    public static String getFileSha256sum(String fileName) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            FileInputStream fis = new FileInputStream(fileName);

            byte[] dataBytes = new byte[1024];

            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }

            byte[] sumBytes = md.digest();

            return toHexString(sumBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 digest algorithm not available!", e);
            throw new AppException(e);
        } catch (IOException ioe) {
            log.error("file read failed!", ioe);
            throw new AppException(ioe);
        }
    }
}
