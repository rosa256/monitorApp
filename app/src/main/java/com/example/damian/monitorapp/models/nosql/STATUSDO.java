package com.example.damian.monitorapp.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@DynamoDBTable(tableName = "monitorapp-mobilehub-2069127866-STATUS")

public class STATUSDO {
    private String _userId;
    private String _fullDate;
    private String _confidence;
    private String _date;
    private String _hour;
    private Boolean _verified;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBAttribute(attributeName = "userId")
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }
    @DynamoDBRangeKey(attributeName = "full_date")
    @DynamoDBAttribute(attributeName = "full_date")
    public String getFullDate() {
        return _fullDate;
    }

    public void setFullDate(final String _fullDate) {
        this._fullDate = _fullDate;
    }
    @DynamoDBAttribute(attributeName = "confidence")
    public String getConfidence() {
        return _confidence;
    }

    public void setConfidence(final String _confidence) {
        this._confidence = _confidence;
    }
    @DynamoDBAttribute(attributeName = "date")
    public String getDate() {
        return _date;
    }

    public void setDate(final String _date) {
        this._date = _date;
    }
    @DynamoDBAttribute(attributeName = "hour")
    public String getHour() {
        return _hour;
    }

    public void setHour(final String _hour) {
        this._hour = _hour;
    }
    @DynamoDBAttribute(attributeName = "verified")
    public Boolean getVerified() {
        return _verified;
    }

    public void setVerified(final Boolean _verified) {
        this._verified = _verified;
    }

}