package com.ringfulhealth.bots.facebook;


import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.ringfulhealth.bots.DataManager;
import com.ringfulhealth.bots.NewsItem;
import com.ringfulhealth.bots.User;
import com.ringfulhealth.chatbotbook.facebook.BaseServlet;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SendNewsWorker implements Job {

    private static final Logger log = Logger.getLogger(SendNewsWorker.class.getName());

    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Start SendNewsWorker job");

        EntityManagerFactory emf = (EntityManagerFactory) context.getMergedJobDataMap().get("emf");
        DataManager dm = new DataManager (emf);
        ServletContext scontext = (ServletContext) context.getMergedJobDataMap().get("scontext");

        List <User> users = dm.getActiveUsers();
        if (users == null || users.isEmpty()) {
            return;
        }

        for (User user : users) {
            log.info("User: " + user.getId());
            if (user.getFbId() == null || user.getFbId().isEmpty()) {
                continue;
            }

            List <String> faves = user.getFavesList();
            if (faves == null || faves.isEmpty()) {
                continue;
            }

            List <NewsItem> items = new ArrayList <NewsItem> ();
            if (!faves.contains("dW Blog")) {
                faves.add("dW Blog");
            }
            for (String fave : faves) {
                List <NewsItem> nis = dm.getNewsItems(fave);
                if (nis == null || nis.isEmpty()) {
                    continue;
                }
                for (NewsItem ni : nis) {
                    // Must have not seen
                    if (ni.getSaveDate().getTime() > user.getUpdateDate().getTime()) {
                        // We will not add cross topic duplicates
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
            }

            log.info("Items: " + items.size());

            if (items.isEmpty()) {
                continue; // there is no update for today
            }

            // send the replies to the user proactively
            try {
                NewsServlet ns = new NewsServlet(); // This sets up the tokens and constants
                ns.sendReply("Hey, " + user.getFirst_name() + ", here are some articles you might have missed.", user.getFbId());
                ns.sendReply(dm.replyFbItems(items, true), user.getFbId());
                ns.sendReply("To stop future news delivery messages, please text STOP", user.getFbId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            user.setUpdateDate(new Date ());
            dm.saveUser(user);
        }
    }
}
