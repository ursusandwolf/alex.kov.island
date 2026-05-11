# WebSocket API (планирование)

Стриминг состояния симуляции в реальном времени.

## Подключение

URL: `ws://localhost:8080/ws/simulation`

Параметры запроса:
- `plugin`: `nature` | `simcity`
- `interval`: частота отправки снимков (например, `5` — каждый 5-й тик).

## Сообщения от сервера (Server -> Client)

### 1. `tick_snapshot`
Отправляется периодически согласно `interval`. Содержит `WorldSnapshot`.

```json
{
  "type": "tick_snapshot",
  "payload": { ... }
}
```

### 2. `entity_event`
Отправляется мгновенно при возникновении важного события в мире (если включено в настройках).

```json
{
  "type": "entity_event",
  "eventName": "ANIMAL_DIED",
  "payload": {
    "species": "wolf",
    "cause": "HUNGER",
    "x": 10,
    "y": 12
  }
}
```

### 3. `simulation_state`
Изменение статуса симуляции.

```json
{
  "type": "simulation_state",
  "state": "PAUSED" | "RUNNING" | "STOPPED",
  "reason": "USER_ACTION" | "EXTINCTION"
}
```

## Сообщения от клиента (Client -> Server)

### 1. `set_speed`
Изменение скорости симуляции (длительность тика в мс).

```json
{
  "type": "set_speed",
  "tickDurationMs": 100
}
```

### 2. `control`
Управление жизненным циклом.

```json
{
  "type": "control",
  "command": "PAUSE" | "RESUME" | "STEP"
}
```
