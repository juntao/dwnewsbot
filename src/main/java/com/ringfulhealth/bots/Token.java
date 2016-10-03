package com.ringfulhealth.bots;

import com.ringfulhealth.bots.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

@Entity
@Table(name="tokens")
public class Token implements Serializable {

    private long id;
    private Date updateDate;
    private String teamId;
    private String botToken;

    public Token () {
        updateDate = new Date ();
        teamId = "";
        botToken = "";
    }

    @Id @GeneratedValue
    public long getId() { return id;}
    public void setId(long id) { this.id = id; }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getUpdateDate() {
        return updateDate;
    }
    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getTeamId() {
        return teamId;
    }
    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getBotToken() {
        return botToken;
    }
    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }
}


