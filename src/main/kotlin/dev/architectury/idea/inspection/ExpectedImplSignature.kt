package dev.architectury.idea.inspection

import com.intellij.psi.*

data class ExpectedImplSignature(
    val name: String,
    val returnType: PsiType,
    val parameterTypes: List<PsiType>
) {
    companion object {
        fun fromExpectMethod(method: PsiMethod): ExpectedImplSignature {
            val project = method.project
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            val paramTypes = mutableListOf<PsiType>()

            // For instance methods, add the containing class as first parameter
            if (!method.hasModifierProperty(PsiModifier.STATIC)) {
                val containingClass = method.containingClass!!
                val classType = PsiType.getTypeByName(
                    containingClass.qualifiedName!!,
                    project,
                    method.resolveScope
                ) ?: elementFactory.createType(containingClass)
                paramTypes.add(classType)
            }

            // Add original method parameters
            method.parameterList.parameters.mapTo(paramTypes) { it.type }

            return ExpectedImplSignature(
                name = method.name,
                returnType = method.returnType ?: PsiType.VOID,
                parameterTypes = paramTypes
            )
        }
    }
}
