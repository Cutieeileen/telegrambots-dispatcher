package org.itburger.telegramupdatesdispatcher.models;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.itburger.telegramupdatesdispatcher.TelegramUpdateDispatcher;
import org.itburger.telegramupdatesdispatcher.exceptions.TelegramMiddlewareException;
import org.itburger.telegramupdatesdispatcher.generics.AbstractBotUser;
import org.itburger.telegramupdatesdispatcher.generics.MiddlewareChain;
import org.itburger.telegramupdatesdispatcher.generics.TelegramMiddleware;

import java.util.List;

@Slf4j
public class DefaultMiddlewareChain<U extends AbstractBotUser> implements MiddlewareChain<U> {

    private final List<TelegramMiddleware<U>> middlewares;
    private final TelegramUpdateDispatcher<U> handlerInvoker;
    private int index = 0;

    public DefaultMiddlewareChain(
            List<TelegramMiddleware<U>> middlewares,
            TelegramUpdateDispatcher<U> handlerInvoker
    ) {
        this.middlewares = middlewares;
        this.handlerInvoker = handlerInvoker;
    }

    @Override
    public void next(Update update, U user) throws TelegramMiddlewareException {

        if (index < middlewares.size()) {
            TelegramMiddleware<U> middleware = middlewares.get(index);
            index++;
            middleware.process(update, user, this);
        } else {
            handlerInvoker.dispatch(update, user);
        }
    }

}

