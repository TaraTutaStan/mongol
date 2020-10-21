package com.iri.backend.tools.chengeSet;

import org.bson.Document;

import java.util.Date;

public class ChangeEntry {
    public static final String KEY_CHANGEID = "changeId";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_CHANGELOGCLASS = "changeLogClass";
    public static final String KEY_CHANGESETMETHOD = "changeSetMethod";

    private String changeSetId;
    private String author;
    private Date timestamp;
    private String changeLogClass;
    private String changeSetMethodName;

    public ChangeEntry(String changeSetId, String author, Date timestamp, String changeLogClass, String changeSetMethodName) {
        this.changeSetId = changeSetId;
        this.author = author;
        this.timestamp = new Date(timestamp.getTime());
        this.changeLogClass = changeLogClass;
        this.changeSetMethodName = changeSetMethodName;
    }

    public Document toFullDocument() {
        Document entry = new Document();

        entry.append(KEY_CHANGEID, this.changeSetId)
                .append(KEY_AUTHOR, this.author)
                .append(KEY_TIMESTAMP, this.timestamp)
                .append(KEY_CHANGELOGCLASS, this.changeLogClass)
                .append(KEY_CHANGESETMETHOD, this.changeSetMethodName);

        return entry;
    }

    public Document toSearchDocument() {
        return new Document()
                .append(KEY_CHANGEID, this.changeSetId)
                .append(KEY_AUTHOR, this.author);
    }

    @Override
    public String toString() {
        return "ChangeEntry{" +
                "changeSetId='" + changeSetId + '\'' +
                ", author='" + author + '\'' +
                ", timestamp=" + timestamp +
                ", changeLogClass='" + changeLogClass + '\'' +
                ", changeSetMethodName='" + changeSetMethodName + '\'' +
                '}';
    }

    public String getChangeSetId() {
        return this.changeSetId;
    }

    public String getAuthor() {
        return this.author;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public String getChangeLogClass() {
        return this.changeLogClass;
    }

    public String getChangeSetMethodName() {
        return this.changeSetMethodName;
    }
}
