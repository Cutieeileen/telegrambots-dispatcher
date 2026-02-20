package org.itburger.telegramupdatesdispatcher.generics;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.itburger.telegramupdatesdispatcher.exceptions.TelegramMiddlewareException;

public interface TelegramMiddleware<U extends AbstractBotUser> {
    void process(Update update, U user, MiddlewareChain<U> chain) throws TelegramMiddlewareException;
}
