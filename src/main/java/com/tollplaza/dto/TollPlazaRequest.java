package com.tollplaza.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TollPlazaRequest(
        @NotBlank(message = "sourcePincode is required")
        @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "Invalid source or destination pincode")
        String sourcePincode,

        @NotBlank(message = "destinationPincode is required")
        @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "Invalid source or destination pincode")
        String destinationPincode) {
}
