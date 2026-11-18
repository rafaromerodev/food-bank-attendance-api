package dev.rafaromero.attendance.card;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beneficiaries/{beneficiaryId}/cards")
@Tag(name = "Cards")
class CardController {

    private final CardService service;

    CardController(CardService service) {
        this.service = service;
    }

    record AssignCardRequest(@NotBlank String uid) {
    }

    record ReplaceCardRequest(@NotBlank String uid, @NotNull DeactivationReason reason) {
    }

    record CardResponse(Long id, String uid, Instant assignedAt, Instant deactivatedAt,
            DeactivationReason deactivationReason, boolean active) {

        static CardResponse of(Card card) {
            return new CardResponse(card.getId(), card.getUid(), card.getAssignedAt(), card.getDeactivatedAt(),
                    card.getDeactivationReason(), card.isActive());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign the first card to a beneficiary")
    CardResponse assign(@PathVariable long beneficiaryId, @Valid @RequestBody AssignCardRequest request) {
        return CardResponse.of(service.assign(beneficiaryId, request.uid()));
    }

    @PostMapping("/replacement")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Replace a lost, stolen or damaged card",
            description = "The old card is deactivated and kept; the attendance history is not affected.")
    CardResponse replace(@PathVariable long beneficiaryId, @Valid @RequestBody ReplaceCardRequest request) {
        return CardResponse.of(service.replace(beneficiaryId, request.uid(), request.reason()));
    }

    @GetMapping
    @Operation(summary = "Card history, newest first")
    List<CardResponse> history(@PathVariable long beneficiaryId) {
        return service.history(beneficiaryId).stream().map(CardResponse::of).toList();
    }
}
