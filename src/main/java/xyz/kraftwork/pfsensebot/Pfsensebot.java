/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package xyz.kraftwork.pfsensebot;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import xyz.kraftwork.chatbot.ChatInfo;
import xyz.kraftwork.chatbot.Chatbot;
import xyz.kraftwork.chatbot.MessageListener;
import xyz.kraftwork.chatbot.utils.ConfigurationHolder;
import xyz.kraftwork.pfsensebot.utils.RestUtils;

public class Pfsensebot implements MessageListener {

    private static final String GET_USERS = "pfsense:get_users";
    private static final String CREATE_USER = "pfsense:create_user";
    private static final String REMOVE_USER = "pfsense:remove_user";
    private final Chatbot bot;

    public Pfsensebot() {
        this.bot = new Chatbot();
        bot.addMessageListener(this);
    }

    @Override
    public Object onMessage(ChatInfo info) {
        System.out.println("======== ON MESSAGE");
        String[] args = info.getMessage().split(" ");
        switch (args[0]) {
            case GET_USERS -> {
                getUsers(info);
            }
            case CREATE_USER -> {
                createUser(info, StringUtils.stripAll(args));
            }
            case REMOVE_USER -> {
                removeUser(info, StringUtils.stripAll(args));
            }
        }
        return null;
    }

    private void removeUser(ChatInfo info, String[] args) {
        if (args.length != 3) {
            info.setMessage(String.format("Incorrect number of arguments, expecting 3, got %d", args.length));
            bot.sendMessage(info);
            return;
        }

    }

    private ArrayList<Map> getUsers() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                // .sslContext(sslContext)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildURL("/api/v1/user")))
                .header("Authorization", RestUtils.basicAuth(ConfigurationHolder.getInstance().get("PFSENSE_USERNAME"), ConfigurationHolder.getInstance().get("PFSENSE_PASSWORD")))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String json = (response.body());
        Gson gson = new Gson();
        Map map = gson.fromJson(json, Map.class);

        return (ArrayList<Map>) map.get("data");
    }

    private void getUsers(ChatInfo info) {
        System.out.println("====== ON GET USERS");
        try {

            StringBuilder result = new StringBuilder();
            for (Map user : getUsers()) {
                System.out.println(user);
                result.append(user.get("name"));
                result.append(", ");
            }
            info.setMessage(result.toString());
            bot.sendMessage(info);
        } catch (Exception ex) {
            System.out.println("uh oh");
            Logger.getLogger(Pfsensebot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        Pfsensebot bot = new Pfsensebot();

    }

    private void createUser(ChatInfo info, String[] args) {
        try {
            if (args.length > 4 && args.length < 3) {
                info.setMessage(String.format("Incorrect number of arguments, expecting 3-4, got %d", args.length));
                bot.sendMessage(info);
                return;
            }
            String firstName = args[1];
            String lastName = args[2];
            int number = 1;
            try {
                number = Integer.parseInt(args[3]);
            } catch (Exception ex) {

            }
            for (int i = 1; i <= number; i++) {
                String username = firstName + "." + lastName + "." + i;
                String pass = UUID.randomUUID().toString();
                String body = String.format("""
                        {
                          "authorizedkeys": "",
                          "cert": [],
                          "descr": "auto generated",
                          "disabled": false,
                          "expires": "",
                          "ipsecpsk": "",
                          "password": "%s",
                          "priv": ["user-services-captiveportal-login"],
                          "username": "%s"
                        }""", pass, username);
                HttpClient client = HttpClient.newBuilder()
                        // .sslContext(sslContext)
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(buildURL("/api/v1/user")))
                        .header("Authorization", RestUtils.basicAuth("admin", "thaadminrlz"))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String json = (response.body());
                Gson gson = new Gson();
                Map map = gson.fromJson(json, Map.class);
                System.out.println("============");
                System.out.println(map);
                info.setMessage(String.format("User generated %s password %s", username, pass));
                bot.sendMessage(info);
            }

        } catch (Exception ex) {
            Logger.getLogger(Pfsensebot.class.getName()).log(Level.SEVERE, null, ex);
            info.setMessage(ex.getMessage());
            bot.sendMessage(info);
        }
    }

    public String buildURL(String path) {
        ConfigurationHolder config = ConfigurationHolder.getInstance();
        return config.get("PFSENSE_IP") + path;
    }

}
