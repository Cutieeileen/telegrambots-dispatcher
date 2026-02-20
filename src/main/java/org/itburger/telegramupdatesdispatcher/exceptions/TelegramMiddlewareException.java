package org.itburger.telegramupdatesdispatcher.exceptions;

public class TelegramMiddlewareException extends RuntimeException {
    public TelegramMiddlewareException(String message) {
        super(message);
    }

    public TelegramMiddlewareException(String message, Throwable cause) {
        super(message, cause);
    }

    public TelegramMiddlewareException(Throwable cause) {
        super(cause);
    }
}
