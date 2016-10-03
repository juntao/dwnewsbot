package com.ringfulhealth.bots;


import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class FetchAtomWorker implements Job {

    private static final Logger log = Logger.getLogger(FetchAtomWorker.class.getName());

    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Start FetchAtomWorker job");

        EntityManagerFactory emf = (EntityManagerFactory) context.getMergedJobDataMap().get("emf");
        DataManager dm = new DataManager (emf);
        ServletContext scontext = (ServletContext) context.getMergedJobDataMap().get("scontext");

        Iterator it = dm.feeds.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry <String, String[]> pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());

            try {
                URL feedUrl = new URL(pair.getValue()[1]);

                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(feedUrl));
                for (SyndEntry entry : (List<SyndEntry>) feed.getEntries()) {
                    dm.saveNewsItem(entry, pair.getKey());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}
