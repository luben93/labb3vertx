package io.vertx.example.chat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.example.util.Runner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

/**
 * A {@link io.vertx.core.Verticle} which implements a simple, realtime,
 * multiuser chat. Anyone can connect to the chat application on port
 * 8000 and type messages. The messages will be rebroadcast to all
 * connected users via the @{link EventBus} Websocket bridge.
 *
 * @author <a href="https://github.com/InfoSec812">Deven Phillips</a>
 */
public class Server extends AbstractVerticle {

  // Convenience method so you can run it in your IDE
  public static void main(String[] args) {
    Runner.runExample(Server.class);
  }

  @Override
  public void start() throws Exception {

    Router router = Router.router(vertx);

    // Allow events for the designated addresses in/out of the event bus bridge
    BridgeOptions opts = new BridgeOptions()
            .addInboundPermitted(new PermittedOptions().setAddress("chat.to.server"))
            .addInboundPermitted(new PermittedOptions().setAddress("logon.to.server"))
            .addOutboundPermitted(new PermittedOptions().setAddressRegex("\\w+\\.\\w+\\.\\w+\\.(\\d+)\\.(\\d+)"));

    // Create the event bus bridge and add it to the router.
    SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
//    router.route("/eventbus/*").handler(ebHandler);
    router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(opts, event -> {
      if (event.type() == BridgeEventType.SOCKET_CREATED) {
//        System.out.println("A socket was created");
      }
//      System.out.println(event.type());
      event.complete(true);
    }));

      router.route().handler(StaticHandler.create());

    // Start the web server and tell it to use the router to handle requests.
    vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    EventBus eb = vertx.eventBus();

    eb.consumer("logon.to.server").handler(search -> {
      // Create a timestamp string
      //String timestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date.from(Instant.now()));
      // Send the message back out to all clients with the timestamp prepended.
      //eb.publish("chat.to.client", timestamp + ": " + "hi, and welcome to our domain, you must now complie to all of my commands");
      System.out.println(" msg: "+search.body());
    });

    // Register to listen for messages coming IN to the server
    eb.consumer("chat.to.server").handler(message -> {
      JsonObject object = (JsonObject) message.body();

      System.out.println("adr: "+message.address()+" replyadr: "+message.replyAddress()+" header: "+message.headers()+" msg: "+message.body()+" json room: "+ object.getString("room"));

      // Create a timestamp string
      String timestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date.from(Instant.now()));
      // Send the message back out to all clients with the timestamp prepended.
    //  eb.send("chat.to.client.1.1", timestamp + ": " +object.getString("message"));
      eb.send("chat.to.client."+object.getString("room"), timestamp + ": " +object.getString("message"));
      eb.send("chat.to.client."+object.getString("myroom"), timestamp + ": " +object.getString("message"));

    });



  }
}