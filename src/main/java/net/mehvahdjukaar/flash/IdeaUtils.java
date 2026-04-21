package net.mehvahdjukaar.flash;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.util.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class IdeaUtils {

    public static <T> T read(Supplier<T> function) {
        return ApplicationManager.getApplication()
            .runReadAction((ThrowableComputable<T, RuntimeException>) function::get);

    }

    public static Project findProjectByName(String name) {
        return read(() -> {
            ProjectManager pm = ApplicationManager.getApplication().getService(ProjectManager.class);
            if (pm == null) return null;
            Project[] projects = pm.getOpenProjects();
            for (Project p : projects) {
                if (name.equals(p.getName())) {
                    return p;
                }
            }
            return null;
        });
    }

    public static String getOpenProjectNames() {
        return read(() -> {
            ProjectManager pm = ApplicationManager.getApplication().getService(ProjectManager.class);
            if (pm == null) return "ProjectManager not available";
            Project[] projects = pm.getOpenProjects();
            if (projects.length == 0) return "No projects open";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < projects.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(projects[i].getName());
            }
            return sb.toString();
        });
    }

    public static List<String> getSuperclasses(Project project, String fqn) {

        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
        if (psiClass == null) {
            return new ArrayList<>();
        }
        List<String> superclasses = new ArrayList<>();
        PsiClass current = psiClass.getSuperClass();
        int count = 0;
        while (current != null && count < 5) {
            String qn = current.getQualifiedName();
            if (qn != null) {
                superclasses.add(qn);
            }
            current = current.getSuperClass();
            count++;
        }
        return superclasses;
    }

    public static String getContent(Project project, String fqn, String methodName, Integer startLine, Integer endLine) {
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
        if (psiClass == null) {
            return "";
        }
        if (methodName != null) {
            PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
            if (methods.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < methods.length; i++) {
                    if (i > 0) sb.append("\n\n");
                    sb.append(methods[i].getText());
                }
                return sb.toString();
            } else {
                return "";
            }
        } else {
            String text = psiClass.getText();
            if (startLine != null && endLine != null) {
                String[] lines = text.split("\n");
                int start = Math.max(0, startLine - 1);
                int end = Math.min(lines.length, endLine);
                if (start >= end) return "";
                StringBuilder sb = new StringBuilder();
                for (int i = start; i < end; i++) {
                    if (i > start) sb.append("\n");
                    sb.append(lines[i]);
                }
                return sb.toString();
            } else {
                return text;
            }
        }
    }

    public static List<Map<String, Object>> getMethodCallers(Project project, String fqn, String methodName, boolean projectOnly) {
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
        if (psiClass == null) {
            return new ArrayList<>();
        }
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
        List<Map<String, Object>> callers = new ArrayList<>();
        GlobalSearchScope scope = projectOnly ? GlobalSearchScope.projectScope(project) : GlobalSearchScope.allScope(project);
        for (PsiMethod method : methods) {
            Query<PsiReference> query = ReferencesSearch.search(method, scope);
            for (PsiReference ref : query.findAll()) {
                PsiElement element = ref.getElement();
                PsiClass containingClass = findContainingClass(element);
                if (containingClass != null) {
                    String className = containingClass.getQualifiedName();
                    if (className != null) {
                        Document document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());
                        if (document != null) {
                            int line = document.getLineNumber(element.getTextRange().getStartOffset()) + 1;
                            Map<String, Object> caller = new HashMap<>();
                            caller.put("class", className);
                            caller.put("line", line);
                            callers.add(caller);
                        }
                    }
                }
            }
        }
        return callers;
    }

    private static PsiClass findContainingClass(PsiElement element) {
        while (element != null) {
            if (element instanceof PsiClass) {
                return (PsiClass) element;
            }
            element = element.getParent();
        }
        return null;
    }

    public static List<String> findClassFQNs(Project project, String className) {
        List<String> fqns = new ArrayList<>();
        if (className.contains(".")) {
            // Treat as FQN
            PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project));
            if (psiClass != null) {
                fqns.add(className);
            }
        } else {
            // Simple name
            PsiClass[] classes = JavaPsiFacade.getInstance(project).findClasses(className, GlobalSearchScope.allScope(project));
            for (PsiClass cls : classes) {
                String qn = cls.getQualifiedName();
                if (qn != null) {
                    fqns.add(qn);
                }
            }
        }
        return fqns;
    }

    public static String getContainingMethodContent(Project project, String fqn, int lineNumber) {
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
        if (psiClass == null) {
            return "";
        }
        PsiFile psiFile = psiClass.getContainingFile();
        if (psiFile == null) {
            return "";
        }
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            return "";
        }
        int lineCount = document.getLineCount();
        if (lineNumber < 1 || lineNumber > lineCount) {
            return "";
        }
        int offset = document.getLineStartOffset(lineNumber - 1);
        PsiElement element = psiFile.findElementAt(offset);
        if (element == null) {
            return "";
        }
        // Traverse up to find PsiMethod
        PsiElement current = element;
        while (current != null) {
            if (current instanceof PsiMethod) {
                return current.getText();
            }
            current = current.getParent();
        }
        return "";
    }

    public static Map<String, Object> getMethodDeclaration(Project project, String fqn, String methodName) {
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
        if (psiClass == null) {
            return null;
        }
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
        if (methods.length == 0) {
            return null;
        }
        PsiMethod method = methods[0]; // Take the first one
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return null;
        }
        String className = containingClass.getQualifiedName();
        if (className == null) {
            return null;
        }
        PsiFile psiFile = containingClass.getContainingFile();
        if (psiFile == null) {
            return null;
        }
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            return null;
        }
        int line = document.getLineNumber(method.getTextRange().getStartOffset()) + 1;
        Map<String, Object> result = new HashMap<>();
        result.put("class", className);
        result.put("line", line);
        return result;
    }

    public static List<Map<String, Object>> getMethodImplementations(Project project, String fqn, String methodName) {
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
        if (psiClass == null) {
            return new ArrayList<>();
        }
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
        List<Map<String, Object>> implementations = new ArrayList<>();
        for (PsiMethod method : methods) {
            // Use OverridingMethodsSearch to find implementations
            Query<PsiMethod> query = OverridingMethodsSearch.search(method);
            for (PsiMethod impl : query.findAll()) {
                PsiClass containingClass = impl.getContainingClass();
                if (containingClass != null) {
                    String className = containingClass.getQualifiedName();
                    if (className != null) {
                        PsiFile psiFile = containingClass.getContainingFile();
                        if (psiFile != null) {
                            Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                            if (document != null) {
                                int line = document.getLineNumber(impl.getTextRange().getStartOffset()) + 1;
                                Map<String, Object> implMap = new HashMap<>();
                                implMap.put("class", className);
                                implMap.put("line", line);
                                implementations.add(implMap);
                            }
                        }
                    }
                }
            }
        }
        return implementations;
    }
}
