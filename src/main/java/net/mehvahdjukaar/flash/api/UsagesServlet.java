package net.mehvahdjukaar.flash.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.intellij.openapi.project.Project;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.mehvahdjukaar.flash.FlashPlugin;
import net.mehvahdjukaar.flash.IdeaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.mehvahdjukaar.flash.api.ServletUtils.*;

@Path("/usages")
public class UsagesServlet {


    @GET
    @Path("class")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClassUsages(@QueryParam("class") String className,
                                   @QueryParam("project") String projectName,
                                   @QueryParam("includeDependencies") Boolean includeDependencies) {
        FlashPlugin.LOGGER.debug("Class usages endpoint called - class: " + className + ", project: " + projectName + ", includeDependencies: " + includeDependencies);
        if (className == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing 'class' parameter").build();
        }

        List<Project> projectsToCheck = new ArrayList<>();
        var resp = getProjectsToCheck(projectsToCheck, projectName);
        if (resp != null) return resp;

        List<String> resolvedFQNs = new ArrayList<>();
        resp = resolveClassName(className, projectsToCheck, resolvedFQNs);
        if (resp != null) return resp;

        String fqn = resolvedFQNs.get(0);
        List<Map<String, Object>> allUsages = new ArrayList<>();
        boolean onlyProject = includeDependencies == null || !includeDependencies;
        for (Project project : projectsToCheck) {
            List<Map<String, Object>> usages = IdeaUtils.read(() ->
                IdeaUtils.getClassUsages(project, fqn, onlyProject));
            allUsages.addAll(usages);
        }
        try {
            String json = MAPPER.writeValueAsString(allUsages);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing response").build();
        }
    }

    @GET
    @Path("field")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFieldUsages(@QueryParam("class") String className,
                                   @QueryParam("field") String fieldName,
                                   @QueryParam("project") String projectName,
                                   @QueryParam("includeDependencies") Boolean includeDependencies) {
        FlashPlugin.LOGGER.debug("Field usages endpoint called - class: " + className + ", field: " + fieldName + ", project: " + projectName + ", includeDependencies: " + includeDependencies);
        if (className == null || fieldName == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing 'class' or 'field' parameter").build();
        }

        List<Project> projectsToCheck = new ArrayList<>();
        var resp = getProjectsToCheck(projectsToCheck, projectName);
        if (resp != null) return resp;

        List<String> resolvedFQNs = new ArrayList<>();
        resp = resolveClassName(className, projectsToCheck, resolvedFQNs);
        if (resp != null) return resp;

        String fqn = resolvedFQNs.get(0);
        List<Map<String, Object>> allUsages = new ArrayList<>();
        boolean onlyProject = includeDependencies == null || !includeDependencies;
        for (Project project : projectsToCheck) {
            List<Map<String, Object>> usages = IdeaUtils.read(() ->
                IdeaUtils.getFieldUsages(project, fqn, fieldName, onlyProject));
            allUsages.addAll(usages);
        }
        try {
            String json = MAPPER.writeValueAsString(allUsages);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing response").build();
        }
    }

    @GET
    @Path("method")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCallers(@QueryParam("class") String className,
                               @QueryParam("method") String methodName,
                               @QueryParam("project") String projectName,
                               @QueryParam("includeDependencies") Boolean includeDependencies) {
        FlashPlugin.LOGGER.debug("Callers endpoint called - class: " + className + ", method: " + methodName + ", project: " + projectName + ", includeDependencies: " + includeDependencies);
        if (className == null || methodName == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing 'class' or 'method' parameter").build();
        }

        List<Project> projectsToCheck = new ArrayList<>();
        var resp = getProjectsToCheck(projectsToCheck, projectName);
        if (resp != null) return resp;

        List<String> resolvedFQNs = new ArrayList<>();
        resp = resolveClassName(className, projectsToCheck, resolvedFQNs);
        if (resp != null) return resp;

        String fqn = resolvedFQNs.get(0);
        List<Map<String, Object>> allCallers = new ArrayList<>();
        boolean onlyProject = includeDependencies == null || !includeDependencies;
        for (Project project : projectsToCheck) {
            List<Map<String, Object>> callers = IdeaUtils.read(() ->
                IdeaUtils.getMethodCallers(project, fqn, methodName, onlyProject));
            allCallers.addAll(callers);
        }
        try {
            String json = MAPPER.writeValueAsString(allCallers);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing response").build();
        }
    }


}
