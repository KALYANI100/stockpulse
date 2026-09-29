package com.example.stockpulse.repository;

import com.example.stockpulse.domain.ReorderSuggestion;
import com.example.stockpulse.domain.SuggestionStatus;
import com.example.stockpulse.domain.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, UUID> {
    boolean existsByProductIdAndTriggerReasonAndStatus(UUID productId, TriggerReason triggerReason,
                                                       SuggestionStatus status);
    boolean existsByProductIdAndStatus(UUID productId, SuggestionStatus status);
    List<ReorderSuggestion> findAllByStatusOrderByCreatedAtDesc(SuggestionStatus status);
}
