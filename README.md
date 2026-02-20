# TelegramBots Dispatcher

[English version](README.en.md)

Библиотека для удобной маршрутизации обновлений Telegram-ботов с использованием аннотаций Java.
Базируется на официальной библиотеке [TelegramBots](https://github.com/rubenlagus/TelegramBots).

## Возможности

- Декларативное определение обработчиков различных типов событий Telegram с помощью аннотаций
- Автоматическая маршрутизация обновлений к соответствующим обработчикам
- Гибкая фильтрация для гарантированной доставки события в нужный обработчик
- Интеграция с Spring Boot
- Middleware для обработки каждого входящего события перед передачей в целевой обработчик
- Нативная поддержка мультиязычности

## Начало работы

### Подключение библиотеки

Добавьте зависимость в ваш pom.xml:

```xml
<dependency>
    <groupId>org.itburger</groupId>
    <artifactId>telegrambots-dispatcher</artifactId>
    <version>1.0.2</version>
</dependency>
```

### Основные шаги использования

1. Создайте класс пользователя, наследующийся от `AbstractBotUser`
2. Реализуйте сервис пользователей, наследуясь от `AbstractUserService`
3. Создайте классы обработчиков с аннотацией `@UpdateHandler`
4. Определите методы-обработчики с соответствующими аннотациями
5. Настройте диспетчер обновлений `TelegramUpdateDispatcher`

## Создание модели пользователя

```java
public class BotUser extends AbstractBotUser {
    // Дополнительные поля и методы пользователя
    private UserState state;

    @Override
    public Enum<?> getState() {
        return state;
    }

    public void setState(UserState state) {
        this.state = state;
    }

    // Другие необходимые методы
}

public enum UserState {
    START, WAITING_NAME, WAITING_CONFIRMATION
}
```

## Реализация сервиса пользователей

```java
@Service
public class UserService extends AbstractUserService<BotUser> {

    // Хранилище пользователей (можно заменить на репозиторий)
    private final Map<Long, BotUser> users = new ConcurrentHashMap<>();

    @Override
    public BotUser findByTelegramId(Long telegramId) {
        return users.computeIfAbsent(telegramId, id -> {
            BotUser user = new BotUser();
            user.setTelegramId(id);
            user.setState(UserState.START);
            return user;
        });
    }

    // Другие методы для работы с пользователями
}
```

## Создание обработчиков

```java
@UpdateHandler
public class MessageHandlers {

    @CommandHandler("/start")
    public void handleStart(Update update, BotUser user) {
        // Обработка команды /start
        // Логика отправки приветственного сообщения
    }

    @MessageHandler(regex = ".*привет.*")
    public void handleHello(Update update, BotUser user) {
        // Обработка сообщений, содержащих слово "привет"
    }

    @MessageHandler
    @TelegramUpdatesConsumer
    public void handleDefaultMessage(Update update, BotUser user) {
        // Обработка всех остальных сообщений
    }

    @CallbackQueryHandler(value = "action_confirm")
    public void handleConfirmAction(Update update, BotUser user) {
        // Обработка callback-запроса с данными "action_confirm"
    }

    @InlineQueryHandler(startsWith = true)
    public void handleInlineQuery(Update update, BotUser user) {
        // Обработка inline-запросов
    }

    @MessageHandler(requiredStates = {UserState.WAITING_NAME})
    public void handleNameInput(Update update, BotUser user) {
        // Обработка ввода имени, когда пользователь находится в состоянии WAITING_NAME
    }
}
```

## Настройка диспетчера

### В Spring-приложении

```java
@Configuration
public class BotConfig {

    @Bean
    public TelegramUpdateDispatcher<BotUser> telegramUpdateDispatcher(
            UserService userService,
            ApplicationContext context
    ) {
        return new TelegramUpdateDispatcher<>(
                userService,
                BotUser.class,
                "ru.your.package.handlers", // пакет с обработчиками
                context
        );
    }

    // Настройка LongPollingBot или WebhookBot
    @Bean
    public TelegramLongPollingBot telegramBot(TelegramUpdateDispatcher<BotUser> dispatcher) {
        return new TelegramLongPollingBot() {
            @Override
            public void onUpdateReceived(Update update) {
                dispatcher.dispatch(update);
            }

            // другие методы бота
        };
    }
}
```

### В приложении без Spring

```java
public class Bot {
    private final TelegramUpdateDispatcher<BotUser> dispatcher;

    public Bot() {
        UserService userService = new UserService();
        dispatcher = new TelegramUpdateDispatcher<>(
                userService,
                BotUser.class,
                "ru.your.package.handlers",
                null // ApplicationContext не используется
        );

        // Настройка бота
    }

    public void processUpdate(Update update) {
        dispatcher.dispatch(update);
    }
}
```

Позже будет добавлена документация к функционалу библиотеки.

## Лицензия
MIT