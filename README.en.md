# TelegramBots Dispatcher

[Русская версия](README.md)

A library for convenient routing of Telegram bot updates using Java annotations.
Based on the official [TelegramBots](https://github.com/rubenlagus/TelegramBots) library.

## Features

- Declarative definition of handlers for various types of Telegram events using annotations
- Automatic routing of updates to the corresponding handlers
- Flexible filtering to ensure guaranteed event delivery to the correct handler
- Integration with Spring Boot
- Middleware for processing each incoming event before passing it to the target handler
- Native support for multi-language bots

## Getting Started

### Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.itburger</groupId>
    <artifactId>telegrambots-dispatcher</artifactId>
    <version>1.0.2</version>
</dependency>
```

### Basic Steps

1. Create a user class inheriting from `AbstractBotUser`
2. Implement a user service inheriting from `AbstractUserService`
3. Create handler classes with the `@UpdateHandler` annotation
4. Define handler methods with the corresponding annotations
5. Configure the `TelegramUpdateDispatcher`

## Creating the User Model

```java
public class BotUser extends AbstractBotUser {
    // Additional fields and methods for the user
    private UserState state;

    @Override
    public Enum<?> getState() {
        return state;
    }

    public void setState(UserState state) {
        this.state = state;
    }

    // Other necessary methods
}

public enum UserState {
    START, WAITING_NAME, WAITING_CONFIRMATION
}
```

## Implementing the User Service

```java
@Service
public class UserService extends AbstractUserService<BotUser> {

    // User storage (can be replaced with a repository)
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

    // Other methods for working with users
}
```

## Creating Handlers

```java
@UpdateHandler
public class MessageHandlers {

    @CommandHandler("/start")
    public void handleStart(Update update, BotUser user) {
        // Handle /start command
        // Logic for sending a welcome message
    }

    @MessageHandler(regex = ".*hello.*")
    public void handleHello(Update update, BotUser user) {
        // Handle messages containing the word "hello"
    }

    @MessageHandler
    @TelegramUpdatesConsumer
    public void handleDefaultMessage(Update update, BotUser user) {
        // Handle all other messages
    }

    @CallbackQueryHandler(value = "action_confirm")
    public void handleConfirmAction(Update update, BotUser user) {
        // Handle callback query with data "action_confirm"
    }

    @InlineQueryHandler(startsWith = true)
    public void handleInlineQuery(Update update, BotUser user) {
        // Handle inline queries
    }

    @MessageHandler(requiredStates = {UserState.WAITING_NAME})
    public void handleNameInput(Update update, BotUser user) {
        // Handle name input when the user is in the WAITING_NAME state
    }
}
```

## Configuring the Dispatcher

### In a Spring Application

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
                "com.your.package.handlers", // package with handlers
                context
        );
    }

    // Configure LongPollingBot or WebhookBot
    @Bean
    public TelegramLongPollingBot telegramBot(TelegramUpdateDispatcher<BotUser> dispatcher) {
        return new TelegramLongPollingBot() {
            @Override
            public void onUpdateReceived(Update update) {
                dispatcher.dispatch(update);
            }

            // other bot methods
        };
    }
}
```

### In a Non-Spring Application

```java
public class Bot {
    private final TelegramUpdateDispatcher<BotUser> dispatcher;

    public Bot() {
        UserService userService = new UserService();
        dispatcher = new TelegramUpdateDispatcher<>(
                userService,
                BotUser.class,
                "com.your.package.handlers",
                null // ApplicationContext is not used
        );

        // Bot configuration
    }

    public void processUpdate(Update update) {
        dispatcher.dispatch(update);
    }
}
```

Further documentation on library features will be added later.

## License
MIT
