# Onboarding: первые шаги

## Требования
- Java 21 (проверить: `java --version`)
- Maven 3.8+ (проверить: `mvn --version`)
- IDE: IntelliJ IDEA рекомендуется (Lombok и JPMS поддержка из коробки)

## Шаг 1: Клонировать и собрать (10 минут)
git clone https://github.com/ursusandwolf/alex.kov.island.git
cd alex.kov.island
git checkout dev
mvn clean verify        # ← должно завершиться BUILD SUCCESS

## Шаг 2: Запустить симуляцию
mvn exec:java -pl island-app -Dexec.mainClass="com.island.NatureLauncher"

# В консоли вы увидите:
# Тик 1: 🐺 Wolves: 12 | 🐇 Rabbits: 84 | 🌿 Plants: 1240
# ...

## Шаг 3: Прочитать за 1 час
1. docs/GLOSSARY.md              — ключевые термины
2. docs/ARCHITECTURE.md          — общая картина
3. docs/engine/ECS_GUIDE.md     — как устроены сущности

## Шаг 4: Первая задача — добавить логирование
Измените `island-nature/src/main/java/.../service/AnimalHealthSystem.java`:
найдите метод `process()` и добавьте `log.debug(...)` для отладки.
Запустите тесты: `mvn test -pl island-nature`

## Структура проекта
island-engine/    — движок (НЕ трогать без согласования)
island-nature/    — плагин экосистемы
island-simcity/   — плагин городского симулятора
island-app/       — точки входа (Launcher-ы), ArchUnit-тесты

## Куда обращаться
- Код-вопросы: ревью PR или обсуждение в issue
- Архитектурные решения: см. docs/adr/
