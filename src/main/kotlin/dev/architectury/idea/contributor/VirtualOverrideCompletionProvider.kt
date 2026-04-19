package dev.architectury.idea.contributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import dev.architectury.idea.insight.PlatformVirtualMethod
import dev.architectury.idea.insight.findAllPlatformVirtualOverridableMethods
import dev.architectury.idea.util.getDefaultReturnValue
import dev.architectury.idea.util.isCommon
// VirtualOverrideCompletionProvider.kt

class VirtualOverrideCompletionProvider : CompletionProvider<CompletionParameters>() {
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

        val platformVirtualMethods = containingClass.findAllPlatformVirtualOverridableMethods()

        val existingSignatures = containingClass.methods.map { method ->
            method.name to method.parameterList.parameters.map { it.type.canonicalText }
        }.toSet()

        val filteredMethods = platformVirtualMethods.filter { pvm ->
            val sig = pvm.method.name to pvm.method.parameterList.parameters.map { it.type.canonicalText }
            sig !in existingSignatures
        }

        for (pvm in filteredMethods) {
            val lookupElement = createLookupElement(pvm, containingClass)
            result.addElement(lookupElement)
        }
    }

    private fun createLookupElement(pvm: PlatformVirtualMethod, targetClass: PsiClass): LookupElement {
        val method = pvm.method
        val returnType = method.returnType?.presentableText ?: "void"
        val params = method.parameterList.parameters.joinToString(", ") { it.type.presentableText }
        val tailText = " → $returnType ($params)"
        val platformName = pvm.platform.id

        return LookupElementBuilder.create(method, method.name)
            .withIcon(method.getIcon(0))
            .withTailText(tailText, true)
            .withTypeText("$platformName · ${method.containingClass?.name}")
            .bold()
            .withInsertHandler { context, item ->
                val editor = context.editor
                val project = context.project
                val document = editor.document

                // Build method text with comment and @Override
                val methodText = buildMethodText(pvm)

                // Insert at current caret position
                val startOffset = editor.caretModel.offset
                document.insertString(startOffset, methodText)

                // Move caret inside the method body
                val bodyStart = startOffset + methodText.indexOf("{") + 1
                editor.caretModel.moveToOffset(bodyStart)

                // Optionally format the inserted code
                PsiDocumentManager.getInstance(project).commitDocument(document)
                val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
                if (psiFile != null) {
                    val insertedMethod =
                        PsiTreeUtil.findElementOfClassAtOffset(psiFile, bodyStart, PsiMethod::class.java, false)
                    insertedMethod?.let { CodeStyleManager.getInstance(project).reformat(it) }
                }
            }
    }

    private fun buildMethodText(pvm: PlatformVirtualMethod): String {
        val method = pvm.method
        val returnType = method.returnType?.canonicalText ?: "void"
        val methodName = method.name
        val params = method.parameterList.parameters.joinToString(", ") { param ->
            "${param.type.canonicalText} ${param.name}"
        }
        val exceptions = method.throwsList.referenceElements.joinToString(", ") { it.canonicalText }
        val throwsClause = if (exceptions.isNotEmpty()) " throws $exceptions" else ""

        return buildString {
            append("// From ${pvm.platform.id}\n")
            append("@Override\n")
            append("public $returnType $methodName($params)$throwsClause {\n")
            if (returnType != "void") {
                append("    // TODO: Implement for ${pvm.platform.id}\n")
                append("    return ${getDefaultReturnValue(method.returnType)};\n")
            } else {
                append("    // TODO: Implement for ${pvm.platform.id}\n")
            }
            append("}\n")
        }
    }

}
