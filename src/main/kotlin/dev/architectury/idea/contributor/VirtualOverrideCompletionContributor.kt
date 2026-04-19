package dev.architectury.idea.contributor
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import dev.architectury.idea.insight.findAllPlatformVirtualOverridableMethods
import dev.architectury.idea.util.isCommon

class VirtualOverrideCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(PsiIdentifier::class.java)
            .inside(PsiMethod::class.java)
            .inside(PsiClass::class.java)
            .inFile(PlatformPatterns.psiFile(PsiFile::class.java)),
            PlatformVirtualOverrideCompletionProvider()
        )
    }
}

class PlatformVirtualOverrideCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val psiFile = position.containingFile
        val project = position.project

        val module = ModuleUtil.findModuleForPsiElement(psiFile) ?: return
        if (!module.isCommon) return

        val containingClass = PsiTreeUtil.getParentOfType(position, PsiClass::class.java) ?: return

        val overridableMethods = containingClass.findAllPlatformVirtualOverridableMethods()

        val existingSignatures = containingClass.methods.map { method ->
            method.name to method.parameterList.parameters.map { it.type.canonicalText }
        }.toSet()

        val filteredMethods = overridableMethods.filter { platformMethod ->
            val sig = platformMethod.name to platformMethod.parameterList.parameters.map { it.type.canonicalText }
            sig !in existingSignatures
        }

        for (platformMethod in filteredMethods) {
            val lookupElement = createLookupElement(platformMethod, containingClass)
            result.addElement(lookupElement)
        }
    }

    private fun createLookupElement(platformMethod: PsiMethod, targetClass: PsiClass): LookupElement {
        val returnType = platformMethod.returnType?.presentableText ?: "void"
        val params = platformMethod.parameterList.parameters.joinToString(", ") { it.type.presentableText }
        val tailText = " → $returnType ($params)"

        return LookupElementBuilder.create(platformMethod.name)
            .withIcon(PlatformIcons.METHOD_ICON)
            .withTailText(tailText, true)
            .withTypeText(platformMethod.containingClass?.name)
            .withInsertHandler { context, item ->
                val editor = context.editor
                val project = context.project

                // Use the correct API
                val insertedMethods = OverrideImplementUtil.overrideOrImplementMethod(
                    targetClass,
                    platformMethod,
                    false // do not copy javadoc
                )

                insertedMethods.firstOrNull()?.let { method ->
                    val body = method.body ?: return@withInsertHandler
                    val lBrace = body.lBrace ?: return@withInsertHandler
                    editor.caretModel.moveToOffset(lBrace.textOffset + 1)
                }
            }
    }
}
