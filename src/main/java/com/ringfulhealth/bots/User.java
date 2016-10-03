package com.ringfulhealth.bots;

import com.ringfulhealth.bots.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

@Entity
@Table(name="users")
public class User implements Serializable {

    private long id;
    private Date createDate;
    private Date updateDate;

    private String fbId;
    private String slackId;
    private String slackTeamId;
    private String slackChannel;
    private String slackToken;
    private String email;

    private String first_name;
    private String last_name;
    private String profile_pic;
    private String locale;
    private int timezone;
    private String gender;

    private String faves;
    private int stopped;

    public User () {
        updateDate = new Date ();
        createDate = new Date ();

        fbId = "";
        slackId = "";
        slackTeamId = "";
        slackChannel = "";
        slackToken = "";
        email = "";

        first_name = "";
        last_name = "";
        profile_pic = "";
        locale = "";
        timezone = 0;
        gender = "";

        faves = "";
        stopped = 0;
    }

    @Id @GeneratedValue
    public long getId() { return id;}
    public void setId(long id) { this.id = id; }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreateDate() {
        return createDate;
    }
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getUpdateDate() {
        return updateDate;
    }
    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getFbId() {
        return fbId;
    }
    public void setFbId(String fbId) {
        this.fbId = fbId;
    }

    public String getSlackId() {
        return slackId;
    }
    public void setSlackId(String slackId) {
        this.slackId = slackId;
    }

    public String getSlackTeamId() {
        return slackTeamId;
    }
    public void setSlackTeamId(String slackTeamId) {
        this.slackTeamId = slackTeamId;
    }

    public String getSlackChannel() {
        return slackChannel;
    }
    public void setSlackChannel(String slackChannel) {
        this.slackChannel = slackChannel;
    }

    public String getSlackToken() {
        return slackToken;
    }
    public void setSlackToken(String slackToken) {
        this.slackToken = slackToken;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirst_name() {
        return first_name;
    }
    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }
    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getProfile_pic() {
        return profile_pic;
    }
    public void setProfile_pic(String profile_pic) {
        this.profile_pic = profile_pic;
    }

    public String getLocale() {
        return locale;
    }
    public void setLocale(String locale) {
        this.locale = locale;
    }

    public int getTimezone() {
        return timezone;
    }
    public void setTimezone(int timezone) {
        this.timezone = timezone;
    }

    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getFaves() {
        return faves;
    }
    public void setFaves(String faves) {
        this.faves = faves;
    }

    public int getStopped() {
        return stopped;
    }
    public void setStopped(int stopped) {
        this.stopped = stopped;
    }

    @Transient
    public String getUpdateDateStr() {
        return Util.formatDateTime(updateDate);
    }

    @Transient
    public List <String> getFavesList () {
        String [] fsa = faves.split(",|;");
        List <String> res = new ArrayList <String> ();
        for (String fs : fsa) {
            if (!fs.trim().isEmpty()) {
                res.add(fs.trim());
            }
        }
        return res;
    }
}


