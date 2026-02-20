package org.itburger.telegramupdatesdispatcher.generics;

public abstract class AbstractBotUser<S extends UserState> {

    public abstract S getState();

    public abstract void setState(S state);

    public abstract Long getTelegramId();

    public abstract void setTelegramId(Long telegramId);

    public abstract String getLangCode();

}

