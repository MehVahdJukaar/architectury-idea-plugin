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
            "<li><a href=\"/superclasses?class=java.lang.String\">/superclasses?class=java.lang.String</a> - Get superclasses for a class</li>" +
            "<li><a href=\"/method-content?class=java.lang.String&method=toString\">/method-content?class=java.lang.String&method=toString</a> - Get content of a method</li>" +
            "<li><a href=\"/class-content?class=java.lang.String\">/class-content?class=java.lang.String</a> - Get full content of a class</li>" +
            "<li><a href=\"/partial-class-content?class=java.lang.String&startLine=1&endLine=10\">/partial-class-content?class=java.lang.String&startLine=1&endLine=10</a> - Get partial content of a class</li>" +
            "<li><a href=\"/containing-method?class=java.lang.String&line=5\">/containing-method?class=java.lang.String&line=5</a> - Get the method containing a line</li>" +
            "<li><a href=\"/callers?class=java.lang.String&method=toString\">/callers?class=java.lang.String&method=toString</a> - Get callers of a method</li>" +
            "<li><a href=\"/declaration?class=java.lang.String&method=toString\">/declaration?class=java.lang.String&method=toString</a> - Get declaration of a method</li>" +
            "<li><a href=\"/implementations?class=java.lang.String&method=toString\">/implementations?class=java.lang.String&method=toString</a> - Get implementations of a method</li>" +
            "<li><a href=\"/class-info?class=java.lang.String\">/class-info?class=java.lang.String</a> - Get information about a class</li>" +
            "<li><a href=\"/class-usages?class=java.lang.String\">/class-usages?class=java.lang.String</a> - Get usages of a class</li>" +
            "<li><a href=\"/field-usages?class=java.lang.String&field=value\">/field-usages?class=java.lang.String&field=value</a> - Get usages of a field</li>" +
            "<li><a href=\"/class-inheritors?class=java.lang.String\">/class-inheritors?class=java.lang.String</a> - Get inheritors of a class</li>" +
            "<li><a href=\"/class-api?class=java.lang.String\">/class-api?class=java.lang.String</a> - Get method signatures of a class</li>" +
            "<li><a href=\"/class-fields?class=java.lang.String\">/class-fields?class=java.lang.String</a> - Get field signatures of a class</li>" +
            "</ul>" +
            "<h3>Debug Info</h3>" +
            "<p>Open projects: " + IdeaUtils.getOpenProjectNames() + "</p>" +
            "</body></html>";
        FlashPlugin.LOGGER.debug("Root endpoint accessed");
        return Response.ok(html).build();
    }

}
