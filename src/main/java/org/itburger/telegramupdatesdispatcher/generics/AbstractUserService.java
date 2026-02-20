package org.itburger.telegramupdatesdispatcher.generics;

public abstract class AbstractUserService<U extends AbstractBotUser> {
    public abstract U findByTelegramId(Long telegramId);
    public abstract U save(U user);
}

