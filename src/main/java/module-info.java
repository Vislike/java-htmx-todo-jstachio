import io.jstach.jstache.JStacheConfig;
import io.jstach.jstache.JStachePath;

@JStacheConfig(pathing = @JStachePath(prefix = "templates/", suffix = ".mustache")) module vislike.htmx.todo.jstachio {
	requires io.javalin;
	requires io.jstach.jstache;
	requires io.jstach.jstachio;

	opens htmx.todo to io.jstach.jstachio;
}