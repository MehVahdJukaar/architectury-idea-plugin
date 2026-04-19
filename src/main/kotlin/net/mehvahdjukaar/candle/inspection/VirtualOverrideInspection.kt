package net.mehvahdjukaar.candle.inspection
import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import net.mehvahdjukaar.candle.util.AnnotationType
import net.mehvahdjukaar.candle.util.isValidVirtualOverrideForPlatform

class VirtualOverrideInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                // Find the @VirtualOverride annotation
                val annotation = method.annotations.firstOrNull { ann ->
                    AnnotationType.VIRTUAL_OVERRIDE.any { ann.hasQualifiedName(it) }
                } ?: return

                // Extract platform value
                val platformValue = annotation.findAttributeValue("value") as? PsiLiteralExpression
                val platformId = platformValue?.value as? String

                if (platformId.isNullOrEmpty()) {
                    // Missing platform value – highlight the annotation
                    holder.registerProblem(
                        annotation,
                        "@VirtualOverride must specify a platform",
                        ProblemHighlightType.ERROR
                    )
                    return
                }

                // Validate override
                if (!method.isValidVirtualOverrideForPlatform(platformId)) {
                    // Invalid override – highlight the annotation, not the method
                    holder.registerProblem(
                        annotation,
                        "Method does not override any method from platform '$platformId'",
                        ProblemHighlightType.ERROR
                    )
                }
            }
        }
    }
}

class RemoveVirtualOverrideFix : LocalQuickFix {
    override fun getFamilyName(): String = "Remove @VirtualOverride annotation"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val annotation = descriptor.psiElement as? PsiAnnotation ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            annotation.delete()
        }
    }
}
