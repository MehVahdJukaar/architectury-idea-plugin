package net.mehvahdjukaar.flash.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.mehvahdjukaar.flash.FlashPlugin;
import net.mehvahdjukaar.flash.IdeaUtils;

@Path("/")
public class RootServlet {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getRoot() {
        String html = "<html><body>" +
            "<h1>Candlelight Plugin HTTP Server</h1>" +
            "<p>Server is running on port 4303</p>" +
            "<h2>Available Endpoints</h2>" +
            "<ul>" +
            "<li><a href=\"/content/method?class=java.lang.String&method=toString\">/content/method?class=java.lang.String&method=toString</a> - Get content of a method</li>" +
            "<li><a href=\"/content/class?class=java.lang.String\">/content/class?class=java.lang.String</a> - Get full content of a class</li>" +
            "<li><a href=\"/content/partial-class?class=java.lang.String&startLine=1&endLine=10\">/content/partial-class?class=java.lang.String&startLine=1&endLine=10</a> - Get partial content of a class</li>" +
            "<li><a href=\"/content/containing-method?class=java.lang.String&line=5\">/content/containing-method?class=java.lang.String&line=5</a> - Get the method containing a line</li>" +
            "<li><a href=\"/usages/method?class=java.lang.String&method=toString\">/usages/method?class=java.lang.String&method=toString</a> - Get callers of a method</li>" +
            "<li><a href=\"/usages/class?class=java.lang.String\">/usages/class?class=java.lang.String</a> - Get usages of a class</li>" +
            "<li><a href=\"/usages/field?class=java.lang.String&field=value\">/usages/field?class=java.lang.String&field=value</a> - Get usages of a field</li>" +
            "<li><a href=\"/structure/class-info?class=java.lang.String\">/structure/class-info?class=java.lang.String</a> - Get information about a class</li>" +
            "<li><a href=\"/structure/declaration?class=java.lang.String&method=toString\">/structure/declaration?class=java.lang.String&method=toString</a> - Get declaration of a method</li>" +
            "<li><a href=\"/structure/implementations?class=java.lang.String&method=toString\">/structure/implementations?class=java.lang.String&method=toString</a> - Get implementations of a method</li>" +
            "<li><a href=\"/structure/class-inheritors?class=java.lang.String\">/structure/class-inheritors?class=java.lang.String</a> - Get inheritors of a class</li>" +
            "<li><a href=\"/structure/class-api?class=java.lang.String\">/structure/class-api?class=java.lang.String</a> - Get method signatures of a class</li>" +
            "<li><a href=\"/structure/class-fields?class=java.lang.String\">/structure/class-fields?class=java.lang.String</a> - Get field signatures of a class</li>" +
            "</ul>" +
            "<h3>Debug Info</h3>" +
            "<p>Open projects: " + IdeaUtils.getOpenProjectNames() + "</p>" +
            "</body></html>";
        FlashPlugin.LOGGER.debug("Root endpoint accessed");
        return Response.ok(html).build();
    }

}
