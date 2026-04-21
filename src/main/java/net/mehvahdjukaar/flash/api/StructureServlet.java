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

@Path("/structure")
public class StructureServlet {

    @GET
    @Path("declaration")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeclaration(@QueryParam("class") String className,
                                   @QueryParam("method") String methodName,
                                   @QueryParam("project") String projectName) {
        FlashPlugin.LOGGER.debug("Declaration endpoint called - class: " + className + ", method: " + methodName + ", project: " + projectName);
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
        for (Project project : projectsToCheck) {
            Map<String, Object> declaration = IdeaUtils.read(() ->
                IdeaUtils.getMethodDeclaration(project, fqn, methodName));

            if (declaration != null) {
                FlashPlugin.LOGGER.debug("Found declaration in project " + project.getName());
                try {
                    String json = MAPPER.writeValueAsString(declaration);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                } catch (JsonProcessingException e) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing response").build();
                }
            }
        }
        FlashPlugin.LOGGER.debug("No declaration found for class: " + fqn + ", method: " + methodName);
        return Response.status(Response.Status.NOT_FOUND).entity("Declaration not found").build();
    }

    @GET
    @Path("implementations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImplementations(@QueryParam("class") String className,
                                       @QueryParam("method") String methodName,
                                       @QueryParam("project") String projectName) {
        FlashPlugin.LOGGER.debug("Implementations endpoint called - class: " + className + ", method: " + methodName + ", project: " + projectName);
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
        List<Map<String, Object>> allImplementations = new ArrayList<>();
        for (Project project : projectsToCheck) {
            List<Map<String, Object>> implementations = IdeaUtils.read(() ->
                IdeaUtils.getMethodImplementations(project, fqn, methodName));
            allImplementations.addAll(implementations);
        }
        try {
            String json = MAPPER.writeValueAsString(allImplementations);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing response").build();
        }
    }

    @GET
    @Path("class-api")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClassApi(@QueryParam("class") String className,
                                @QueryParam("project") String projectName,
                                @QueryParam("visibility") String visibility,
                                @QueryParam("inside") Boolean inside,
                                @QueryParam("deep") Boolean deep) {
        FlashPlugin.LOGGER.debug("Class API endpoint called - class: " + className + ", project: " + projectName + ", visibility: " + visibility + ", inside: " + inside + ", deep: " + deep);
        if (className == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing 'class' parameter").build();
        }

        // Default values
        final String vis = visibility == null ? "public" : visibility;
        final boolean isInside = inside != null && inside;
        final boolean isDeep = deep != null && deep;

        List<Project> projectsToCheck = new ArrayList<>();
        var resp = getProjectsToCheck(projectsToCheck, projectName);
        if (resp != null) return resp;

        List<String> resolvedFQNs = new ArrayList<>();
        resp = resolveClassName(className, projectsToCheck, resolvedFQNs);
        if (resp != null) return resp;

        String fqn = resolvedFQNs.get(0);
        List<String> allMethodSignatures = new ArrayList<>();
        for (Project project : projectsToCheck) {
            List<String> signatures = IdeaUtils.read(() ->
                IdeaUtils.getClassMethods(project, fqn, vis, isInside, isDeep));
            allMethodSignatures.addAll(signatures);
        }
        try {
            String json = MAPPER.writeValueAsString(allMethodSignatures);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing response").build();
        }
    }

    @GET
    @Path("class-fields")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClassFields(@QueryParam("class") String className,
                                   @QueryParam("project") String projectName,
                                   @QueryParam("visibility") String visibility,
                                   @QueryParam("inside") Boolean inside,
                                   @QueryParam("deep") Boolean deep) {
        FlashPlugin.LOGGER.debug("Class fields endpoint called - class: " + className + ", project: " + projectName + ", visibility: " + visibility + ", inside: " + inside + ", deep: " + deep);
        if (className == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing 'class' parameter").build();
        }

        final String vis = visibility == null ? "public" : visibility;
        final boolean isInside = inside != null && inside;
        final boolean isDeep = deep != null && deep;

        List<Project> projectsToCheck = new ArrayList<>();
        var resp = getProjectsToCheck(projectsToCheck, projectName);
        if (resp != null) return resp;

        List<String> resolvedFQNs = new ArrayList<>();
        resp = resolveClassName(className, projectsToCheck, resolvedFQNs);
        if (resp != null) return resp;

        String fqn = resolvedFQNs.get(0);
        List<String> allFieldSignatures = new ArrayList<>();
        for (Project project : projectsToCheck) {
            List<String> signatures = IdeaUtils.read(() ->
                IdeaUtils.getClassFields(project, fqn, vis, isInside, isDeep));
            allFieldSignatures.addAll(signatures);
        }
        try {
            String json = MAPPER.writeValueAsString(allFieldSignatures);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing response").build();
        }
    }


    @GET
    @Path("class-info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClassInfo(@QueryParam("class") String className,
                                 @QueryParam("project") String projectName,
                                 @QueryParam("fullInheritance") Boolean fullInheritance) {
        FlashPlugin.LOGGER.debug("Class info endpoint called - class: " + className + ", project: " + projectName + ", fullInheritance: " + fullInheritance);
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
        for (Project project : projectsToCheck) {
            Map<String, Object> info = IdeaUtils.read(() ->
                IdeaUtils.getClassInfo(project, fqn, fullInheritance != null && fullInheritance));

            if (info != null) {
                FlashPlugin.LOGGER.debug("Found class info in project " + project.getName());
                try {
                    String json = MAPPER.writeValueAsString(info);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                } catch (JsonProcessingException e) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing response").build();
                }
            }
        }
        FlashPlugin.LOGGER.debug("No class info found for class: " + fqn);
        return Response.status(Response.Status.NOT_FOUND).entity("Class info not found").build();
    }

}
