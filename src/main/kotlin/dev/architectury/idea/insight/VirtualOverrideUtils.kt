// VirtualOverrideUtils.kt
package dev.architectury.idea.insight

import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.InheritanceUtil
import dev.architectury.idea.util.Platform
import dev.architectury.idea.util.signatureKey

/**
 * Returns all methods in platform modules that this common method virtually overrides.
 * This includes methods from modified superclasses or added interfaces in platform‑specific
 * versions of the common class's ancestors.
 */
fun PsiMethod.findPlatformVirtualOverrides(): Set<PsiMethod> {
    val commonClass = containingClass ?: return emptySet()
    val project = project

    return CachedValuesManager.getCachedValue(this) {
        val result = mutableSetOf<PsiMethod>()
        val dependencies = mutableSetOf<PsiElement>(this, commonClass)

        val availablePlatforms = Platform.availables(project)
        for (platform in availablePlatforms) {
            val platformModule = platform.findModuleForPlatform(project) ?: continue

            // Collect all supertypes (classes and interfaces) of the common class, excluding the class itself
            val commonSuperTypes = collectAllSuperTypes(commonClass, mutableSetOf())
                .filter { it.qualifiedName != CommonClassNames.JAVA_LANG_OBJECT && it != commonClass }

            for (superType in commonSuperTypes) {
                val qualifiedName = superType.qualifiedName ?: continue

                // Find the platform version of this supertype
                val platformSuperType = JavaPsiFacade.getInstance(project)
                    .findClass(qualifiedName, GlobalSearchScope.moduleRuntimeScope(platformModule, false))
                    ?: continue

                dependencies.add(platformSuperType)

                // Examine the entire hierarchy of the platform supertype for matching methods
                val platformHierarchy : Set<PsiClass> = collectAllSuperTypes(platformSuperType, dependencies)

                for (platformClass in platformHierarchy) {
                    platformClass.findMethodsByName(name, false).forEach { platformMethod ->
                        if (signaturesMatch(platformMethod)) {
                            result.add(platformMethod)
                            dependencies.add(platformMethod)
                        }
                    }
                }
            }
        }

        CachedValueProvider.Result(result, *dependencies.toTypedArray())
    }
}

/**
 * Recursively collects all superclasses and superinterfaces of the given class.
 */
private fun collectAllSuperTypes(psiClass: PsiClass, dependencies: MutableSet<PsiElement>): Set<PsiClass> {
    val result = mutableSetOf<PsiClass>()

    // Superclasses
    val superClasses = mutableSetOf<PsiClass>()
    InheritanceUtil.getSuperClasses(psiClass, superClasses, true)
    result.addAll(superClasses)
    dependencies.addAll(superClasses)

    // Interfaces – may be either PsiClassType (source) or PsiClass (compiled)
    for (ifaceElement in psiClass.interfaces) {
        val iface = when (ifaceElement) {
            is PsiClass -> ifaceElement
            else -> continue
        } ?: continue

        result.add(iface)
        dependencies.add(iface)
        result.addAll(collectAllSuperTypes(iface, dependencies))
    }
    result.add(psiClass)

    return result
}

/**
 * Checks if the given platform method has a signature that matches this common method.
 */
private fun PsiMethod.signaturesMatch(other: PsiMethod): Boolean {
    if (name != other.name) return false
    val myParams = parameterList.parameters
    val otherParams = other.parameterList.parameters
    if (myParams.size != otherParams.size) return false
    return myParams.zip(otherParams).all { (p1, p2) -> p1.type.equals(p2.type) }
}

// VirtualOverrideUtils.kt

data class PlatformVirtualMethod(
    val method: PsiMethod,
    val platform: Platform
)
fun PsiClass.findAllPlatformVirtualOverridableMethods(): List<PlatformVirtualMethod> {
    val project = project

    return CachedValuesManager.getCachedValue(this) {
        val dependencies = mutableSetOf<PsiElement>(this)

        // signature -> platforms + methods
        val index = mutableMapOf<String, MutableList<PlatformVirtualMethod>>()

        val availablePlats = Platform.availables(project)

        for (platform in availablePlats) {
            val platformModule = platform.findModuleForPlatform(project) ?: continue

            val commonSuperTypes = collectAllSuperTypes(this, dependencies)
                .filter { it.qualifiedName != CommonClassNames.JAVA_LANG_OBJECT && it != this }

            for (superType in commonSuperTypes) {
                val qualifiedName = superType.qualifiedName ?: continue

                val platformSuperType = JavaPsiFacade.getInstance(project)
                    .findClass(qualifiedName, GlobalSearchScope.moduleRuntimeScope(platformModule, false))
                    ?: continue

                dependencies.add(platformSuperType)

                val platformHierarchy = collectAllSuperTypes(platformSuperType, dependencies)

                for (platformClass in platformHierarchy) {
                    for (method in platformClass.methods) {
                        if (!isOverridable(method)) continue

                        val key = method.signatureKey()
                        index.getOrPut(key) { mutableListOf() }
                            .add(PlatformVirtualMethod(method, platform))
                    }
                }
            }
        }

        // KEEP ONLY METHODS EXISTING IN EXACTLY ONE PLATFORM
        val result = index.values
            .filter { it.map { pm -> pm.platform }.toSet().size == 1 }
            .map { it.first() }

        CachedValueProvider.Result(result, *dependencies.toTypedArray())
    }
}

private fun isOverridable(method: PsiMethod): Boolean {
    if (method.isConstructor) return false
    if (method.hasModifierProperty(PsiModifier.STATIC)) return false
    if (method.hasModifierProperty(PsiModifier.FINAL)) return false
    if (method.hasModifierProperty(PsiModifier.PRIVATE)) return false
    // Also exclude package-private if class is in a different package?
    // For simplicity, we include all non-private instance methods.
    return true
}
