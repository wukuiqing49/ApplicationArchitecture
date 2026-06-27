package com.wkq.router.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
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
        val paramName = "com.wkq.router.annotation.Param"

        val routeSymbols = resolver.getSymbolsWithAnnotation(routeName)
        val serviceSymbols = resolver.getSymbolsWithAnnotation(serviceName)
        val interceptorSymbols = resolver.getSymbolsWithAnnotation(interceptorName)
        val paramSymbols = resolver.getSymbolsWithAnnotation(paramName)

        val ret = (routeSymbols + serviceSymbols + interceptorSymbols + paramSymbols)
            .filter { !it.validate() }
            .toList()

        val annotatedRoutes = routeSymbols.filterIsInstance<KSClassDeclaration>().toList()
        val annotatedServices = serviceSymbols.filterIsInstance<KSClassDeclaration>().toList()
        val annotatedInterceptors = interceptorSymbols.filterIsInstance<KSClassDeclaration>().toList()
        val annotatedParams = paramSymbols.filterIsInstance<KSPropertyDeclaration>().toList()

        if (
            annotatedRoutes.isEmpty() &&
            annotatedServices.isEmpty() &&
            annotatedInterceptors.isEmpty() &&
            annotatedParams.isEmpty()
        ) {
            return ret
        }

        val moduleName = options["moduleName"]
        if (moduleName.isNullOrBlank() || moduleName == "Default") {
            logger.error(
                "Router moduleName cannot be empty. Please configure " +
                    "ksp { arg(\"moduleName\", \"YourModuleName\") } in the route module."
            )
            return ret
        }

        validateRoutes(annotatedRoutes, routeName)

        val packageName = "com.wkq.router.generated"
        val className = "RouteInit_$moduleName"

        generateSyringeFiles(resolver, annotatedParams)

        val routeGroups = groupRoutes(annotatedRoutes, routeName)
        generateRouteGroupFiles(resolver, packageName, moduleName, routeName, routeGroups)
        generateRouteInitFile(
            resolver = resolver,
            packageName = packageName,
            className = className,
            moduleName = moduleName,
            routeGroups = routeGroups,
            serviceName = serviceName,
            annotatedServices = annotatedServices,
            interceptorName = interceptorName,
            annotatedInterceptors = annotatedInterceptors
        )
        generateServiceFile(packageName, className)

        invoked = true
        return ret
    }

    private fun generateSyringeFiles(
        resolver: Resolver,
        annotatedParams: List<KSPropertyDeclaration>
    ) {
        val paramGroups = annotatedParams.groupBy { it.parentDeclaration as KSClassDeclaration }
        paramGroups.forEach { (clazz, params) ->
            val targetClassName = clazz.simpleName.asString()
            val syringeClassName = "${targetClassName}_Syringe"
            val targetPackage = clazz.packageName.asString()
            val isActivity = clazz.superTypes.any {
                it.resolve().declaration.qualifiedName?.asString()?.contains("Activity") == true
            }
            val isFragment = clazz.superTypes.any {
                it.resolve().declaration.qualifiedName?.asString()?.contains("Fragment") == true
            }

            val syringeFile = FileSpec.builder(targetPackage, syringeClassName)
                .addType(
                    TypeSpec.classBuilder(syringeClassName)
                        .addSuperinterface(ClassName("com.wkq.router.api", "ISyringe"))
                        .addFunction(
                            FunSpec.builder("inject")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter("target", Any::class)
                                .addStatement("val t = target as %T", ClassName(targetPackage, targetClassName))
                                .apply {
                                    when {
                                        isActivity -> addStatement("val extras = t.intent?.extras ?: return")
                                        isFragment -> addStatement("val extras = t.arguments ?: return")
                                        else -> addStatement("val extras = android.os.Bundle()")
                                    }
                                }
                                .apply {
                                    addParamStatements(resolver, params, targetClassName)
                                }
                                .build()
                        )
                        .build()
                )
                .build()
            syringeFile.writeTo(codeGenerator, Dependencies(true, *resolver.getAllFiles().toList().toTypedArray()))
        }
    }

    private fun FunSpec.Builder.addParamStatements(
        resolver: Resolver,
        params: List<KSPropertyDeclaration>,
        targetClassName: String
    ) {
        params.forEach { param ->
            val paramName = param.simpleName.asString()
            val annotation = param.annotations.find {
                it.annotationType.resolve().declaration.qualifiedName?.asString() ==
                    "com.wkq.router.annotation.Param"
            }
            val key = annotation?.arguments?.find { it.name?.asString() == "name" }?.value as? String
            val finalKey = if (key.isNullOrEmpty()) paramName else key
            val type = param.type.resolve()
            val typeName = type.declaration.qualifiedName?.asString()
            val parcelableType = resolver
                .getClassDeclarationByName(resolver.getKSNameFromString("android.os.Parcelable"))
                ?.asType(emptyList())
            val serializableType = resolver
                .getClassDeclarationByName(resolver.getKSNameFromString("java.io.Serializable"))
                ?.asType(emptyList())

            when (typeName) {
                "kotlin.String" -> addStatement(
                    "if (extras.containsKey(%S)) t.%L = extras.getString(%S) ?: t.%L",
                    finalKey,
                    paramName,
                    finalKey,
                    paramName
                )

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
                        addStatement(
                            "if (extras.containsKey(%S)) t.%L = extras.getSerializable(%S) as? %T",
                            finalKey,
                            paramName,
                            finalKey,
                            ClassName.bestGuess(typeName ?: "java.lang.Object")
                        )
                    } else {
                        logger.warn("Unsupported @Param type: $typeName for $paramName in $targetClassName")
                    }
                }
            }
        }
    }

    private fun groupRoutes(
        annotatedRoutes: List<KSClassDeclaration>,
        routeName: String
    ): Map<String, List<KSClassDeclaration>> {
        return annotatedRoutes.groupBy { clazz ->
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
    }

    private fun generateRouteGroupFiles(
        resolver: Resolver,
        packageName: String,
        moduleName: String,
        routeName: String,
        routeGroups: Map<String, List<KSClassDeclaration>>
    ) {
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
    }

    private fun generateRouteInitFile(
        resolver: Resolver,
        packageName: String,
        className: String,
        moduleName: String,
        routeGroups: Map<String, List<KSClassDeclaration>>,
        serviceName: String,
        annotatedServices: List<KSClassDeclaration>,
        interceptorName: String,
        annotatedInterceptors: List<KSClassDeclaration>
    ) {
        val fileSpec = FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addSuperinterface(ClassName("com.wkq.router.api", "IRouteInit"))
                    .addFunction(
                        FunSpec.builder("init")
                            .addModifiers(KModifier.OVERRIDE)
                            .apply {
                                routeGroups.keys.forEach { groupName ->
                                    addStatement(
                                        "com.wkq.router.api.RouteTable.registerGroup(%S, %T::class.java)",
                                        groupName,
                                        ClassName(packageName, "RouteGroup_${moduleName}_$groupName")
                                    )
                                }

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
    }

    private fun validateRoutes(routes: List<KSClassDeclaration>, routeName: String) {
        val seenPaths = mutableMapOf<String, KSClassDeclaration>()
        routes.forEach { clazz ->
            val annotation = clazz.annotations.find {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == routeName
            }
            val path = annotation?.arguments?.find { it.name?.asString() == "path" }?.value as? String
            if (path.isNullOrBlank()) {
                logger.error("@Route path cannot be empty.", clazz)
                return@forEach
            }
            if (!path.matches(Regex("^/[A-Za-z0-9_]+/[A-Za-z0-9_./-]+$"))) {
                logger.error("@Route path must match /group/page format, current path: $path", clazz)
            }
            val existed = seenPaths[path]
            if (existed != null) {
                logger.error(
                    "Duplicate route path found: $path, already used by ${existed.qualifiedName?.asString()}",
                    clazz
                )
            } else {
                seenPaths[path] = clazz
            }
        }
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
