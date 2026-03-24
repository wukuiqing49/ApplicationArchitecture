package com.wkq.router.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class RouteProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return emptyList()
        val routeName = "com.wkq.router.annotation.Route"
        val serviceName = "com.wkq.router.annotation.ProvideService"
        val interceptorName = "com.wkq.router.annotation.Interceptor"
        
        val routeSymbols = resolver.getSymbolsWithAnnotation(routeName)
        val serviceSymbols = resolver.getSymbolsWithAnnotation(serviceName)
        val interceptorSymbols = resolver.getSymbolsWithAnnotation(interceptorName)
        
        val ret = (routeSymbols + serviceSymbols + interceptorSymbols + resolver.getSymbolsWithAnnotation("com.wkq.router.annotation.Param")).filter { !it.validate() }.toList()
        
        val annotatedRoutes = routeSymbols.filterIsInstance<KSClassDeclaration>().toList()
        val annotatedServices = serviceSymbols.filterIsInstance<KSClassDeclaration>().toList()
        val annotatedInterceptors = interceptorSymbols.filterIsInstance<KSClassDeclaration>().toList()
        val annotatedParams = resolver.getSymbolsWithAnnotation("com.wkq.router.annotation.Param")
            .filterIsInstance<KSPropertyDeclaration>().toList()
        
        if (annotatedRoutes.isEmpty() && annotatedServices.isEmpty() && annotatedInterceptors.isEmpty() && annotatedParams.isEmpty()) return ret

        val moduleName = options["moduleName"] ?: "Default"
        val packageName = "com.wkq.router.generated"
        val className = "RouteInit_$moduleName"

        // 0. 处理 @Param 自动注入
        val paramGroups = annotatedParams.groupBy { it.parentDeclaration as KSClassDeclaration }
        paramGroups.forEach { (clazz, params) ->
            val targetClassName = clazz.simpleName.asString()
            val syringeClassName = "${targetClassName}_Syringe"
            val targetPackage = clazz.packageName.asString()
            
            val isActivity = clazz.superTypes.any { it.resolve().declaration.qualifiedName?.asString()?.contains("Activity") == true }
            
            val syringeFile = FileSpec.builder(targetPackage, syringeClassName)
                .addType(
                    TypeSpec.classBuilder(syringeClassName)
                        .addSuperinterface(ClassName("com.wkq.router.api", "ISyringe"))
                        .addFunction(
                            FunSpec.builder("inject")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter("target", Any::class)
                                .addStatement("val t = target as %T", ClassName(targetPackage, targetClassName))
                                .addStatement("val extras = when {")
                                .apply {
                                    if (isActivity) {
                                        addStatement("    t is android.app.Activity -> t.intent?.extras")
                                    }
                                    addStatement("    t is androidx.fragment.app.Fragment -> t.arguments")
                                    addStatement("    else -> null")
                                }
                                .addStatement("} ?: return")
                                .apply {
                                    params.forEach { param ->
                                        val paramName = param.simpleName.asString()
                                        val annotation = param.annotations.find { 
                                            it.annotationType.resolve().declaration.qualifiedName?.asString() == "com.wkq.router.annotation.Param" 
                                        }
                                        val key = annotation?.arguments?.find { it.name?.asString() == "name" }?.value as? String
                                        val finalKey = if (key.isNullOrEmpty()) paramName else key
                                        
                                        val type = param.type.resolve()
                                        val typeName = type.declaration.qualifiedName?.asString()
                                        
                                        val parcelableType = resolver.getClassDeclarationByName(resolver.getKSNameFromString("android.os.Parcelable"))?.asType(emptyList())
                                        val serializableType = resolver.getClassDeclarationByName(resolver.getKSNameFromString("java.io.Serializable"))?.asType(emptyList())

                                        when (typeName) {
                                            "kotlin.String" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getString(%S) ?: t.%L", finalKey, paramName, finalKey, paramName)
                                            "kotlin.Int" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getInt(%S)", finalKey, paramName, finalKey)
                                            "kotlin.Boolean" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getBoolean(%S)", finalKey, paramName, finalKey)
                                            "kotlin.Long" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getLong(%S)", finalKey, paramName, finalKey)
                                            "kotlin.Float" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getFloat(%S)", finalKey, paramName, finalKey)
                                            "kotlin.Double" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getDouble(%S)", finalKey, paramName, finalKey)
                                            "kotlin.Byte" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getByte(%S)", finalKey, paramName, finalKey)
                                            "kotlin.Short" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getShort(%S)", finalKey, paramName, finalKey)
                                            "kotlin.Char" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getChar(%S)", finalKey, paramName, finalKey)
                                            
                                            "kotlin.IntArray" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getIntArray(%S)", finalKey, paramName, finalKey)
                                            "kotlin.BooleanArray" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getBooleanArray(%S)", finalKey, paramName, finalKey)
                                            "kotlin.LongArray" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getLongArray(%S)", finalKey, paramName, finalKey)
                                            "kotlin.FloatArray" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getFloatArray(%S)", finalKey, paramName, finalKey)
                                            "kotlin.DoubleArray" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getDoubleArray(%S)", finalKey, paramName, finalKey)
                                            "kotlin.ByteArray" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getByteArray(%S)", finalKey, paramName, finalKey)
                                            "kotlin.ShortArray" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getShortArray(%S)", finalKey, paramName, finalKey)
                                            "kotlin.CharArray" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getCharArray(%S)", finalKey, paramName, finalKey)

                                            "android.os.Bundle" -> addStatement("if (extras.containsKey(%S)) t.%L = extras.getBundle(%S)", finalKey, paramName, finalKey)

                                            else -> {
                                                if (parcelableType != null && parcelableType.isAssignableFrom(type)) {
                                                    addStatement("if (extras.containsKey(%S)) t.%L = extras.getParcelable(%S)", finalKey, paramName, finalKey)
                                                } else if (serializableType != null && serializableType.isAssignableFrom(type)) {
                                                    addStatement("if (extras.containsKey(%S)) t.%L = extras.getSerializable(%S) as? %T", finalKey, paramName, finalKey, ClassName.bestGuess(typeName ?: "java.lang.Object"))
                                                } else {
                                                    logger.warn("Unsupported @Param type: $typeName for $paramName in $targetClassName")
                                                }
                                            }
                                        }
                                    }
                                }
                                .build()
                        )
                        .build()
                )
                .build()
            syringeFile.writeTo(codeGenerator, Dependencies(true, *resolver.getAllFiles().toList().toTypedArray()))
        }

        // 分组处理 Route
        val routeGroups = annotatedRoutes.groupBy { clazz ->
            val annotation = clazz.annotations.find { 
                it.annotationType.resolve().declaration.qualifiedName?.asString() == routeName 
            }
            val path = annotation?.arguments?.find { it.name?.asString() == "path" }?.value as? String ?: ""
            val group = if (path.startsWith("/")) {
                path.substring(1).substringBefore("/")
            } else {
                path.substringBefore("/")
            }
            if (group.isEmpty()) "default" else group
        }

        // 1. 生成每个组的注册类 (RouteGroup)
        routeGroups.forEach { (groupName, classes) ->
            val groupClassName = "RouteGroup_${moduleName}_$groupName"
            val groupFileSpec = FileSpec.builder(packageName, groupClassName)
                .addType(
                    TypeSpec.classBuilder(groupClassName)
                        .addSuperinterface(ClassName("com.wkq.router.api", "IRouteGroup"))
                        .addFunction(
                            FunSpec.builder("load")
                                .addModifiers(KModifier.OVERRIDE)
                                .apply {
                                    classes.forEach { clazz ->
                                        val annotation = clazz.annotations.find { 
                                            it.annotationType.resolve().declaration.qualifiedName?.asString() == routeName 
                                        }
                                        val path = annotation?.arguments?.find { it.name?.asString() == "path" }?.value as? String
                                        if (path != null) {
                                            val qualifiedName = clazz.qualifiedName?.asString() ?: ""
                                            addStatement(
                                                "com.wkq.router.api.RouteTable.register(%S, %T::class.java)",
                                                path, 
                                                ClassName.bestGuess(qualifiedName)
                                            )
                                        }
                                    }
                                }
                                .build()
                        )
                        .build()
                )
                .build()
            groupFileSpec.writeTo(codeGenerator, Dependencies(true, *resolver.getAllFiles().toList().toTypedArray()))
        }

        // 2. 生成主初始化类 (只注册 Group、Service、Interceptor)
        val fileSpec = FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addSuperinterface(ClassName("com.wkq.router.api", "IRouteInit"))
                    .addFunction(
                        FunSpec.builder("init")
                            .addModifiers(KModifier.OVERRIDE)
                            .apply {
                                // 注册路由组
                                routeGroups.keys.forEach { groupName ->
                                    addStatement(
                                        "com.wkq.router.api.RouteTable.registerGroup(%S, %T::class.java)",
                                        groupName,
                                        ClassName(packageName, "RouteGroup_${moduleName}_$groupName")
                                    )
                                }
                                
                                // 处理 @ProvideService
                                annotatedServices.forEach { clazz ->
                                    val annotation = clazz.annotations.find { 
                                        it.annotationType.resolve().declaration.qualifiedName?.asString() == serviceName 
                                    }
                                    val apiType = annotation?.arguments?.find { it.name?.asString() == "api" }?.value as? KSType
                                    val apiClassName = apiType?.declaration?.qualifiedName?.asString()
                                    
                                    if (apiClassName != null) {
                                        val implClassName = clazz.qualifiedName?.asString() ?: ""
                                        addStatement(
                                            "com.wkq.router.api.RouteTable.registerService(%T::class.java, %T())",
                                            ClassName.bestGuess(apiClassName),
                                            ClassName.bestGuess(implClassName)
                                        )
                                    }
                                }

                                // 处理 @Interceptor
                                annotatedInterceptors.forEach { clazz ->
                                    val annotation = clazz.annotations.find { 
                                        it.annotationType.resolve().declaration.qualifiedName?.asString() == interceptorName 
                                    }
                                    val priority = annotation?.arguments?.find { it.name?.asString() == "priority" }?.value as? Int ?: 0
                                    val implClassName = clazz.qualifiedName?.asString() ?: ""
                                    addStatement(
                                        "com.wkq.router.api.RouteTable.registerInterceptor(%L, %T())",
                                        priority,
                                        ClassName.bestGuess(implClassName)
                                    )
                                }
                            }
                            .build()
                    )
                    .build()
            )
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(true, *resolver.getAllFiles().toList().toTypedArray()))
        generateServiceFile(packageName, className)
        
        invoked = true
        return ret
    }

    private fun generateServiceFile(packageName: String, className: String) {
        val path = "META-INF/services/com.wkq.router.api.IRouteInit"
        try {
            codeGenerator.createNewFile(
                Dependencies(true),
                "",
                path,
                ""
            ).use { output ->
                OutputStreamWriter(output, StandardCharsets.UTF_8).use { writer ->
                    writer.write("$packageName.$className")
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to generate service file: ${e.message}")
        }
    }
}
