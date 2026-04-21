package net.mehvahdjukaar.flash.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.mehvahdjukaar.flash.FlashPlugin;
import net.mehvahdjukaar.flash.IdeaUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServletUtils {
    static final ObjectMapper MAPPER = new ObjectMapper();




    static @Nullable Response resolveClassName(String className, List<Project> projectsToCheck, List<String> resolvedFQNs) {
        resolvedFQNs.clear();
        List<String> allFQNs = new ArrayList<>();
        for (Project project : projectsToCheck) {
            List<String> fqns = IdeaUtils.read(() ->
                IdeaUtils.findClassFQNs(project, className));
            allFQNs.addAll(fqns);
        }
        if (allFQNs.isEmpty()) {
            FlashPlugin.LOGGER.debug("No classes found for: " + className);
            return Response.status(Response.Status.NOT_FOUND).entity("Class not found").build();
        } else if (allFQNs.size() == 1) {
            resolvedFQNs.add(allFQNs.get(0));
            return null;
        } else {
            // Multiple, return JSON list
            try {
                String json = MAPPER.writeValueAsString(allFQNs);
                FlashPlugin.LOGGER.debug("Multiple classes found for: " + className + ", returning list");
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing response").build();
            }
        }
    }

    static @Nullable Response getProjectsToCheck(List<Project> projects, @Nullable String projectName) {
        if (projectName != null) {
            Project project = IdeaUtils.findProjectByName(projectName);
            if (project == null) {
                FlashPlugin.LOGGER.debug("Project not found: " + projectName);
                return Response.status(Response.Status.BAD_REQUEST).entity("Project not found").build();
            }
            projects.add(project);
        } else {
            ProjectManager pm = ApplicationManager.getApplication().getService(ProjectManager.class);
            if (pm == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("ProjectManager not available").build();
            }
            Project[] openProjects = IdeaUtils.read(pm::getOpenProjects);
            projects.addAll(List.of(openProjects));
            FlashPlugin.LOGGER.debug("Checking all projects: " + Arrays.toString(openProjects));
        }
        return null;
    }

}
