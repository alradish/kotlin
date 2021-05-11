# DSL for Optimization framework
Цель: описать DSL для фреймворка оптимизаций
Что должен уметь ФО:
- Описать и найти кусок кода на котором можно применить оптимизацию(трансформацию)
- Описать новый код для вставки/замены
- Описать куда или вместо чего будет вставлен новый код

## DSL


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
### Новый код

Скорее всего уже существующий `IrBuilder` отлично справиться с этой задачей.

### Трансформация

## 1

Можно было бы описывать новый код прямо после кода который нужно заменить.

   Плюсы:

   * Просто в реализации т.к. почти всё что может понадобиться для создания нового кода находится в скоупе(ссылка на класс, параметры функции и т.д.)

   Минусы:
   * Может выглядеть запутанно
   * Возможно не интуитивно. Функция замены будет находиться где-то в середине матчинга, 
     хотя на самом деле она выполнится только если весь матчинг выполнился


### 1.1
```Kotlin
matchClass {
   matchFunction {

   } replaceWith getNewFunction(this@IrClass)
}
```
где
```Kotlin
infix fun Matcher.replaceWith(newElement: IrElement) {
    TODO()
}

fun getNewFunction(clazz: IrClass, f: IrFunction): IrFunction {
    TODO()
}
```

### 1.2

```Kotlin
matchClass {
   matchFunction {
       TODO()
   } replaceWith<IrFunction> { getNewFun(this@IrClass, this) }
}
```
где
```Kotlin
fun <F, T> Matcher<F>.replaceWith(generateNewElement: F.() -> T) {
    TODO()
}

fun getNewFunction(clazz: IrClass, f: IrFunction): IrFunction {
    TODO()
}
```

## 2

На этапе матчинга можно оставлять именованные метки

   Плюсы:
   * 

   Минусы:
   * Теряется информация о типах
   * Как сообщать об ошибках?


```Kotlin
matchClass {
   matchFunction {
       TODO()
   } mark("f", ReplaceContext(this@IrClass))
}
```

Далее где-то...

```Kotlin
replace("f") { context: ReplaceContext ->
    TODO()
}
```

где `mark` и `replace` со следующими сигнатурами

```Kotlin
fun <F> Matcher<F>.mark(id: String, context: ReplaceContext) {
   context.add(this.ir)
   TODO()
}

fun replace(id: String, newElement: (ReplaceContext) -> IrElement) {
   TODO()
}
```