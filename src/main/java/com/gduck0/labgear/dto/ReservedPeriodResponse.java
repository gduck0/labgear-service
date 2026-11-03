package com.gduck0.labgear.dto;

import java.time.LocalDate;

public record ReservedPeriodResponse(LocalDate startDate, LocalDate endDate) {
}
