package org.itburger.telegramupdatesdispatcher;

import lombok.extern.slf4j.Slf4j;
import org.itburger.telegramupdatesdispatcher.annotations.*;
import org.itburger.telegramupdatesdispatcher.generics.*;
import org.reflections.Reflections;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.neformat.telegramupdatesdispatcher.annotations.*;
import org.itburger.telegramupdatesdispatcher.exceptions.TelegramMiddlewareException;
import ru.neformat.telegramupdatesdispatcher.generics.*;
import org.itburger.telegramupdatesdispatcher.models.DefaultMiddlewareChain;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Главный диспетчер для маршрутизации Update-ов к нужным обработчикам.
 */
@Slf4j
public class TelegramUpdateDispatcher<U extends AbstractBotUser> {

    private final AbstractUserService<U> userService;
    private final Class<U> userClass;
    private final Map<Class<? extends Annotation>, List<MethodHandler>> handlers = new HashMap<>();
    private final ApplicationContext applicationContext;
    private final LocaleService localeService;
    private final List<TelegramMiddleware<U>> middlewares;

    public TelegramUpdateDispatcher(
            AbstractUserService<U> userService,
            Class<U> userClass,
            String updateHandlersPackage,
            ApplicationContext applicationContext,
            LocaleService localeService
    ) {
        this.middlewares = new ArrayList<>();
        this.userService = userService;
        this.userClass = userClass;
        this.applicationContext = applicationContext;
        this.localeService = localeService;
        initHandlers(updateHandlersPackage);
        sortHandlers();
    }

    public TelegramUpdateDispatcher(
            AbstractUserService<U> userService,
            Class<U> userClass,
            String updateHandlersPackage,
            ApplicationContext applicationContext
    ) {
        this.middlewares = new ArrayList<>();
        this.userService = userService;
        this.userClass = userClass;
        this.applicationContext = applicationContext;
        this.localeService = null;
        initHandlers(updateHandlersPackage);
        sortHandlers();
    }

    public TelegramUpdateDispatcher(
            AbstractUserService<U> userService,
            Class<U> userClass,
            String updateHandlersPackage,
            ApplicationContext applicationContext,
            LocaleService localeService,
            List<TelegramMiddleware<U>> middlewares
    ) {
        this.middlewares = middlewares;
        this.userService = userService;
        this.userClass = userClass;
        this.applicationContext = applicationContext;
        this.localeService = localeService;
        initHandlers(updateHandlersPackage);
        sortHandlers();
    }

    public TelegramUpdateDispatcher(
            AbstractUserService<U> userService,
            Class<U> userClass,
            String updateHandlersPackage,
            ApplicationContext applicationContext,
            List<TelegramMiddleware<U>> middlewares
    ) {
        this.middlewares = middlewares;
        this.userService = userService;
        this.userClass = userClass;
        this.applicationContext = applicationContext;
        this.localeService = null;
        initHandlers(updateHandlersPackage);
        sortHandlers();
    }

    private void sortHandlers() {
        List<MethodHandler> messageHandlers = handlers.getOrDefault(MessageHandler.class, new ArrayList<>());

        List<MethodHandler> sorted = messageHandlers.stream()
                .sorted(Comparator.comparingInt((MethodHandler mh) -> {
                    int score = 0;
                    try {
                        // если есть regex — большой приоритет
                        String regex = mh.getStringValue("regex");
                        if (regex != null && !regex.isEmpty()) score += 8;

                        // локализованное значение
                        String locKey = mh.getStringValue("localizedValueKey");
                        if (locKey != null && !locKey.isEmpty()) score += 4;

                        // простое value
                        String value = mh.getStringValue("value");
                        if (value != null && !value.isEmpty()) score += 2;

                        // requiredStates (через аннотацию, как у вас было)
                        Annotation annotation = mh.getAnnotation();
                        if (annotation != null) {
                            try {
                                Object o = annotation.annotationType().getMethod("requiredStates").invoke(annotation);
                                if (o instanceof String[]) {
                                    String[] array = (String[]) o;
                                    if (array.length > 0) score += 1;
                                }
                            } catch (NoSuchMethodException ignore) {
                                // аннотация может не иметь requiredStates — это нормально
                            }
                        }
                    } catch (Exception e) {
                        // при ошибке оставляем score как есть и логируем
                        log.error("Error while computing MethodHandler score for sorting", e);
                    }
                    // сортируем по убыванию score: поэтому возвращаем -score
                    return -score;
                }))
                .collect(Collectors.toList());

        // гарантируем изменяемый список при необходимости
        handlers.put(MessageHandler.class, new ArrayList<>(sorted));
    }

    private void initHandlers(String... scanPackages) {
        Reflections reflections = new Reflections((Object[]) scanPackages);

        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(UpdateHandler.class);
        for (Class<?> clazz : classes) {
            try {
                Object instance = null;

                try {
                    instance = applicationContext.getBean(clazz);
                } catch (NoSuchBeanDefinitionException e) {
                    // Бин не найден — создаём новый экземпляр вручную
                    try {
                        instance = clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception ex) {
                        throw new RuntimeException("Ошибка при инициализации обработчика: " + clazz.getName(), ex);
                    }
                }

                for (Method method : clazz.getDeclaredMethods()) {
                    for (Annotation annotation : method.getAnnotations()) {
                        if (isHandlerAnnotation(annotation)) {
                            try {
                                MethodHandler handler = new MethodHandler(instance, method, annotation, localeService);
                                handlers.computeIfAbsent(annotation.annotationType(), k -> new ArrayList<>())
                                        .add(handler);
                            }catch (IllegalArgumentException e){
                                log.error("Error initializing MethodHandler", e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Ошибка при инициализации обработчика: " + clazz, e);
            }
        }
    }

    public void dispatch(Update update, U user){
        Class wantedClass = null;

        if (update.hasMessage() && update.getMessage().isUserMessage()){
            if (update.getMessage().isCommand()){
                wantedClass = CommandHandler.class;
            }else if (update.getMessage().hasPhoto()){
                wantedClass = PhotoHandler.class;
            }else if (update.getMessage().hasDocument()){
                wantedClass = DocumentHandler.class;
            }else if (update.getMessage().hasVoice()){
                wantedClass = VoiceHandler.class;
            }else if (update.getMessage().hasAudio()){
                wantedClass = AudioHandler.class;
            }else if (update.getMessage().hasSuccessfulPayment()){
                wantedClass = SuccessfulPaymentHandler.class;
            }else {
                wantedClass = MessageHandler.class;
            }
        }else if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage().isUserMessage()){
            wantedClass = CallbackQueryHandler.class;
        }else if (update.hasInlineQuery()){
            wantedClass = InlineQueryHandler.class;
        }else if (update.hasPreCheckoutQuery()){
            wantedClass = PreCheckoutQueryHandler.class;
        }

        for (MethodHandler handler : handlers.getOrDefault(wantedClass, new ArrayList<>())) {
            try {
                if (handler.matches(update, user)) {
                    handler.invoke(update, user);
                    return;
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void dispatch(Update update) throws TelegramMiddlewareException {
        U user = null;
        try {
            Long userId = extractUserId(update);
            user = (userId != null) ? userService.findByTelegramId(userId) : null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        MiddlewareChain<U> chain = new DefaultMiddlewareChain<>(middlewares, this);
        chain.next(update, user);

    }

    private boolean isHandlerAnnotation(Annotation annotation) {
        return annotation instanceof MessageHandler ||
                annotation instanceof CommandHandler ||
                annotation instanceof CallbackQueryHandler ||
                annotation instanceof InlineQueryHandler ||
                annotation instanceof BusinessMessageHandler ||
                annotation instanceof BusinessCallbackQueryHandler ||
                annotation instanceof DocumentHandler ||
                annotation instanceof PhotoHandler ||
                annotation instanceof VoiceHandler ||
                annotation instanceof AudioHandler ||
                annotation instanceof SuccessfulPaymentHandler ||
                annotation instanceof PreCheckoutQueryHandler;
    }

    private Long extractUserId(Update update) {
        if (update.getMessage() != null && update.getMessage().getFrom() != null)
            return update.getMessage().getFrom().getId();
        if (update.getCallbackQuery() != null && update.getCallbackQuery().getFrom() != null)
            return update.getCallbackQuery().getFrom().getId();
        if (update.getInlineQuery() != null && update.getInlineQuery().getFrom() != null)
            return update.getInlineQuery().getFrom().getId();
        if (update.getPreCheckoutQuery() != null && update.getPreCheckoutQuery().getFrom() != null)
            return update.getPreCheckoutQuery().getFrom().getId();
        return null;
    }
}
