package com.ringfulhealth.chatbotbook.slack.events;

import com.ringfulhealth.bots.Util;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseServlet extends HttpServlet {

    protected String client_id = "";
    protected String client_secret = "";

    protected String zencoder_apikey = "";
    protected String audio_bluemix_username = "";
    protected String audio_bluemix_password = "";

    protected String nlp_bluemix_username = "";
    protected String nlp_bluemix_password = "";
    protected String nlp_bluemix_id = "";

    protected String conv_bluemix_username = "";
    protected String conv_bluemix_password = "";
    protected String conv_bluemix_id = "";

    protected static ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> cache;
    static {
        cache = new ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> ();
    }

    public abstract Object converse (String human, ConcurrentHashMap<String, Object> context);

    public abstract void saveToken (String team_id, String token);
    public abstract String findToken (String team_id);

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        doPost(req, resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String mode = null; // could be "events" or "buttons"
        String body = null;
        if (req.getParameter("payload") == null || req.getParameter("payload").isEmpty()) {
            // Read POST body from request
            StringBuilder buffer = new StringBuilder();
            BufferedReader reader = req.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            body = buffer.toString();
            mode = "events";
        } else {
            body = req.getParameter("payload");
            mode = "buttons";
        }
        System.out.println(body);

        // See if this is a challenge
        try {
            JSONObject json = new JSONObject(body);
            if (json == null) {
                // fail silently
                return;
            }
            if ("url_verification".equalsIgnoreCase(json.getString("type"))) {
                resp.setContentType("text/plain");
                resp.getOutputStream().println(json.getString("challenge"));
                return;
            }
        } catch (Exception e) {
            // fails silently
        }

        // Check if there is an OAUTH code
        String code = req.getParameter("code");
        if (code != null && !code.isEmpty()) {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            CloseableHttpResponse response = null;
            try {
                HttpPost httpPost = new HttpPost("https://slack.com/api/oauth.access");
                List <NameValuePair> nvps = new ArrayList <NameValuePair>();
                nvps.add(new BasicNameValuePair("client_id", client_id));
                nvps.add(new BasicNameValuePair("client_secret", client_secret));
                nvps.add(new BasicNameValuePair("code", code));
                httpPost.setEntity(new UrlEncodedFormEntity(nvps));
                response = httpclient.execute(httpPost);

                HttpEntity entity = response.getEntity();
                String resp_body = EntityUtils.toString(entity);
                System.out.println(response.getStatusLine().getStatusCode() + " : " + resp_body);

                JSONObject json = new JSONObject(resp_body);
                String team_id = json.getString("team_id");
                String bot_id = json.getJSONObject("bot").getString("bot_user_id");
                String token = json.getJSONObject("bot").getString("bot_access_token");
                saveToken(team_id, token);
                resp.sendRedirect("auth_success.jsp");
                return;

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (httpclient != null) {
                        httpclient.close();
                    }
                } catch (Exception e) {}
                try {
                    if (response != null) {
                        response.close();
                    }
                } catch (Exception e) {}
            }
        }

        // HTTP connection closes here. But the servlet continues to run.
        resp.getOutputStream().close();

        String message = null, channel = null, sender_id = null, team_id = null;
        try {
            JSONObject json = new JSONObject(body);

            // This is an new message event
            if ("events".equalsIgnoreCase(mode) && json.has("event")) {
                JSONObject event = json.getJSONObject("event");
                team_id = json.getString("team_id");

                String type = null;
                try {
                    type = event.getString("type");
                } catch (Exception ee) {
                    type = null;
                }
                String subtype = null;
                try {
                    subtype = event.getString("subtype");
                } catch (Exception ee) {
                    subtype = null;
                }

                // if (event.getString("type").startsWith("message")) {
                if ("message".equalsIgnoreCase(type) && !"bot_message".equalsIgnoreCase(subtype)) {
                    // JSONObject item = event.getJSONObject("item");
                    channel = event.getString("channel");
                    sender_id = event.getString("user");
                    message = event.getString("text");
                }
            }

            // This is an interactive button click
            if ("buttons".equalsIgnoreCase(mode) && json.has("actions")) {
                JSONArray actions = json.getJSONArray("actions");
                team_id = json.getJSONObject("team").getString("id");
                channel = json.getJSONObject("channel").getString("id");
                sender_id = json.getJSONObject("user").getString("id");

                if (actions != null && actions.length() > 0) {
                    message = actions.getJSONObject(0).getString("value");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException (e);
        }


        try {
            // The token should exist
            String token = findToken(team_id);
            if (token == null) {
                // We need to alert the system admin so that she can ask the users to re-authorize OAUTH
                System.out.print("SLACK token for team_id " + team_id + " is NULL");
                return;
            }
            if (message != null && channel != null) {
                ConcurrentHashMap<String, Object> context = cache.get(channel + "_context");
                if (context == null) {
                    context = new ConcurrentHashMap<String, Object> ();
                    cache.put(channel + "_context", context);
                }
                context.put("channel", channel);
                context.put("sender_id", sender_id);
                context.put("team_id", team_id);

                Object bot_says = converse(message.trim(), context);
                if (bot_says instanceof List) {
                    for (Object bs : (List) bot_says) {
                        sendReply(bs, channel, token);
                    }
                } else {
                    sendReply(bot_says, channel, token);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException (e);
        }
    }

    public void sendReply (Object reply, String channel, String token) throws Exception {
        if (reply == null) {
            return; // No action
        }

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost("https://slack.com/api/chat.postMessage");
            List <NameValuePair> nvps = new ArrayList <NameValuePair>();
            nvps.add(new BasicNameValuePair("token", token));
            nvps.add(new BasicNameValuePair("channel", channel));
            if (reply instanceof String) {
                nvps.add(new BasicNameValuePair("text", (String) reply));
            } else if (reply instanceof JSONArray) {
                nvps.add(new BasicNameValuePair("attachments", ((JSONArray) reply).toString()));
            } else if (reply instanceof JSONObject) {
                JSONArray a = new JSONArray();
                a.put(reply);
                nvps.add(new BasicNameValuePair("attachments", (a.toString())));
            } else {
                throw new Exception ("Invalid return value from converse() method");
            }
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            response = httpclient.execute(httpPost);

            HttpEntity entity = response.getEntity();
            String resp_body = EntityUtils.toString(entity);
            System.out.println(response.getStatusLine().getStatusCode() + " : " + resp_body);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (httpclient != null) {
                    httpclient.close();
                }
            } catch (Exception e) {}
            try {
                if (response != null) {
                    response.close();
                }
            } catch (Exception e) {}
        }
    }

    // This is for the buttons template
    public static JSONObject createButtons (String title, HashMap <String, String> buttons) throws Exception {
        JSONObject payload = new JSONObject();
        if (title != null && !title.trim().isEmpty()) {
            payload.put("title", title);
        }
        payload.put("callback_id", Util.randomNumericString(6));
        payload.put("actions", createButtons(buttons.keySet().toArray(new String[] {}), buttons.values().toArray(new String[] {})));
        return payload;
    }

    // This is for the buttons template
    public static JSONArray createButtons (String [] button_titles, String [] button_payloads) throws Exception {
        if (button_titles == null || button_payloads == null || button_titles.length == 0 || button_payloads.length == 0 || button_titles.length != button_payloads.length) {
            throw new Exception ("Buttons titles and payloads mismatch");
        }

        JSONArray arr = new JSONArray ();
        for (int i = 0; i < button_titles.length; i++) {
            arr.put((new JSONObject()).put("name", button_payloads[i]).put("text", button_titles[i]).put("type", "button").put("value", button_payloads[i]));
        }
        return arr;
    }

    public static boolean containsWord (String test, String [] list) {
        for (String s : list) {
            if (test.toUpperCase().contains(" " + s.toUpperCase()) || test.toUpperCase().contains(s.toUpperCase() + " ") || test.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    // This one returns an URL of the transcoded file. It uses zencoder to do this.
    public String transcodeAudio (String orgUrl, String target_format, int sample_rate) {
        String target_url = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost("https://app.zencoder.com/api/v2/jobs");
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpPost.setHeader("Zencoder-Api-Key", zencoder_apikey);

            JSONObject json = new JSONObject();
            JSONArray outputs = new JSONArray ();
            JSONObject output = new JSONObject ();
            output.put("format", target_format);
            output.put("audio_sample_rate", sample_rate);
            outputs.put(output);
            json.put("input", orgUrl);
            json.put("outputs", outputs);
            System.out.println(json.toString());

            httpPost.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
            response = httpclient.execute(httpPost);

            HttpEntity entity = response.getEntity();
            String respBody = EntityUtils.toString(entity);
            response.close();
            System.out.println(respBody);

            json = new JSONObject(respBody);
            long job_id = json.getLong("id");
            target_url = ((JSONObject) json.getJSONArray("outputs").get(0)).getString("url");

            while (!respBody.startsWith("{\"state\":\"finished\"")) {
                Thread.sleep(1000);
                HttpGet httpGet = new HttpGet("https://app.zencoder.com/api/v2/jobs/" + job_id + "/progress.json?api_key=" + zencoder_apikey);
                response = httpclient.execute(httpGet);
                entity = response.getEntity();
                respBody = EntityUtils.toString(entity);
                response.close();
                System.out.println(respBody);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (httpclient != null) {
                    httpclient.close();
                }
            } catch (Exception e) {}
            try {
                if (response != null) {
                    response.close();
                }
            } catch (Exception e) {}
        }
        return target_url;
    }

    // This one uses IBM Watson Speech to Text service
    public String speechToText (String url, String format) throws Exception {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        byte [] sound_data;
        try {
            System.out.println("sound_data url is " + url);
            HttpGet httpGet = new HttpGet(url);
            response = httpclient.execute(httpGet);

            HttpEntity entity = response.getEntity();
            sound_data = EntityUtils.toByteArray(entity);
            System.out.println("sound_data length is " + sound_data.length);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        String transcript = null;
        double confidence;
        try {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(audio_bluemix_username, audio_bluemix_password)
            );
            // HttpClientContext context = HttpClientContext.create();
            // context.setCredentialsProvider(credsProvider);
            httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

            HttpPost httpPost = new HttpPost("https://stream.watsonplatform.net/speech-to-text/api/v1/recognize?timestamps=true&word_alternatives_threshold=0.9");
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "audio/" + format);
            httpPost.setEntity(new ByteArrayEntity(sound_data));
            response = httpclient.execute(httpPost);

            HttpEntity entity = response.getEntity();
            String respBody = EntityUtils.toString(entity);
            System.out.println(respBody);

            JSONObject json = new JSONObject(respBody);
            JSONObject alt = json.getJSONArray("results").getJSONObject(0).getJSONArray("alternatives").getJSONObject(0);
            transcript = alt.getString("transcript");
            confidence = alt.getDouble("confidence");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (httpclient != null) {
                    httpclient.close();
                }
            } catch (Exception e) {}
            try {
                if (response != null) {
                    response.close();
                }
            } catch (Exception e) {}
        }

        return transcript;
    }

    public static List<String> splitByWord (String str, int segment_length) {
        // http://stackoverflow.com/questions/25853393/split-a-string-in-java-into-equal-length-substrings-while-maintaining-word-bound
        List <String> split = new ArrayList <String> ();
        Pattern p = Pattern.compile("\\G\\s*(.{1," + segment_length + "})(?=\\s|$)", Pattern.DOTALL);
        Matcher m = p.matcher(str);
        while (m.find()) {
            split.add(m.group(1));
        }
        return split;
    }

    public static String trim(String s, int len) {
        return s.substring(0, Math.min(s.length(), len));
    }

    public static String fillSlot (List<Hashtable<String, String>>slots, String word) {
        for (Hashtable<String, String> slot : slots) {
            if (slot.get("value").isEmpty()) {
                if (!word.isEmpty()) {
                    if (word.trim().matches(slot.get("valid"))) {
                        slot.put("value", word.trim());
                        break;
                    } else {
                        return "Sorry, your input is not valid. " + slot.get("prompt");
                    }
                }
            }
        }

        for (Hashtable<String, String> slot : slots) {
            if (slot.get("value").isEmpty()) {
                return slot.get("prompt");
            }
        }

        return "";
    }

    public String classifyText (String str) {
        // List<NameValuePair> formparams = new ArrayList<NameValuePair> ();
        // formparams.add(new BasicNameValuePair("text", str));

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        try {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(nlp_bluemix_username, nlp_bluemix_password)
            );
            // HttpClientContext context = HttpClientContext.create();
            // context.setCredentialsProvider(credsProvider);
            httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

            HttpGet httpGet = new HttpGet("https://gateway.watsonplatform.net/natural-language-classifier/api/v1/classifiers/" + nlp_bluemix_id + "/classify?text=" + URLEncoder.encode(str));
            httpGet.addHeader("Content-Type","application/x-www-form-urlencoded");
            response = httpclient.execute(httpGet);

            HttpEntity entity = response.getEntity();
            String respBody = EntityUtils.toString(entity);
            System.out.println(respBody);

            JSONObject json = new JSONObject(respBody);
            String answer = json.getString("top_class");
            double confidence = json.getJSONArray("classes").getJSONObject(0).getDouble("confidence");

            System.out.println("CLASSIFED: " + answer + " with confidence " + confidence + " - " + str);
            if (confidence > 0.75) {
                return answer;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (httpclient != null) {
                    httpclient.close();
                }
            } catch (Exception e) {}
            try {
                if (response != null) {
                    response.close();
                }
            } catch (Exception e) {}
        }

        return "";
    }

    public JSONObject watsonConversation (String str) {
        // List<NameValuePair> formparams = new ArrayList<NameValuePair> ();
        // formparams.add(new BasicNameValuePair("text", str));

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        try {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(conv_bluemix_username, conv_bluemix_password)
            );
            // HttpClientContext context = HttpClientContext.create();
            // context.setCredentialsProvider(credsProvider);
            httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

            JSONObject json_req = new JSONObject();
            json_req.put("input", (new JSONObject()).put("text", str));

            HttpPost httpPost = new HttpPost("https://gateway.watsonplatform.net/conversation/api/v1/workspaces/" + conv_bluemix_id + "/message?version=2016-07-11");
            httpPost.addHeader("Content-Type","application/json");
            httpPost.setEntity(new StringEntity(json_req.toString()));
            response = httpclient.execute(httpPost);

            HttpEntity entity = response.getEntity();
            String respBody = EntityUtils.toString(entity);
            System.out.println(respBody);

            JSONObject json = new JSONObject(respBody);
            return json;
            /*
            String answer = json.getJSONArray("intents").getJSONObject(0).getString("intent");
            double confidence = json.getJSONArray("intents").getJSONObject(0).getDouble("confidence");

            System.out.println("CLASSIFED: " + answer + " with confidence " + confidence + " - " + str);
            if (confidence > 0.75) {
                return answer;
            }
            */
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (httpclient != null) {
                    httpclient.close();
                }
            } catch (Exception e) {}
            try {
                if (response != null) {
                    response.close();
                }
            } catch (Exception e) {}
        }

        return null;
    }
}
