package org.itburger.telegramupdatesdispatcher;

import org.itburger.telegramupdatesdispatcher.annotations.*;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.neformat.telegramupdatesdispatcher.annotations.*;
import org.itburger.telegramupdatesdispatcher.generics.AbstractBotUser;
import org.itburger.telegramupdatesdispatcher.generics.LocaleService;
import org.itburger.telegramupdatesdispatcher.generics.UserState;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Pattern;

public class MethodHandler {
    private final Object instance;
    private final Method method;
    private final Annotation annotation;
    private final LocaleService localeService;

    public MethodHandler(Object instance, Method method, Annotation annotation, LocaleService localeService) {
        this.instance = instance;
        this.method = method;
        this.annotation = annotation;
        this.localeService = localeService;
        validateMethodSignature();
        validateMethodAvailability();
    }

    private void validateMethodSignature() {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 1 && params[0].equals(Update.class)) return;
        if (params.length == 2 && params[0].equals(Update.class)
                && AbstractBotUser.class.isAssignableFrom(params[1])) return;

        throw new IllegalArgumentException("Handler method " + method + " must have signature: (Update) or (Update, User)");
    }

    private void validateMethodAvailability(){
        if (!(!getStringValue("localizedValueKey").isEmpty() && localeService == null)) return;
        throw new IllegalArgumentException("Handler method " + method + " must have LocaleService initialized.");
    }

    public boolean matches(Update update, AbstractBotUser user) {
        try {

            if (annotation instanceof PreCheckoutQueryHandler && update.hasPreCheckoutQuery()) return true;

            if (annotation instanceof SuccessfulPaymentHandler && update.hasMessage() && update.getMessage().hasSuccessfulPayment()) return true;

            String[] requiredStates = (String[]) annotation.annotationType()
                    .getMethod("requiredStates")
                    .invoke(annotation);

            if (annotation instanceof PhotoHandler && update.getMessage() != null && update.getMessage().hasPhoto()){

                if (!checkAccess(user)) return false;
                if (!checkStates(user)) return false;
                return true;

            }

            if (annotation instanceof VoiceHandler && update.getMessage() != null && update.getMessage().hasVoice()){

                if (!checkAccess(user)) return false;
                if (!checkStates(user)) return false;
                return true;

            }

            if (annotation instanceof AudioHandler && update.getMessage() != null && update.getMessage().hasAudio()){

                if (!checkAccess(user)) return false;
                if (!checkStates(user)) return false;
                return true;

            }

            if (annotation instanceof DocumentHandler && update.getMessage() != null && update.getMessage().hasDocument()){

                if (!checkAccess(user)) return false;
                if (!checkStates(user)) return false;
                return true;

            }

            String textToMatch = extractTextFromUpdate(update);

            if (!checkAccess(user)) return false;
            if (!checkStates(user)) return false;

            // Regex имеет приоритет
            String regex = getStringValue("regex");
            if (!regex.isEmpty() && textToMatch != null) {
                return Pattern.matches(regex, textToMatch);
            }

            String localizedValueKey = getStringValue("localizedValueKey");
            if (!localizedValueKey.isEmpty() && textToMatch != null){
                String localizedValue = localeService.getText(user.getLangCode(), localizedValueKey);
                return textToMatch.equals(localizedValue);
            }

            // startsWith + value
            boolean startsWith = getBooleanValue("startsWith");
            String value = getStringValue("value");

            if (value.isEmpty() && annotation instanceof MessageHandler && requiredStates.length != 0) return true;

            if (textToMatch == null) return false;
            return startsWith ? textToMatch.startsWith(value) : textToMatch.equals(value);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String extractTextFromUpdate(Update update) {
        if (annotation instanceof MessageHandler || annotation instanceof CommandHandler) {
            if (update.getMessage() != null && update.getMessage().getText() != null) {
                return update.getMessage().getText();
            }
        } else if (annotation instanceof CallbackQueryHandler) {
            if (update.getCallbackQuery() != null) {
                return update.getCallbackQuery().getData();
            }
        } else if (annotation instanceof InlineQueryHandler) {
            if (update.getInlineQuery() != null) {
                return update.getInlineQuery().getQuery();
            }
        } else if (annotation instanceof BusinessMessageHandler || annotation instanceof BusinessCallbackQueryHandler) {
            return ""; // Пока не реализовано
        } else if (annotation instanceof DocumentHandler) {
            if (update.getMessage() != null && update.getMessage().getDocument() != null) {
                return update.getMessage().getDocument().getFileName();
            }
        } else {}
        return null;
    }

    private boolean checkAccess(AbstractBotUser user) throws Exception {
        boolean accessByUnknownUsers = getBooleanValue("accessByUnknownUsers");
        return user != null || accessByUnknownUsers;
    }

    private boolean checkStates(AbstractBotUser user) throws Exception {
        if (user == null) {
            return true;
        }

        // Получаем список строк из аннотации
        String[] requiredStates = (String[]) annotation.annotationType()
                .getMethod("requiredStates")
                .invoke(annotation);

        if (requiredStates.length == 0) return true;

        UserState userState = user.getState();
        if (userState == null) return false;

        // Проверяем совпадение по строковому идентификатору
        return Arrays.stream(requiredStates)
                .anyMatch(id -> id.equals(userState.getId()));
    }

    public Annotation getAnnotation(){
        return annotation;
    }

    public String getStringValue(String field) {
        try {
            Object val = annotation.annotationType().getMethod(field).invoke(annotation);
            return val == null ? "" : val.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean getBooleanValue(String field) {
        try {
            Object val = annotation.annotationType().getMethod(field).invoke(annotation);
            return val instanceof Boolean && (Boolean) val;
        } catch (Exception e) {
            return false;
        }
    }

    public void invoke(Update update, AbstractBotUser user) throws Exception {
        if (method.getParameterCount() == 2) {
            method.invoke(instance, update, user);
        } else {
            method.invoke(instance, update);
        }
    }
}

