package com.emedical.booking.dto.DoctorSlotDTO;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateSlotsDTO {

    private Long doctorId;
    private Long doctorSlotId;
    private LocalDate workdate;
    private LocalTime startShift;
    private LocalTime endShift;
    private Integer durationMinutes;

}
