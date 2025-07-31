package aptech.be.controllers.staff;

import aptech.be.dto.staff.AttendanceReport;
import aptech.be.dto.staff.AttendanceResponse;
import aptech.be.services.staff.AttendanceService;
import aptech.be.config.JwtProvider;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RestControllerAdvice
@RequestMapping("api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final JwtProvider jwtProvider;

    public AttendanceController(AttendanceService attendanceService,
                                JwtProvider jwtProvider) {
        this.attendanceService = attendanceService;
        this.jwtProvider = jwtProvider;
    }


    @PostMapping("/face-scan")
    public ResponseEntity<AttendanceResponse> scanFace(
            @RequestParam("frames") List<MultipartFile> frames,
            @RequestParam(value = "staffCode", required = false) String staffCode) {

        AttendanceResponse resp = attendanceService.recordAttendanceByFace(frames, staffCode);
        return ResponseEntity.ok(resp);
    }




    @GetMapping("/history/{staffCode}")
    public ResponseEntity<List<AttendanceResponse>> history(
            @PathVariable String staffCode,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        List<AttendanceResponse> list = attendanceService.getHistory(staffCode, fromDate, toDate);
        return ResponseEntity.ok(list);
    }



    @GetMapping("/reports/monthly")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('VIEW_REPORT')")
    public ResponseEntity<List<AttendanceReport>> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month) {

        List<AttendanceReport> reports = attendanceService.generateMonthlyReports(year, month);
        return ResponseEntity.ok(reports);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(400)
                .body(Map.of("message", ex.getMessage()));
    }
}
