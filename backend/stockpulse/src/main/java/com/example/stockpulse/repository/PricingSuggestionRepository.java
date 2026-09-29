package com.example.stockpulse.repository;

import com.example.stockpulse.domain.PricingSuggestion;
import com.example.stockpulse.domain.SuggestionStatus;
import com.example.stockpulse.domain.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, UUID> {
    boolean existsByProductIdAndTriggerReasonAndStatus(UUID productId, TriggerReason triggerReason,
                                                       SuggestionStatus status);
    boolean existsByProductIdAndStatus(UUID productId, SuggestionStatus status);
    List<PricingSuggestion> findAllByStatusOrderByCreatedAtDesc(SuggestionStatus status);
}
