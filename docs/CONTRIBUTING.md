# Как работать в проекте

## Ветки
- `main` — production-ready, только через PR
- `dev` — основная ветка разработки
- `feature/TASK-XXX-short-description` — фича-ветки

## Процесс PR
1. Создать ветку от `dev`
2. Написать код + тесты
3. `mvn verify` должен проходить (включая Checkstyle, JaCoCo ≥58%)
4. Создать PR на `dev` → назначить ревьюера
5. Минимум 1 approve; после merge — удалить ветку

## Чеклист перед PR
- [ ] `mvn checkstyle:check` — нет ошибок
- [ ] `mvn test` — все тесты зелёные
- [ ] Новый код покрыт тестами
- [ ] Javadoc на публичные методы в `island-engine`
- [ ] Нет `TODO` без тикета

## Код-стиль
Проект использует Checkstyle (конфиг: `checkstyle.xml`).
Проверка запускается автоматически при `mvn verify`.

### Запреты
- ❌ Star imports (`import com.island.nature.*`)
- ❌ Прямое использование `engine.internal` из плагинов
- ❌ `System.out.println` — только `log.info/debug/warn/error`
- ❌ Изменение публичного API движка без обсуждения

## Commit messages
Формат: `type(scope): description`  
Типы: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`  
Пример: `feat(nature): add BiomassGrowthSystem with GrowthComponent`
