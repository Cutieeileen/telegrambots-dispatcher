# Telegram Updates Dispatcher

Библиотека для удобной маршрутизации обновлений Telegram-ботов с использованием аннотаций Java.

## Возможности

- Декларативное определение обработчиков различных типов обновлений Telegram через аннотации
- Автоматическая маршрутизация обновлений к соответствующим обработчикам
- Фильтрация обновлений по тексту, регулярным выражениям, состоянию пользователя
- Поддержка различных типов обновлений: сообщения, команды, callback-запросы, inline-запросы и бизнес-сообщения
- Интеграция с Spring Framework (опционально)

## Начало работы

### Подключение библиотеки

Добавьте зависимость в ваш pom.xml:

```xml
<dependency>
    <groupId>ru.neformat</groupId>
    <artifactId>telegram-updates-dispatcher</artifactId>
    <version>1.0.0</version>
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

## Доступные аннотации

### @UpdateHandler
Отмечает класс как содержащий обработчики обновлений Telegram.

### @MessageHandler
Обрабатывает текстовые сообщения. Параметры:
- `value`: точное совпадение текста
- `regex`: регулярное выражение для фильтрации
- `startsWith`: true для проверки начала сообщения
- `accessByUnknownUsers`: разрешить доступ незарегистрированным пользователям
- `requiredStates`: состояния пользователя, при которых сработает обработчик

### @CommandHandler
Обрабатывает команды бота. Имеет те же параметры, что и @MessageHandler.

### @CallbackQueryHandler
Обрабатывает callback-запросы от inline-кнопок. Параметры аналогичны @MessageHandler.

### @InlineQueryHandler
Обрабатывает inline-запросы. Параметры:
- `regex`: фильтр по тексту запроса
- `startsWith`: проверка начала текста запроса
- `accessByUnknownUsers`: разрешить доступ незарегистрированным пользователям
- `requiredStates`: требуемые состояния пользователя

### @BusinessMessageHandler и @BusinessCallbackQueryHandler
Обрабатывают бизнес-сообщения и callback-запросы.

## Жизненный цикл обработки обновлений

1. Update поступает в метод `dispatch()` диспетчера
2. Диспетчер извлекает ID пользователя и ищет/создает пользователя через сервис
3. Перебираются все зарегистрированные обработчики
4. Для каждого обработчика проверяется соответствие критериям (текст, состояние и т.д.)
5. Первый подходящий обработчик вызывается с параметрами Update и пользователь

## Советы и рекомендации

- Используйте общий обработчик с аннотацией `@MessageHandler` без параметров в качестве fallback
- Располагайте обработчики в порядке от наиболее специфичных к наиболее общим
- Для управления состоянием диалога используйте `requiredStates` и обновляйте состояние пользователя
- Создавайте отдельные классы обработчиков для разных функциональных групп

## Лицензия

МИТ