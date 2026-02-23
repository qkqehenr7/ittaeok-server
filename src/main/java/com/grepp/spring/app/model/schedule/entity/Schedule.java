package com.grepp.spring.app.model.schedule.entity;

import com.grepp.spring.app.model.event.code.MeetingType;
import com.grepp.spring.app.model.event.entity.Event;
import com.grepp.spring.app.model.schedule.code.MeetingPlatform;
import com.grepp.spring.app.model.schedule.code.ScheduleStatus;
import com.grepp.spring.app.model.schedule.dto.ModifyScheduleDto;
import com.grepp.spring.infra.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Schedules")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Schedule extends BaseEntity {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column()
    private LocalDateTime startTime;

    @Column()
    private LocalDateTime endTime;

    @Column()
    @Enumerated(EnumType.STRING)
    private ScheduleStatus status;

    @Column
    private String location;

    @Column
    private String scheduleName;

    @Column(columnDefinition = "text")
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private MeetingPlatform meetingPlatform;

    @Column(columnDefinition = "text")
    private String platformUrl;

    @Column
    private String specificLocation;

    @Column
    private Double specificLatitude;

    @Column
    private Double specificLongitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    // 일정이 삭제되면 ScheduleMember 도 같이 삭제
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleMember> scheduleMembers = new ArrayList<>();

    // 일정이 삭제되면 일정에 포함된 장소도 삭제
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Location> locations = new ArrayList<>();

    // 일정이 삭제되면 일정에 포함된 workspace 도 같이 삭제
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Workspace> workspaces = new ArrayList<>();

    // --- Domain Logic Methods ---

    public void updateDetails(LocalDateTime startTime, LocalDateTime endTime, ScheduleStatus status,
            String scheduleName, String description, String location,
            String specificLocation, Double specificLatitude, Double specificLongitude) {
        if (startTime != null)
            this.startTime = startTime;
        if (endTime != null)
            this.endTime = endTime;
        if (status != null)
            this.status = status;
        if (scheduleName != null)
            this.scheduleName = scheduleName;
        if (description != null)
            this.description = description;
        if (location != null)
            this.location = location;
        if (specificLocation != null)
            this.specificLocation = specificLocation;
        if (specificLatitude != null)
            this.specificLatitude = specificLatitude;
        if (specificLongitude != null)
            this.specificLongitude = specificLongitude;
    }

    public void updateMeetingPlatform(MeetingPlatform meetingPlatform, String platformUrl) {
        if (meetingPlatform != null)
            this.meetingPlatform = meetingPlatform;
        if (platformUrl != null)
            this.platformUrl = platformUrl;
    }

    public void determineLocation(String location) {
        this.location = location;
    }

}
