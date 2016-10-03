package com.ringfulhealth.bots;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

@Entity
@Indexed
@Table(name="newsitems")
public class NewsItem implements Serializable, Comparable <NewsItem> {

    private long id;
    private Date updateDate;
    private Date saveDate;

    private String topic;
    private String title;
    private String subtitle;
    private String article;
    private String imageUrl;
    private String articleUrl;

    public NewsItem () {
        updateDate = new Date ();
        saveDate = new Date ();

        topic = "";
        title = "";
        subtitle = "";
        article = "";
        imageUrl = "";
        articleUrl = "";
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

    @Temporal(TemporalType.TIMESTAMP)
    public Date getSaveDate() {
        return saveDate;
    }
    public void setSaveDate(Date saveDate) {
        this.saveDate = saveDate;
    }

    public String getTopic() {
        return topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Field
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    @Field
    @Column(length=4096)
    public String getSubtitle() {
        return subtitle;
    }
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    @Field
    @Column(columnDefinition = "TEXT")
    public String getArticle() {
        return article;
    }
    public void setArticle(String article) {
        this.article = article;
    }

    @Column(length=512)
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Column(length=512)
    public String getArticleUrl() {
        return articleUrl;
    }
    public void setArticleUrl(String articleUrl) {
        this.articleUrl = articleUrl;
    }

    @Transient
    public String getUpdateDateStr() {
        return Util.formatDateTime(updateDate);
    }

    @Transient
    public String getSaveDateStr() {
        return Util.formatDateTime(saveDate);
    }

    @Transient
    public int compareTo (NewsItem ni) {
        if (updateDate.compareTo(ni.getUpdateDate()) == 0) {
            return title.compareTo(ni.getTitle());
        }
        return (-1) * updateDate.compareTo(ni.getUpdateDate());
        // return (int) (updateDate.getTime() - ni.getUpdateDate().getTime());
    }
}