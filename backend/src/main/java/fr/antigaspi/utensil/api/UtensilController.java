package fr.antigaspi.utensil.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.antigaspi.utensil.model.Utensil;
import fr.antigaspi.utensil.service.UtensilService;

/**
 * API REST des ustensiles, destinée au front.
 */
@RestController
@RequestMapping("/api/utensils")
public class UtensilController {

    private final UtensilService utensilService;

    public UtensilController(UtensilService utensilService) {
        this.utensilService = utensilService;
    }

    @GetMapping
    public List<UtensilResponse> list() {
        return utensilService.listAll().stream().map(UtensilController::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<UtensilResponse> add(@RequestBody UtensilRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(utensilService.add(request.name())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable long id) {
        utensilService.remove(id);
        return ResponseEntity.noContent().build();
    }

    private static UtensilResponse toResponse(Utensil utensil) {
        return new UtensilResponse(utensil.id(), utensil.name());
    }
}
