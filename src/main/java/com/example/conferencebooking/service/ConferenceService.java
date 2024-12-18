package com.example.conferencebooking.service;

import com.example.conferencebooking.entity.Conference;
import com.example.conferencebooking.dto.ConferenceSearchDTO;
import java.util.List;

public interface ConferenceService {
    Conference createConference(Conference conference);
    Conference getConference(String id);
    List<Conference> getAllConferences();
    void deleteConference(String id);
    List<Conference> searchConferences(ConferenceSearchDTO searchDTO);
    List<Conference> getSuggestedConferences(String userId);
    List<Conference> getUpcomingConferences();
} 