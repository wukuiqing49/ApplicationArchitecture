package com.wkq.base.reflect

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Resolve the generic class argument from a superclass chain.
 */
@Suppress("UNCHECKED_CAST")
fun <T> resolveGenericClass(instance: Any, index: Int): Class<T> {
    val parameterizedType = findParameterizedType(instance.javaClass.genericSuperclass)
    val typeArgument = parameterizedType.actualTypeArguments.getOrNull(index)
        ?: throw IllegalStateException(
            "Missing generic type argument at index $index for ${instance.javaClass.name}"
        )

    return when (typeArgument) {
        is Class<*> -> typeArgument as Class<T>
        is ParameterizedType -> typeArgument.rawType as Class<T>
        else -> throw IllegalStateException(
            "Unsupported generic type argument $typeArgument for ${instance.javaClass.name}"
        )
    }
}

/**
 * Walk up the superclass chain until we find a ParameterizedType.
 */
tailrec fun findParameterizedType(type: Type?): ParameterizedType {
    return when (type) {
        is ParameterizedType -> type
        is Class<*> -> {
            val superclass = type.genericSuperclass
                ?: throw IllegalStateException("No generic superclass found for ${type.name}")
            findParameterizedType(superclass)
        }
        else -> throw IllegalStateException("Unable to resolve parameterized type from $type")
    }
}
