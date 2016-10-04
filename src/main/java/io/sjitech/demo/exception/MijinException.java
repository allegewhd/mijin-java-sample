package io.sjitech.demo.exception;

/**
 * Created by wang on 2016/07/28.
 */
public class MijinException extends AppException {
    public MijinException() {
    }

    public MijinException(String message) {
        super(message);
    }

    public MijinException(String message, Throwable cause) {
        super(message, cause);
    }

    public MijinException(Throwable cause) {
        super(cause);
    }

    public MijinException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
