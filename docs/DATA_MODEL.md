# Модель данных для Frontend

## WorldSnapshot (снимок состояния мира)

Создаётся в конце каждого тика в фазе `POSTPROCESS`. Frontend получает его для рендеринга сетки и обновления графиков.

```json
{
  "tickCount": 142,
  "width": 20,
  "height": 20,
  "totalEntityCount": 1284,
  "metrics": {
    "wolf_count": 12,
    "rabbit_count": 84,
    "bear_count": 3,
    "plant_biomass": 124000,
    "total_deaths": 5,
    "total_births": 8
  },
  "nodes": [
    {
      "coordinates": "0,0",
      "topSpeciesCode": "rabbit",
      "topSpeciesIsPlant": false,
      "hasOrganisms": true
    }
  ]
}
```

### Детали полей

- **tickCount**: Порядковый номер шага симуляции.
- **metrics**: Словарь ключ-значение. Состав зависит от активного плагина.
  - *Nature:* `wolf_count`, `rabbit_count`, `plant_biomass`, `total_deaths`.
  - *SimCity:* `population`, `happiness`, `budget`, `pollution_avg`.
- **nodes**: Плоский массив ячеек размера `width * height`.
  - Индекс в массиве: `index = y * width + x`.
  - **topSpeciesCode**: Строковой идентификатор вида, которого в ячейке больше всего (для иконок).
  - **hasOrganisms**: Флаг для быстрой отрисовки пустых/занятых ячеек.

## NodeSnapshot (детальное состояние ячейки)

Запрашивается отдельно при клике пользователя на ячейку.

```json
{
  "x": 3,
  "y": 5,
  "entities": [
    {
      "id": 1024,
      "type": "wolf",
      "health": 85,
      "energy": 60,
      "age": 5
    }
  ],
  "environmentalData": {
    "pollution": 12,
    "desirability": 75
  }
}
```

## Маппинг видов (Species Codes)

| Код | Вид (Nature) | Код | Здание (SimCity) |
|---|---|---|---|
| `wolf` | Волк | `res_low` | Жилье (низкая плотность) |
| `rabbit` | Кролик | `ind_high` | Промзона (высокая) |
| `plant` | Растение | `road` | Дорога |
| `bear` | Медведь | `power_plant` | Электростанция |
