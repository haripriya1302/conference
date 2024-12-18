package com.example.conferencebooking.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;


@Entity
@Table(name = "conferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conference {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(name = "start_timestamp", nullable = false)
    private LocalDateTime startTimestamp;

    @Column(name = "end_timestamp", nullable = false)
    private LocalDateTime endTimestamp;

    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots;

    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots;

    @Column(name = "waitlist_queue_id")
    private String waitlistQueueId;

    @Column(name = "fully_booked")
    private boolean fullyBooked;

    @Version
    private Long version;

    // Storing topics as a JSON column
    @Builder.Default
    private List<String> topics = new ArrayList<>();

    @jakarta.persistence.Transient
    private boolean hasActiveBooking;

    @jakarta.persistence.Transient
    private BookingStatus bookingStatus;
}