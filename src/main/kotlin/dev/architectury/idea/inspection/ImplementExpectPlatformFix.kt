package dev.architectury.idea.inspection

import com.intellij.codeInsight.generation.GenerateMembersUtil
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.JavaProjectRootsUtil
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.pom.Navigatable
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import dev.architectury.idea.util.AnnotationType
import dev.architectury.idea.util.ArchitecturyBundle
import dev.architectury.idea.util.Platform
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.idea.base.util.module

class ImplementExpectPlatformFix(private val platforms: List<Platform>) : LocalQuickFix {
    override fun getFamilyName(): String {
        val platform = platforms.singleOrNull()

        if (platform != null) return ArchitecturyBundle["inspection.implementExpectPlatform.single", platform]
        return ArchitecturyBundle["inspection.implementExpectPlatform"]
    }
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val method = findMethod(descriptor.psiElement) ?: error("Could not find method from ${descriptor.psiElement}")
        val facade = JavaPsiFacade.getInstance(project)
        val missingPackages = HashMap<Platform, String>()

        val implClassName = Platform.getImplementationName(method.containingClass!!)
        // Use findClasses to get all classes with this FQN across all modules (Fabric, NeoForge, etc.)
        val candidateClasses = facade.findClasses(implClassName, GlobalSearchScope.projectScope(project))

        for (platform in platforms) {
            // Find the class from the candidates that physically lives in this platform's folder
            val implClass = candidateClasses.firstOrNull {
                platform.hasClass(it)
            } ?: run {
                val packageName = implClassName.substringBeforeLast('.')
                val pkg = facade.findPackage(packageName)

                if (pkg == null) {
                    missingPackages[platform] = packageName
                    return@run null
                }

                // Filter directories to find the one belonging to this platform
                val platformDirs = pkg.directories.filter {
                    it.virtualFile.path.contains("/${platform.id}/", ignoreCase = true)
                }

                val dir = if (platformDirs.isNotEmpty()) {
                    findJavaSourceDirectory(platformDirs.toTypedArray())
                } else {
                    // Fallback to searching modules if the package folder doesn't exist yet
                    null
                }

                if (dir != null) {
                    val shortName = implClassName.substringAfterLast('.')
                    JavaDirectoryService.getInstance().getClasses(dir)
                        .firstOrNull { it.name == shortName }
                        ?: JavaDirectoryService.getInstance().createClass(dir, shortName)
                } else {
                    missingPackages[platform] = packageName
                    null
                }
            } ?: continue

            addMethod(project, method, implClass)
        }

        // Handle platforms where the class/package was completely missing
        if (missingPackages.isNotEmpty()) {
            // Step out of the write action for UI dialogs
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                missingPackages.forEach { (platform, packageName) ->
                    var directory: PsiDirectory? = null
                    val modules = ModuleManager.getInstance(project).modules.asSequence()
                        .distinctBy { it.name }
                        .toList()

                    fun handleModule(module: Module, test: Boolean): Boolean {
                        // Matching module name to platform (e.g. polytone.neoforge.main)
                        if (module.name.contains(platform.id, ignoreCase = true) && !module.name.contains("common")) {
                            val dir = ModuleRootManager.getInstance(module).contentEntries.asSequence()
                                .flatMap { it.getSourceFolders(if (test) JavaSourceRootType.TEST_SOURCE else JavaSourceRootType.SOURCE) }
                                .filter { !JavaProjectRootsUtil.isForGeneratedSources(it) }
                                .mapNotNull { it.file }
                                .firstOrNull { it.path.contains("/main/") }
                            if (dir != null) {
                                directory = PsiManager.getInstance(project).findDirectory(dir)
                                return true
                            }
                        }
                        return false
                    }

                    for (module in modules) { if (handleModule(module, false)) break }
                    if (directory == null) {
                        for (module in modules) { if (handleModule(module, true)) break }
                    }

                    ImplementExpectPlatformFixDialog(project, platform, packageName, method, directory).show()
                }
            }
        }
    }

    companion object {
        fun addMethod(project: Project, method: PsiMethod, clazz: PsiClass) {
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            val template = elementFactory.createMethod(method.name, method.returnType)

            // Copy the parameters to the template
            for (param in method.parameterList.parameters) {
                template.parameterList.add(param)
            }

            // Add the different modifiers. The public modifier is first removed to correct its place.
            template.modifierList.setModifierProperty(PsiModifier.PUBLIC, false) // remove from list...
            GenerateMembersUtil.copyAnnotations(method, template, *AnnotationType.EXPECT_PLATFORM.toTypedArray())
            template.modifierList.setModifierProperty(PsiModifier.PUBLIC, true) // ... add to list
            template.modifierList.setModifierProperty(PsiModifier.STATIC, true)

            val inserted = GenerateMembersUtil.insert(clazz, template, clazz.methods.lastOrNull(), false)
            (inserted as? Navigatable)?.navigate(true)
        }
    }

    private tailrec fun findMethod(element: PsiElement): PsiMethod? =
        when {
            element is PsiMethod -> element
            element.parent != null -> findMethod(element.parent)
            else -> null
        }

    /**
     * Finds the most Java-looking source directory in [directories].
     *
     * - `src/main/java` is matched first
     * - any `java` is matched second
     * - if none of the above, the first element of the array
     */
    private fun findJavaSourceDirectory(directories: Array<out PsiDirectory>): PsiDirectory {
        if (directories.size == 1) return directories.single()

        val srcMainJava = directories.firstOrNull {
            val path = it.virtualFile.toNioPath().toAbsolutePath()
            "src/main/java" in path.toString().replace(path.fileSystem.separator, "/")
        }

        if (srcMainJava != null) return srcMainJava

        val anyJava = directories.firstOrNull {
            "java" in it.virtualFile.toNioPath().toAbsolutePath().toString()
        }

        if (anyJava != null) return anyJava

        return directories.first()
    }
}
