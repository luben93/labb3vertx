package io.vertx.example.chat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.example.util.Runner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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
    router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(opts));

      router.route().handler(StaticHandler.create());

    // Start the web server and tell it to use the router to handle requests.
    vertx.createHttpServer().requestHandler(router::accept).listen(8082);
    EventBus eb = vertx.eventBus();

    //logon to server and rest get
    eb.consumer("logon.to.server").handler(message -> {
              System.out.println(" msg: " + message.body());
              JsonObject object = (JsonObject) message.body();
              JsonObject respones = new JsonObject(restGetChatHistory(object.getString("id"), object.getString("other")));
              JsonArray arr = respones.getJsonArray("list");
              for (Object Oline : arr) {
                JsonObject line = (JsonObject) Oline;
                System.out.println(line);
                System.out.println(line.getClass());
//        line.getString("message")
        eb.send("chat.to.client."+object.getString("id")+"."+object.getString("other"), line.getInteger("sender") + ": " + line.getString("message"));
              }

//      eb.send("chat.to.client."+object.getString("id")+"."+object.getString("other"), line.getString("sender") + ": " +     line.getString("message")

    });

    // Register to listen for messages coming IN to the server
    eb.consumer("chat.to.server").handler(message -> {
      JsonObject object = (JsonObject) message.body();

      System.out.println("adr: "+message.address()+" replyadr: "+message.replyAddress()+" header: "+message.headers()+" msg: "+message.body()+" json room: "+ object.getString("room"));


      String id =object.getString("id");
      eb.send("chat.to.client."+id+"."+object.getString("other"), id + ": " +object.getString("message"));
      eb.send("chat.to.client."+object.getString("other")+"."+id, id + ": " +object.getString("message"));
      restPostMessage(id,object.getString("other"),object.getString("message"));
    });

  }

  //private static String rest="http://localhost:8081/rest/chat";
  private static String rest="http://130.237.84.10:8081/starter/rest/chat";

  private void restPostMessage(String id,String other,String msg){
    try {

      URL url = new URL(rest);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");

      String input = "{sender:"+id+",recvier:"+other+",message:\""+msg+"\"}";
//      String input = "{\"sender\":"+id+",\"recvier\":"+other+",\"message\":\""+msg+"\"}";
      System.out.println(input);
      OutputStream os = conn.getOutputStream();
      os.write(input.getBytes());
      os.flush();
//
//      if (conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
////        throw new RuntimeException("Failed : HTTP error code : "
////                + conn.getResponseCode());
//        System.out.println(conn.getResponseCode());
//      }
//
//      BufferedReader br = new BufferedReader(new InputStreamReader(
//              (conn.getInputStream())));
//
//      String output;
//      System.out.println("Output from Server .... \n");
//      while ((output = br.readLine()) != null) {
//        System.out.println(output);
//      }

      conn.disconnect();

    } catch (MalformedURLException e) {

      e.printStackTrace();

    } catch (IOException e) {

      e.printStackTrace();

    }
  }

  private String restGetChatHistory(String id,String other){
    String fullOutput="";

    try {

      URL url = new URL(rest+"/"+id+"/"+other);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/json");

      if (conn.getResponseCode() != 200) {
        throw new RuntimeException("Failed : HTTP error code : "
                + conn.getResponseCode());
//        System.out.println("respone code "+conn.getResponseCode());
      }

      BufferedReader br = new BufferedReader(new InputStreamReader(
              (conn.getInputStream())));

      String output;
      System.out.println("Output from Server .... \n");
      while ((output = br.readLine()) != null) {
        System.out.println(output);
        fullOutput += output;
      }

      conn.disconnect();

    } catch (MalformedURLException e) {

      e.printStackTrace();

    } catch (IOException e) {

      e.printStackTrace();

    }
    System.out.println("all done");
    return fullOutput;
  }
}