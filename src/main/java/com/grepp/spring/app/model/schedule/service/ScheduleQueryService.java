package com.grepp.spring.app.model.schedule.service;

import com.grepp.spring.app.controller.api.schedule.payload.response.ShowScheduleResponse;
import com.grepp.spring.app.controller.api.schedule.payload.response.ShowSuggestedLocationsResponse;
import com.grepp.spring.app.controller.api.schedule.payload.response.ShowVoteMembersResponse;
import com.grepp.spring.app.model.event.code.MeetingType;
import com.grepp.spring.app.model.event.entity.Event;
import com.grepp.spring.app.model.event.repository.EventRepository;
import com.grepp.spring.app.model.schedule.dto.MetroInfoDto;
import com.grepp.spring.app.model.schedule.dto.ShowScheduleDto;
import com.grepp.spring.app.model.schedule.dto.ShowSuggestedLocationsDto;
import com.grepp.spring.app.model.schedule.dto.VoteMemberDto;
import com.grepp.spring.app.model.schedule.entity.Location;
import com.grepp.spring.app.model.schedule.entity.MetroTransfer;
import com.grepp.spring.app.model.schedule.entity.Schedule;
import com.grepp.spring.app.model.schedule.entity.ScheduleMember;
import com.grepp.spring.app.model.schedule.entity.Vote;
import com.grepp.spring.app.model.schedule.entity.Workspace;
import com.grepp.spring.app.model.schedule.repository.LocationQueryRepository;
import com.grepp.spring.app.model.schedule.repository.MetroTransferQueryRepository;
import com.grepp.spring.app.model.schedule.repository.ScheduleMemberQueryRepository;
import com.grepp.spring.app.model.schedule.repository.ScheduleQueryRepository;
import com.grepp.spring.app.model.schedule.repository.VoteQueryRepository;
import com.grepp.spring.app.model.schedule.repository.WorkspaceQueryRepository;
import com.grepp.spring.infra.error.exceptions.event.EventNotFoundException;
import com.grepp.spring.infra.error.exceptions.group.ScheduleNotFoundException;
import com.grepp.spring.infra.error.exceptions.schedule.LocationNotFoundException;
import com.grepp.spring.infra.error.exceptions.schedule.NotScheduleMemberException;
import com.grepp.spring.infra.error.exceptions.schedule.ScheduleMemberNotFoundException;
import com.grepp.spring.infra.error.exceptions.schedule.WorkSpaceNotFoundException;
import com.grepp.spring.infra.response.EventErrorCode;
import com.grepp.spring.infra.response.GroupErrorCode;
import com.grepp.spring.infra.response.ScheduleErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleQueryService {

    private final ScheduleQueryRepository scheduleQueryRepository;
    private final ScheduleMemberQueryRepository scheduleMemberQueryRepository;
    private final WorkspaceQueryRepository workspaceQueryRepository;
    private final EventRepository eventRepository;
    private final LocationQueryRepository locationQueryRepository;
    private final VoteQueryRepository voteQueryRepository;
    private final MetroTransferQueryRepository metroTransferQueryRepository;

    @Transactional(readOnly = true)
    public ShowScheduleResponse showSchedule(Schedule schedule, String userId) {

        Long eventId = schedule.getEvent().getId();

        List<ScheduleMember> scheduleMembers = scheduleMemberQueryRepository.findByScheduleId(
                schedule.getId());
        Boolean bool = false;
        for (ScheduleMember scheduleMember : scheduleMembers) {
            if (scheduleMember.getMember().getId().equals(userId)) {
                bool = true;
            }
        }

        if (bool) {
            List<Workspace> workspaces = workspaceQueryRepository.findAllByScheduleId(schedule.getId());

            MeetingType meetingType = eventRepository.findById(eventId).get().getMeetingType();
            List<Long> voteLocationId = voteQueryRepository.findByScheduleIdAndMemberId(schedule.getId());

            ShowScheduleDto dto = ShowScheduleDto.fromEntity(meetingType, eventId, schedule,
                    scheduleMembers, workspaces, voteLocationId);

            return ShowScheduleDto.fromDto(dto);
        }

        throw new NotScheduleMemberException(ScheduleErrorCode.NOT_SCHEDULE_MEMBER);
    }

    @Transactional(readOnly = true)
    public ShowSuggestedLocationsResponse showSuggestedLocation(Long scheduleId) {
        List<MetroInfoDto> infoDto = getMetroInfoDtos(scheduleId);
        int scheduleMemberNumber = scheduleMemberQueryRepository.findByScheduleId(scheduleId).size();
        int departLocationCount = getDepartLocationCount(scheduleId);
        int voteCount = voteQueryRepository.findByScheduleId(scheduleId).size();

        ShowSuggestedLocationsDto finalDto = ShowSuggestedLocationsDto.fromMetroInfoDto(infoDto,
                scheduleMemberNumber, voteCount, departLocationCount);

        return ShowSuggestedLocationsDto.fromDto(finalDto);
    }

    private List<MetroInfoDto> getMetroInfoDtos(Long scheduleId) {
        List<Location> locations = locationQueryRepository.findByScheduleId(scheduleId);
        if (locations.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> locationIds = locations.stream().map(Location::getId).toList();
        List<MetroTransfer> allTransfers = metroTransferQueryRepository.findByLocationIdIn(locationIds);

        // Group transfers by locationId
        java.util.Map<Long, List<MetroTransfer>> transfersByLocation = allTransfers.stream()
                .collect(java.util.stream.Collectors.groupingBy(mt -> mt.getLocation().getId()));

        List<MetroInfoDto> infoDto = new ArrayList<>();
        for (Location l : locations) {
            List<MetroTransfer> transferForLocation = transfersByLocation.getOrDefault(l.getId(), new ArrayList<>());
            infoDto.add(MetroInfoDto.toDto(l, transferForLocation));
        }
        return infoDto;
    }

    private int getDepartLocationCount(Long scheduleId) {
        List<ScheduleMember> scheduleMembers = scheduleMemberQueryRepository.findByScheduleId(scheduleId);
        int departLocationCount = 0;
        for (ScheduleMember scheduleMember : scheduleMembers) {
            if (scheduleMember.getLatitude() != null) {
                departLocationCount++;
            }
        }
        return departLocationCount;
    }

    @Transactional(readOnly = true)
    public ShowVoteMembersResponse findVoteMembers(Long scheduleId) {

        List<VoteMemberDto> voteMemberList = getVoteMemberDtos(scheduleId);
        ShowVoteMembersResponse response = VoteMemberDto.fromDto(voteMemberList);

        return response;
    }

    private List<VoteMemberDto> getVoteMemberDtos(Long scheduleId) {
        List<ScheduleMember> scheduleMembers = scheduleMemberQueryRepository.findByScheduleId(scheduleId);
        if (scheduleMembers.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> memberIds = scheduleMembers.stream().map(ScheduleMember::getId).toList();
        List<Vote> allVotes = voteQueryRepository.findByScheduleMemberIdIn(memberIds);

        // Create a set of ScheduleMember IDs that have voted
        java.util.Set<Long> votedMemberIds = allVotes.stream()
                .map(v -> v.getScheduleMember().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<VoteMemberDto> voteMemberList = new ArrayList<>();
        for (ScheduleMember sm : scheduleMembers) {
            if (votedMemberIds.contains(sm.getId())) {
                VoteMemberDto voteMemberDto = VoteMemberDto.toDto(sm.getMember());
                voteMemberList.add(voteMemberDto);
            }
        }
        return voteMemberList;
    }

    @Transactional(readOnly = true)
    public Schedule findScheduleById(Long scheduleId) {
        return scheduleQueryRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(
                        GroupErrorCode.SCHEDULE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Event findEventById(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(
                EventErrorCode.EVENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Location findLocationById(Long locationId) {
        return locationQueryRepository.findById(locationId)
                .orElseThrow(() -> new LocationNotFoundException(
                        ScheduleErrorCode.LOCATION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ScheduleMember findScheduleMemberById(Long scheduleMemberId) {
        return scheduleMemberQueryRepository.findById(scheduleMemberId)
                .orElseThrow(() -> new ScheduleMemberNotFoundException(
                        ScheduleErrorCode.LOCATION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Workspace findWorkspaceById(Long workspaceId) {
        return workspaceQueryRepository.findById(workspaceId).orElseThrow(
                () -> new WorkSpaceNotFoundException(ScheduleErrorCode.WORKSPACE_NOT_FOUND));
    }
}
