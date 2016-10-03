package com.ringfulhealth.bots.slack;

import com.ringfulhealth.bots.*;
import com.ringfulhealth.chatbotbook.slack.events.BaseServlet;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.EntityManagerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsServlet extends BaseServlet {

    public NewsServlet () {
        client_id = Constants.slack_client_id;
        client_secret = Constants.slack_client_secret;

        zencoder_apikey = Constants.zencoder_apikey;
        audio_bluemix_username = Constants.audio_bluemix_username;
        audio_bluemix_password = Constants.audio_bluemix_password;

        nlp_bluemix_username = Constants.nlp_bluemix_username;
        nlp_bluemix_password = Constants.nlp_bluemix_password;
        nlp_bluemix_id = Constants.nlp_bluemix_id;

        conv_bluemix_username = Constants.conv_bluemix_username;
        conv_bluemix_password = Constants.conv_bluemix_password;
        conv_bluemix_id = Constants.conv_bluemix_id;
    }

    private EntityManagerFactory emf;

    public void saveToken (String team_id, String token) {
        if (emf == null) {
            emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        }
        DataManager dm = new DataManager (emf);

        Token t = dm.getToken(team_id);
        if (t == null) {
            t = new Token ();
        }
        t.setTeamId(team_id);
        t.setBotToken(token);
        dm.saveToken(t);
    }

    public String findToken (String team_id) {
        if (emf == null) {
            emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        }
        DataManager dm = new DataManager (emf);

        Token t = dm.getToken(team_id);
        if (t != null) {
            return t.getBotToken();
        }
        return null;
    }

    public Object converse (String human, ConcurrentHashMap<String, Object> context) {
        System.out.println("SlackdWServlet converse: " + human);

        if (emf == null) {
            emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        }
        DataManager dm = new DataManager (emf);

        boolean new_user = false;
        User user = dm.getSlackUser((String) context.get("channel"));
        if (user == null) {
            user = new User ();
            user.setSlackId((String) context.get("sender_id"));
            user.setSlackTeamId((String) context.get("team_id"));
            user.setSlackChannel((String) context.get("channel"));
            /*
            HashMap profile = getUserProfile(user.getFbId());
            if (profile != null && !profile.isEmpty()) {
                user.setFirst_name((String) profile.get("first_name"));
                user.setLast_name((String) profile.get("last_name"));
                user.setProfile_pic((String) profile.get("profile_pic"));
                user.setLocale((String) profile.get("locale"));
                user.setGender((String) profile.get("gender"));
                try {
                    user.setTimezone((Integer) profile.get("timezone"));
                } catch (Exception e) {
                    // This one does not exist
                    user.setTimezone(0);
                }
            }
            */
            dm.saveUser(user);
            new_user = true;
        }
        List <String> faves = user.getFavesList();

        if (human.equalsIgnoreCase("stop")) {
            user.setStopped(1);
            dm.saveUser(user);
            context.remove("search");
            return "I have stopped your news delivery. You can still get dW news by initiating a conversation with me.";
        }

        if (human.equalsIgnoreCase("resume")) {
            user.setStopped(0);
            dm.saveUser(user);
            context.remove("search");
            return "I have resumed your news delivery.";
        }

        if (human.equalsIgnoreCase("RANDOM-TOPICS")) {
            faves = new ArrayList<String>();
            List<String> allfaves = Arrays.asList("big data", "bluemix", "bpm", "commerce", "cognitive", "iot", "java", "linux", "mobile", "open source", "security", "SOA", "web", "XML", "cloud");
            Random rand = new Random ();
            for (int i = 0; i < 3; i++) {
                int index = rand.nextInt(allfaves.size());
                faves.add(allfaves.get(index));
                allfaves.remove(index);
            }

            user.setFaves(String.join(",", faves));
            dm.saveUser(user);

            try {
                return createButtons(
                    "Great, I selected the following topics for you: \"" + user.getFaves() + "\".",
                    new HashMap<String, String>(){{
                        put("I want to change", "TOPICS");
                        put("Great, show me!", "NEWS");
                    }}
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            /*
            try {
                JSONObject payload = new JSONObject();
                payload.put("template_type", "button");
                payload.put("text", "Great, I selected the following topics for you: \"" + user.getFaves() + "\".");
                String[] button_titles = {"I want to change", "Great, show me!"};
                String[] button_payloads = {"TOPICS", "NEWS"};
                payload.put("buttons", createButtons(button_titles, button_payloads));
                return payload;
            } catch (Exception e) {
                e.printStackTrace();
            }
            */
        }

        if (context.get("search") != null) {
            List <NewsItem> items = dm.searchNewsItems(human);
            context.put("items", items);
            context.remove("search");

            List replies = new ArrayList();
            replies.add ("Search results for: " + human);
            replies.add(dm.replySlackItems(items, true));
            return replies;
        }

        // Pattern ptn = Pattern.compile("((change)|(update)|(modify)).*topic", Pattern.CASE_INSENSITIVE);
        // Matcher match = ptn.matcher(human);
        // boolean change_faves = match.find();
        boolean change_faves = human.equalsIgnoreCase("topics");

        if (faves == null || faves.isEmpty() || change_faves || new_user) {
            if (new_user) {
                // New user
                List replies = new ArrayList();
                replies.add("Hello " + user.getFirst_name() + "! This bot delivers developer news and articles from IBM developerWorks to you!");
                replies.add("To start, please reply with your interests in technical topics. Example:");
                replies.add("big data, bluemix, bpm, commerce, cognitive, IBM i, iot, java, linux, mobile, open source, security, service management, SOA, web, XML, cloud, industries");
                return replies;

            } else if (change_faves) {
                // The human input asks for changing faves
                user.setFaves("");
                dm.saveUser(user);

                List replies = new ArrayList();
                replies.add("Pls reply with your interests in technical topics. Example:");
                replies.add("big data, bluemix, bpm, commerce, cognitive, IBM i, iot, java, linux, mobile, open source, security, service management, SOA, web, XML, cloud, industries");
                return replies;

            } else {
                // The human is no longer asking for changing faves. He is giving his faves
                Set <String> faveset = new HashSet <String> ();
                Iterator it = dm.feeds.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry <String, String[]> pair = (Map.Entry)it.next();
                    System.out.println(pair.getKey() + " = " + pair.getValue());

                    Pattern ptn2 = Pattern.compile(pair.getValue()[0], Pattern.CASE_INSENSITIVE);
                    Matcher match2 = ptn2.matcher(human);
                    if (match2.find()) {
                        faveset.add(pair.getKey());
                    }
                }
                /*
                String[] elements = human.split(",|;");
                for (String e : elements) {
                    Iterator it = dm.feeds.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry <String, String[]> pair = (Map.Entry)it.next();
                        System.out.println(pair.getKey() + " = " + pair.getValue());

                        Pattern ptn2 = Pattern.compile(pair.getValue()[0], Pattern.CASE_INSENSITIVE);
                        Matcher match2 = ptn2.matcher(e);
                        if (match2.find()) {
                            faveset.add(pair.getKey());
                            break;
                        }
                    }
                }
                */
                faves = new ArrayList <String> (faveset);

                if (faves.isEmpty()) {
                    List replies = new ArrayList();
                    replies.add("Sorry, I do not see any available technical topic.");
                    replies.add("Pls reply with your interests in technical topics. Example:");
                    replies.add("big data, bluemix, bpm, commerce, cognitive, IBM i, iot, java, linux, mobile, open source, security, service management, SOA, web, XML, cloud, industries");
                    try {
                        replies.add(createButtons(
                                "Or, alternatively, you can let me randomly pick 3 topics for you!",
                                new HashMap<String, String>(){{
                                    put("Choose random", "RANDOM-TOPICS");
                                }}
                                // new String[] {"Choose random"},
                                // new String[] {"RANDOM-TOPICS"}
                        ));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return replies;

                } else {
                    user.setFaves(String.join(",", faves));
                    dm.saveUser(user);
                    try {
                        return createButtons(
                                "Great, your topics are \"" + user.getFaves() + "\".",
                                new HashMap<String, String>() {{
                                    put("Oh no", "TOPICS");
                                    put("Okay!", "NEWS");
                                }}
                                // new String[] {"I made a mistake", "This is correct!"},
                                // new String[] {"TOPICS", "NEWS"}
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        System.out.println("CHECK GET TOPIC");
        if (human.startsWith("GET-TOPIC-")) {
            String topic = human.substring(10);
            List <NewsItem> items = dm.getNewsItems(topic);
            context.put("items", items);

            List replies = new ArrayList();
            replies.add("Latest articles from " + topic);
            replies.add(dm.replySlackItems(items, false));
            return replies;
        }

        System.out.println("CHECK GET SUMMARY");
        if (human.startsWith("GET-SUMMARY-")) {
            long sid = Long.parseLong(human.substring(12));
            final NewsItem ni = dm.getNewsItem(sid); // Needed this for access from inner class for the HashMap init

            List replies = new ArrayList();
            try {
                replies.add(createButtons(
                        ni.getTitle(),
                        new HashMap<String, String>(){{
                            // put("Read article", ni.getArticleUrl());
                            put(ni.getTopic(), "GET-TOPIC-" + ni.getTopic());
                            put("All my topics", "NEWS");
                        }}
                        // new String[] {"Read article", ni.getTopic(), "All my topics"},
                        // new String[] {ni.getArticleUrl(), "GET-TOPIC-" + ni.getTopic(), "NEWS"}
                ).put("title_link", ni.getArticleUrl()).put("text", ni.getSubtitle()));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return replies;
        }

        System.out.println("CHECK NEWS");
        if (human.equalsIgnoreCase("news")) {
            // SHOW FAVE TOPICS. We will always to blogs
            List <NewsItem> items = new ArrayList <NewsItem> ();
            if (!faves.contains("dW Blog")) {
                faves.add("dW Blog");
            }
            for (String fave : faves) {
                List <NewsItem> nis = dm.getNewsItems(fave);
                if (nis == null || nis.isEmpty()) {
                    continue;
                }
                // Do NOT add duplicates. We will just add each article once -- based on the first topic we found it.
                for (NewsItem ni : nis) {
                    boolean needToAdd = true;
                    for (NewsItem item : items) {
                        if (ni.getTitle().equals(item.getTitle())) {
                            needToAdd = false;
                            break;
                        }
                    }
                    if (needToAdd) {
                        items.add(ni);
                    }
                }
            }
            context.put("items", items);

            List replies = new ArrayList();
            replies.add ("Latest articles from topics you are interested in.");
            replies.add(dm.replySlackItems(items, true));
            return replies;
            // return showNews("Latest articles from topics you are interested in.", faves, dm);
        }

        System.out.println("CHECK NEXT ARTICLE");
        if (human.equals("NEXT-ARTICLE")) {
            List <NewsItem> items = (List <NewsItem>) context.get("items");
            if (items == null || items.isEmpty()) {
                try {
                    return createButtons(
                            "Sorry, No more articles.",
                            new HashMap<String, String>(){{
                                put("Search", "SEARCH");
                                put("Articles for me", "NEWS");
                            }}
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
                /*
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("template_type", "button");
                    payload.put("text", "Sorry, No more articles.");
                    String[] button_titles = {"Articles for me", "Search"};
                    String[] button_payloads = {"NEWS", "SEARCH"};
                    payload.put("buttons", createButtons(button_titles, button_payloads));
                    return payload;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                */
                return "Sorry, No more articles";

            } else {
                int limit = items.size();
                if (limit > 8) {
                    limit = 8;
                }
                for (int i = 0; i < limit; i++) {
                    items.remove(0);
                }
                context.put("items", items);

                if (items.isEmpty()) {
                    return "You have reached the end of the list!";
                } else {
                    List replies = new ArrayList();
                    replies.add("More articles:");
                    replies.add(dm.replySlackItems(items, false));
                    return replies;
                }
            }
        }

        System.out.println("CHECK NLP");
        // String cstr = classifyText(human);
        String cstr = "";
        JSONObject json = watsonConversation(human);
        if (json != null) {
            try {
                JSONArray intents = json.getJSONArray("intents");
                if (intents.getJSONObject(0).getDouble("confidence") > 0.5) {
                    cstr = intents.getJSONObject(0).getString("intent");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("DETECTED INTENT: " + cstr);

        if ("HELP".equalsIgnoreCase(cstr)) {
            return "Hello! Here are a couple of commands to get you started: NEWS to get latest articles. TOPICS to change your interested topics; STOP to stop news delivery; and RESUME to resume news delivery.";

        } else if ("HELLO".equalsIgnoreCase(cstr)) {
            try {
                return createButtons(
                        "Hello! It is great meeting you! Would you like to see",
                        new HashMap<String, String>(){{
                            put("Search", "SEARCH");
                            put("Articles for me", "NEWS");
                            put("Change my interests", "TOPICS");
                        }}
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            /*
            try {
                JSONObject payload = new JSONObject();
                payload.put("template_type", "button");
                payload.put("text", "Hello! It is great meeting you! Would you like to see");
                String[] button_titles = {"Articles for me", "Change my interests", "Search"};
                String[] button_payloads = {"NEWS", "TOPICS", "SEARCH"};
                payload.put("buttons", createButtons(button_titles, button_payloads));
                return payload;
            } catch (Exception e) {
                e.printStackTrace();
            }
            */
            // return showNews("Hello! It is great meeting you! Below are the latest articles based on your interests.", faves, dm);

        } else if ("TOPIC".equalsIgnoreCase(cstr)) {
            try {
                return createButtons(
                        "Are you sure you want to update your interested topics?",
                        new HashMap<String, String>(){{
                            put("Yes", "TOPICS");
                        }}
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            /*
            try {
                JSONObject payload = new JSONObject();
                payload.put("template_type", "button");
                payload.put("text", "Are you sure you want to update your interested topics?");
                String[] button_titles = {"Yes"};
                String[] button_payloads = {"TOPICS"};
                payload.put("buttons", createButtons(button_titles, button_payloads));
                return payload;
            } catch (Exception e) {
                e.printStackTrace();
            }
            */

        } else if ("STOP".equalsIgnoreCase(cstr)) {
            try {
                return createButtons(
                        "Are you sure you want to stop developerWorks news delivery to you?",
                        new HashMap<String, String>(){{
                            put("Yes", "STOP");
                        }}
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            /*
            try {
                JSONObject payload = new JSONObject();
                payload.put("template_type", "button");
                payload.put("text", "Are you sure you want to stop developerWorks news delivery to you?");
                String[] button_titles = {"Yes"};
                String[] button_payloads = {"STOP"};
                payload.put("buttons", createButtons(button_titles, button_payloads));
                return payload;
            } catch (Exception e) {
                e.printStackTrace();
            }
            */

        } else if ("RESUME".equalsIgnoreCase(cstr)) {
            try {
                return createButtons(
                        "Are you sure you want to resume developerWorks news delivery to you?",
                        new HashMap<String, String>(){{
                            put("Yes", "RESUME");
                        }}
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            /*
            try {
                JSONObject payload = new JSONObject();
                payload.put("template_type", "button");
                payload.put("text", "Are you sure you want to resume developerWorks news delivery to you?");
                String[] button_titles = {"Yes"};
                String[] button_payloads = {"RESUME"};
                payload.put("buttons", createButtons(button_titles, button_payloads));
                return payload;
            } catch (Exception e) {
                e.printStackTrace();
            }
            */

        } else if ("SEARCH".equalsIgnoreCase(cstr)) {
            context.put("search", true);
            return "Please enter your search query here. For example, you can enter \"Java Stream\"";

        } else if ("MORE".equalsIgnoreCase(cstr)) {
            try {
                return createButtons(
                        "Do you want to see more articles from the previous list?",
                        new HashMap<String, String>(){{
                            put("Yes", "NEXT-ARTICLE");
                            put("Search", "SEARCH");
                            put("Help", "HELP");
                        }}
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            /*
            try {
                JSONObject payload = new JSONObject();
                payload.put("template_type", "button");
                payload.put("text", "Do you want to see more articles from the previous list?");
                String[] button_titles = {"Yes", "Search", "Help"};
                String[] button_payloads = {"NEXT-ARTICLE", "SEARCH", "HELP"};
                payload.put("buttons", createButtons(button_titles, button_payloads));
                return payload;
            } catch (Exception e) {
                e.printStackTrace();
            }
            */

            /*
            String topic = (String) context.get("topic");
            if (topic == null) {
                return showNews("Latest articles from topics you are interested in.", faves, dm);
            } else {
                List replies = new ArrayList();
                replies.add("Latest articles from " + topic);
                replies.add(Util.replyItems(dm.getNewsItems(topic), false));
                return replies;
            }
            */
        } else {
            try {
                return createButtons(
                        "Sorry, I cannot understand you.",
                        new HashMap<String, String>(){{
                            put("Articles for me", "NEWS");
                            put("Search", "SEARCH");
                            put("Help me", "HELP");
                        }}
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            /*
            try {
                JSONObject payload = new JSONObject();
                payload.put("template_type", "button");
                payload.put("text", "Sorry, I cannot understand you.");
                String[] button_titles = {"Help me", "Articles for me", "Search"};
                String[] button_payloads = {"HELP", "NEWS", "SEARCH"};
                payload.put("buttons", createButtons(button_titles, button_payloads));
                return payload;
            } catch (Exception e) {
                e.printStackTrace();
            }
            */
            // return showNews("Sorry, I cannot understand you. Try HELP to see a menu of options. Below are the latest articles ", faves, dm);
        }

        return "";
    }


    public List showNews (String greeting, List <String> faves, DataManager dm) {
        // SHOW FAVE TOPICS. We will always to blogs
        List <NewsItem> items = new ArrayList <NewsItem> ();
        if (!faves.contains("dW Blog")) {
            faves.add("dW Blog");
        }
        for (String fave : faves) {
            items.addAll(dm.getNewsItems(fave));
        }
        List replies = new ArrayList();
        if (greeting.isEmpty()) {
            // Nothing
        } else {
            replies.add (greeting);
        }
        replies.add(dm.replySlackItems(items, true));
        return replies;
    }

}