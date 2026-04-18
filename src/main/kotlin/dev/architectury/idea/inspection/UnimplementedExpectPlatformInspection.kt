package dev.architectury.idea.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import dev.architectury.idea.util.AnnotationType
import dev.architectury.idea.util.ArchitecturyBundle
import dev.architectury.idea.util.Platform
import dev.architectury.idea.util.findAnnotation
import dev.architectury.idea.util.isCommonExpectPlatform

class UnimplementedExpectPlatformInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                // 1. Only inspect @ExpectPlatform methods in common
                if (method.isCommonExpectPlatform) {
                    val containingClass = method.containingClass ?: return
                    val facade = JavaPsiFacade.getInstance(method.project)
                    val implClassName = Platform.getImplementationName(containingClass)

                    // 2. Get every class in the project with the implementation FQN
                    val candidateClasses =
                        facade.findClasses(implClassName, GlobalSearchScope.projectScope(method.project))

                    // 3. Filter for platforms that are missing a valid implementation
                    val missingPlatforms = Platform.values().filter { platform ->
                        // Skip platforms not present in this project
                        if (!platform.isIn(method.project)) return@filter false

                        // Find the implementation class that physically belongs to this platform's module
                        val implClass = candidateClasses.firstOrNull { clazz ->
                            platform.hasClass(clazz)
                        }

                        // If class is missing, or class exists but doesn't have the method signature
                        if (implClass == null) {
                            true
                        } else {
                            // Check if the specific method signature exists in that class
                            val hasMethod = implClass.findMethodsByName(method.name, false).any { implMethod ->
                                // Match parameter count (or full signature if you prefer)
                                implMethod.parameterList.parametersCount == method.parameterList.parametersCount
                            }
                            !hasMethod
                        }
                    }

                    // 4. If any platform is missing its method, register the error
                    if (missingPlatforms.isNotEmpty()) {
                        val fixes = missingPlatforms.mapTo(ArrayList()) { ImplementExpectPlatformFix(listOf(it)) }

                        // Add the "Fix all" option if multiple platforms are missing
                        if (fixes.size > 1) {
                            fixes.add(0, ImplementExpectPlatformFix(missingPlatforms))
                        }

                        holder.registerProblem(
                            method.findAnnotation(AnnotationType.EXPECT_PLATFORM) ?: method.nameIdentifier ?: method,
                            ArchitecturyBundle["inspection.missingExpectPlatform", method.name, missingPlatforms.joinToString()],
                            *fixes.toTypedArray()
                        )
                    }
                }
            }
        }
}
