package org.itburger.telegramupdatesdispatcher.generics;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface MiddlewareChain<U extends AbstractBotUser> {
    void next(Update update, U user);
}
