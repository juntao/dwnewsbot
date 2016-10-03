package com.ringfulhealth.bots;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    public static String randomNumericString (int dig) {
        StringBuffer buf = new StringBuffer ();
        Random generator = new Random();
        for (int i = 0; i < dig; i++) {
            buf.append (generator.nextInt(10));
        }
        return buf.toString ();
    }

    public static String getRandomId (int dig) {
        String charTable = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuffer buf = new StringBuffer ();
        Random rand = new Random ();
        for (int i = 0; i < dig; i++) {
            buf.append (charTable.charAt(rand.nextInt(61)));
        }
        return buf.toString ();
    }

    public static String formatPhoneNumber (String phone) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber number = phoneUtil.parse(phone, "US");
            if (!phoneUtil.isValidNumber(number)) {
                throw new Exception ("Phone number validation failed");
            }
            return phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (Exception e) {
            // e.printStackTrace ();
            System.out.print("Phone number validation failed for " + phone);
            return "";
        }
    }

    public static String getElapsedTime(Date created) {
        long duration = System.currentTimeMillis() - created.getTime();
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
        long days = TimeUnit.MILLISECONDS.toDays(duration);
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        if (days > 0) {
            return days + " days ago";
        }
        if (hours > 0) {
            return hours + " hrs ago";
        }
        if (minutes > 0) {
            return minutes + " minutes ago";
        }

        return seconds + " seconds ago";
    }

    public static String getFutureElapsedTime(Date future) {
        long duration = future.getTime() - System.currentTimeMillis();
        if (duration <= 0) {
            return "Now";
        }

        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
        long days = TimeUnit.MILLISECONDS.toDays(duration);
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        if (days > 0) {
            return "In " + days + " days";
        }
        if (hours > 0) {
            return "In " + hours + " hrs";
        }
        if (minutes > 0) {
            return "In " + minutes + " minutes";
        }

        return "In " + seconds + " seconds";
    }

    public static Date parseDate (String s) {
        try {
            DateFormat df = new SimpleDateFormat("M/d/yyyy");
            return df.parse (s);
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatDate (Date d) {
        if (d == null || d.getTime() == 0) {
            return "";
        } else {
            DateFormat df = new SimpleDateFormat ("MM/dd/yyyy");
            return df.format (d);
        }
    }

    public static String formatDateTime (Date d) {
        if (d == null || d.getTime() == 0) {
            return "";
        } else {
            DateFormat df = new SimpleDateFormat ("MM/dd/yyyy HH:mm");
            return df.format (d);
        }
    }

    public static String cleanUpFormData (String para) {
        if (para == null) {
            para = "";
        } else {
            para = para.trim();
        }
        return para;
    }


    public static String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    public static Date laterDate (Date d1, Date d2) {
        if (d1 == null && d2 == null) {
            return null;
        } else if (d1 != null && d2 == null) {
            return d1;
        } else if (d1 == null && d2 != null) {
            return d2;
        } else {
            if (d1.getTime() > d2.getTime()) {
                return d1;
            } else {
                return d2;
            }
        }
    }

    public static boolean isDateNull (Date d) {
        if (d == null) {
            return true;
        } else if (d.getTime() == 0l) {
            return true;
        } else {
            return false;
        }

    }

    public static String cleanUp (String s) {
        if (s == null) {
            return "";
        }
        // s = s.trim();
        // s = s.replaceAll("\\s+", " ");
        return Jsoup.parse(s.trim()).text();
    }
}