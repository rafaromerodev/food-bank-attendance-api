package dev.rafaromero.attendance.beneficiary;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beneficiaries")
@Tag(name = "Beneficiaries")
class BeneficiaryController {

    private final BeneficiaryService service;

    BeneficiaryController(BeneficiaryService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Register a beneficiary",
            description = "Rejects a DNI/NIE with a wrong check letter and suggests the correct one.")
    ResponseEntity<BeneficiaryResponse> create(@Valid @RequestBody BeneficiaryRequest request) {
        Beneficiary created = service.create(request);
        return ResponseEntity.created(URI.create("/api/beneficiaries/" + created.getId()))
                .body(BeneficiaryResponse.of(created, service.today()));
    }

    @GetMapping("/{id}")
    BeneficiaryResponse get(@PathVariable long id) {
        return BeneficiaryResponse.of(service.get(id), service.today());
    }

    @GetMapping
    @Operation(summary = "Search by name, document or file number")
    PagedModel<BeneficiaryResponse> search(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "true") boolean active,
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        return new PagedModel<>(service.search(query, active, pageable)
                .map(b -> BeneficiaryResponse.of(b, service.today())));
    }

    @PutMapping("/{id}")
    BeneficiaryResponse update(@PathVariable long id, @Valid @RequestBody BeneficiaryRequest request) {
        return BeneficiaryResponse.of(service.update(id, request), service.today());
    }

    @PostMapping("/{id}/renewal")
    @Operation(summary = "Renew the file for 6 or 12 months")
    BeneficiaryResponse renew(@PathVariable long id, @RequestParam int months) {
        return BeneficiaryResponse.of(service.renew(id, months), service.today());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate (soft delete)", description = "The attendance history is kept.")
    ResponseEntity<Void> deactivate(@PathVariable long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivation")
    BeneficiaryResponse reactivate(@PathVariable long id) {
        return BeneficiaryResponse.of(service.reactivate(id), service.today());
    }
}
