package htmx.todo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.Javalin;
import io.jstach.jstache.JStache;
import io.jstach.jstachio.JStachio;

public class Server {

	private static final Logger log = LoggerFactory.getLogger(Server.class);

	@JStache(path = "todoPage")
	public record TodoPage(List<TodoItemView> todos) {
	}

	@JStache(path = "todoPage#todoList")
	public record TodoListView(List<TodoItemView> todos) {
	}

	@JStache(path = "todoPage#todoItem")
	public record TodoItemView(String content, String id, boolean completed, boolean editing) {
	}

	/**
	 * Represents a single todo item.
	 */
	public record Todo(String content, boolean completed) {
	}

	private final Javalin javalin;

	public Server() {
		// In-memory 'todo' store.
		Map<String, Todo> idToTodo = new LinkedHashMap<>();
		idToTodo.put(UUID.randomUUID().toString(), new Todo("Buy milk", false));

		// Set up Javalin server.
		javalin = Javalin.create(config -> {
			config.useVirtualThreads = true;

			config.staticFiles.enableWebjars();

			config.requestLogger.http((ctx, ms) -> {
				log.info("{} {} {} {}ms", ctx.req().getMethod(), ctx.path(), ctx.status(), ms);
			});
		});

		// Display base page
		javalin.get("/", ctx -> {
			ctx.header("Cache-Control", "no-store");
			ctx.html(JStachio.render(new TodoPage(mapStoreToViewList(idToTodo))));
		});

		// Create new todo item.
		javalin.post("/todos", ctx -> {
			String newContent = ctx.formParam("content");
			idToTodo.put(UUID.randomUUID().toString(), new Todo(newContent, false));
			ctx.html(JStachio.render(new TodoListView(mapStoreToViewList(idToTodo))));
		});

		// Update the content of a todo item.
		javalin.post("/todos/{id}", ctx -> {
			String id = ctx.pathParam("id");
			String newContent = ctx.formParam("value");

			Todo updatedTodo = idToTodo.computeIfPresent(id,
					(_id, oldTodo) -> new Todo(newContent, oldTodo.completed()));

			ctx.html(JStachio.render(new TodoItemView(updatedTodo.content(), id, updatedTodo.completed(), false)));
		});

		// Toggle the completed status of a specified todo item.
		javalin.post("/todos/{id}/toggle", ctx -> {
			String id = ctx.pathParam("id");

			Todo updatedTodo = idToTodo.computeIfPresent(id,
					(_id, oldTodo) -> new Todo(oldTodo.content(), !oldTodo.completed()));

			ctx.html(JStachio.render(new TodoItemView(updatedTodo.content(), id, updatedTodo.completed(), false)));
		});

		// Returns an 'edit' view of a todo item.
		javalin.post("/todos/{id}/edit", ctx -> {
			String id = ctx.pathParam("id");

			Todo todo = idToTodo.get(id);

			ctx.html(JStachio.render(new TodoItemView(todo.content(), id, todo.completed(), true)));
		});
	}

	private List<TodoItemView> mapStoreToViewList(Map<String, Todo> map) {
		return map.entrySet().stream()
				.map(e -> new TodoItemView(e.getValue().content(), e.getKey(), e.getValue().completed(), false))
				.toList();
	}

	private void start() {
		javalin.start();
	}

	public static void main(String[] args) {
		Server server = new Server();
		server.start();
	}
}
