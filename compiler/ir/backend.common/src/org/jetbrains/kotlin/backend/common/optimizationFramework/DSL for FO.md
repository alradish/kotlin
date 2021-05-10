# DSL for Optimization framefork
Цель: описать DSL для фреймворка оптимизаций
Что должен уметь ФО:
- Описать и найти кусок кода на котором можно применить оптимизацию(трансформацию)
- 
### Матчер
```Kotlin
matchFile {
    matchClass {
        // Вместо отдельного блока под всё можно сделать универсальный `match {...}` 
        // с ресивиром. В данном случае `IrClass`
        config {
            // Штуки чтобы вычислить нужный класс по тиму:
            //    это интерфейс? Наследуется от? Типовые параметры? и т.д.
            // Возможно блок конфиг не нужен
            classKind { it == ClassKind.INTERFACE }
            isCompanion()
            isInner()
            isData()
            isExternal()
            isInline()
            isExpect()
            isFun()
            matchSuperTypes { ... }
            matchThisReceiver { ... }
            // и т.д.
        }

        // Проматчить тело класса. Пока не понятно как лучше сделать
        // 1)   Можно утвердить что внутри блока `matchClass` важен порядок и использовать констуркции вида
        //      `matchConstructor` `matchProperty` `matchFunction` `matchCompanion` `anything`
        // 2)   TODO Придумать другой способ
        matchProperty {
            // TODO   
        }
        anything()
        matchFuntion {
            isInline()
            isExternal()
            isExpect()
            matchReturnType { ... }
            matchDispatchReceiverParameter { ... }
            matchExtensionReceiverParameter { ... }
            // TODO
        }
    }
}
```

### Трансформация