package com.ringfulhealth.bots;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.quartz.*;
import org.quartz.ee.servlet.QuartzInitializerServlet;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;
import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class StartupServlet extends GenericServlet {

    @PersistenceUnit
    private EntityManagerFactory emf;
    
    private static final Logger log = Logger.getLogger(StartupServlet.class.getName());
    // http://www.quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/crontrigger.html
    private static final String CRON_EXPRESSION_1 = "0 5 0/1 * * ?"; // 5 min past every hour
    private static final String CRON_EXPRESSION_2 = "0 0 16 ? * MON,WED,FRI"; // 4pm (GMT) on specific week days
    // private static final String CRON_EXPRESSION_2 = "0 50 0/1 * * ?"; // 50 min past every hour

    public void init(ServletConfig servletConfig) throws ServletException {

        super.init(servletConfig);

        try {
            Properties props = new Properties();
            props.load(getServletContext().getResourceAsStream("/WEB-INF/classes/META-INF/conf.properties"));
            
            // This is for Tomcat
            if (emf == null) {
                emf = Persistence.createEntityManagerFactory(props.getProperty("persistenceUnit"));
                getServletContext().setAttribute("emf", emf);
            }

            SchedulerFactory schedulerFactory = (SchedulerFactory) getServletContext().getAttribute(QuartzInitializerServlet.QUARTZ_FACTORY_KEY);
            Scheduler scheduler = schedulerFactory.getScheduler();

            // The news fetch job

            JobDetail jobDetail1 = new JobDetail("MyJob1", "MyJobGroup", FetchAtomWorker.class);
            JobDataMap dataMap1 = new JobDataMap ();
            dataMap1.put("emf", emf);
            dataMap1.put("scontext", getServletContext());
            jobDetail1.setJobDataMap (dataMap1);

            CronTrigger cronTrigger1 = new CronTrigger("MyTrigger1", "MyTriggerGroup");
            CronExpression cexp1 = new CronExpression(CRON_EXPRESSION_1);
            cronTrigger1.setCronExpression(cexp1);

            scheduler.scheduleJob(jobDetail1, cronTrigger1);

            // The daily news job for Facebook
            JobDetail jobDetail2 = new JobDetail("MyJob2", "MyJobGroup", com.ringfulhealth.bots.facebook.SendNewsWorker.class);
            JobDataMap dataMap2 = new JobDataMap ();
            dataMap2.put("emf", emf);
            dataMap2.put("scontext", getServletContext());
            jobDetail2.setJobDataMap (dataMap2);

            CronTrigger cronTrigger2 = new CronTrigger("MyTrigger2", "MyTriggerGroup");
            CronExpression cexp2 = new CronExpression(CRON_EXPRESSION_2);
            cronTrigger2.setCronExpression(cexp2);

            scheduler.scheduleJob(jobDetail2, cronTrigger2);

            // The daily news job for Slack
            JobDetail jobDetail3 = new JobDetail("MyJob3", "MyJobGroup", com.ringfulhealth.bots.slack.SendNewsWorker.class);
            JobDataMap dataMap3 = new JobDataMap ();
            dataMap3.put("emf", emf);
            dataMap3.put("scontext", getServletContext());
            jobDetail3.setJobDataMap (dataMap3);

            CronTrigger cronTrigger3 = new CronTrigger("MyTrigger3", "MyTriggerGroup");
            CronExpression cexp3 = new CronExpression(CRON_EXPRESSION_2);
            cronTrigger3.setCronExpression(cexp3);

            scheduler.scheduleJob(jobDetail3, cronTrigger3);

            // always rebuild the search index
            FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(emf.createEntityManager());
            fullTextEntityManager.createIndexer().startAndWait();

            // Instantiate NewsServlet so that it sets up all static variables for the BaseServlet, which is used by the worker thread
            com.ringfulhealth.bots.facebook.NewsServlet ns1 = new com.ringfulhealth.bots.facebook.NewsServlet();
            com.ringfulhealth.bots.slack.NewsServlet ns2 = new com.ringfulhealth.bots.slack.NewsServlet();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void service(ServletRequest serveletRequest, ServletResponse servletResponse) throws ServletException, IOException {
    }

}
