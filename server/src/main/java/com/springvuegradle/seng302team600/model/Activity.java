package com.springvuegradle.seng302team600.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.*;

@Entity
public class Activity {

    final static private int NAME_LEN = 75;
    final static private int DESCRIPTION_LEN = 1500;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id", nullable = false)
    @JsonProperty("id")
    private Long activityId;

    // This could be a way to link to the creator of this activity...
    private Long creatorUserId;

    @NotNull(message = "This Activity needs a name")
    @Column(name = "activity_name", length = NAME_LEN, nullable = false)
    @JsonProperty("activity_name")
    private String name;

    @Column(name = "description", length = DESCRIPTION_LEN)
    @JsonProperty("description")
    private String description;

    @NotNull(message = "This Activity needs one or more ActivityTypes associated with it")
    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})  // ALL except REMOVE
    @JoinTable(
            name = "activity_activity_type",
            joinColumns = @JoinColumn(name = "activity_id"),
            inverseJoinColumns = @JoinColumn(name = "activity_type_id"))
    @JsonProperty("activity_type")
    private Set<ActivityType> activityTypes;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})  // ALL except REMOVE
    @JoinTable(
            name = "activity_participant",
            joinColumns = @JoinColumn(name = "activity_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> participants = new HashSet<>();

    @NotNull(message = "This Activity needs to be either continuous or have a duration")
    @Column(name = "is_continuous", columnDefinition = "boolean", nullable = false)
    @JsonProperty("continuous")
    private boolean continuous;

    @Column(name = "start_time", columnDefinition = "DATETIME")
    // See here for format pattern: https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssZ")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonProperty("start_time")
    private Date startTime;

    @Column(name = "end_time", columnDefinition = "DATETIME")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssZ")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonProperty("end_time")
    private Date endTime;

    // ToDO These tags need to be modified in U9 (see  api-minimal-spec2.1.txt)
    @NotNull(message = "This Activity needs a location")
    @Column(name = "location", length = NAME_LEN, nullable = false)
    @JsonProperty("location")
    private String location;   // ToDo change String to Location in U9


    /**
     * Default constructor for Activity.
     * Mandatory for repository actions?
     */
    public Activity() {}

    public Long getActivityId() {
        return activityId;
    }

    public Long getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(Long creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<ActivityType> getActivityTypes() {
        return activityTypes;
    }

    public void setActivityTypes(Set<ActivityType> activityTypes) {
        this.activityTypes = activityTypes;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public Set<User> getParticipants() {return participants;}

    public void addParticipant(User user) { this.participants.add(user); }

    public void removeParticipant(User user) { this.participants.remove(user); }

    public void setParticipants(Set<User> participants) {this.participants = participants;}

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Return a unique hash based on attributes.
     * @return hash code
     */
    @Override
    public int hashCode() {
        return activityId.hashCode();
    }

    /**
     * Compare Activities based on attributes
     * @param obj other Activity to campare
     * @return equality
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Activity) {
            final Activity other = (Activity) obj;
            // Check that all attributes are equal, except activityId and creatorUserId
            return this.getActivityId().equals(other.getActivityId());
        }
        return false;
    }

    @Override
    public String toString() {
        return "Activity{" +
                "activityId=" + activityId +
                ", creatorUserId=" + creatorUserId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", activityTypes=" + activityTypes +
                ", continuous=" + continuous +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", location='" + location + '\'' +
                ", participants='" + participants + '\'' +
                '}';
    }
}