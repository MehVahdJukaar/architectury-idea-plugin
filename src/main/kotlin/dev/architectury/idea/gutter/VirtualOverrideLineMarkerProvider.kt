package dev.architectury.idea.gutter

import com.intellij.psi.PsiMethod

class VirtualOverrideLineMarkerProvider : AbstractVirtualOverrideLineMarkerProvider<PsiMethod>() {
    override val converter = PsiMethodConverter.JAVA
}
