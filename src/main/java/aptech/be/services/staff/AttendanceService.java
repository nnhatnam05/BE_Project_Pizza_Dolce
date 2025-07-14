package aptech.be.services.staff;

import aptech.be.dto.staff.AttendanceResponse;
import aptech.be.dto.staff.AttendanceReport;
import aptech.be.models.UserEntity;
import aptech.be.models.staff.AttendanceRecord;
import aptech.be.models.staff.ShiftAssignment;
import aptech.be.models.staff.StaffProfile;
import aptech.be.repositories.staff.AttendanceRecordRepository;
import aptech.be.repositories.staff.ShiftAssignmentRepository;
import aptech.be.repositories.staff.StaffProfileRepository;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Value("${app.upload.base-path}")
    private String uploadBasePath;

    private final StaffProfileRepository staffRepo;
    private final AttendanceRecordRepository attendanceRepo;
    private final ShiftAssignmentRepository shiftRepo;

    @Value("${ai.face-recognition.url:http://localhost:5000/recognize}")
    private String faceRecognitionApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public AttendanceService(
            StaffProfileRepository staffRepo,
            AttendanceRecordRepository attendanceRepo,
            ShiftAssignmentRepository shiftRepo) {
        this.staffRepo = staffRepo;
        this.attendanceRepo = attendanceRepo;
        this.shiftRepo = shiftRepo;
    }

    /**
     * Điểm danh bằng nhận diện khuôn mặt, trả về thông tin điểm danh và nhân viên
     */

    public AttendanceResponse recordAttendanceByFace(List<MultipartFile> frames, String staffCode) {
        StaffProfile staff = staffRepo.findByStaffCode(staffCode)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        UserEntity user = staff.getUser();
        if (user == null || user.getImageUrl() == null || user.getImageUrl().isEmpty()) {
            throw new RuntimeException("Staff has not registered a face image yet");
        }

        String relativePath = user.getImageUrl();
        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            relativePath = relativePath.substring(1);
        }
        String absolutePath = Paths.get(uploadBasePath, relativePath).toString();

        File file = new File(absolutePath);
        if (!file.exists()) {
            throw new RuntimeException("Cannot load user face image at: " + absolutePath);
        }

        byte[] faceTemplate;
        try (FileInputStream fis = new FileInputStream(file)) {
            faceTemplate = IOUtils.toByteArray(fis);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load user face image", e);
        }

        boolean matched;
        double score;
        try {
            FaceAiResult aiResult = sendFramesToAiService(frames, faceTemplate);
            matched = aiResult.isMatched();
            score = aiResult.getScore();
        } catch (IOException e) {
            throw new RuntimeException("File error: " + e.getMessage(), e);
        }

        double threshold = 0.8;
        if (!matched || score >= threshold) {
            throw new RuntimeException("Face recognition failed. Please try again.");
        }

        AttendanceResponse attendanceRes = recordAttendanceInternal(staff, "FACE", score);

        attendanceRes.setStaffCode(staff.getStaffCode());
        attendanceRes.setStaffName(user.getName());
        attendanceRes.setAvatarUrl(user.getImageUrl());
        attendanceRes.setPosition(staff.getPosition());
        attendanceRes.setDepartment(staff.getWorkLocation());

        return attendanceRes;
    }

    private FaceAiResult sendFramesToAiService(List<MultipartFile> frames, byte[] templateImage) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (MultipartFile frame : frames) {
            body.add("frames", new MultipartInputStreamFileResource(frame));
        }
        body.add("img2", new ByteArrayResource(templateImage) {
            @Override public String getFilename() { return "template.jpg"; }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(faceRecognitionApiUrl, requestEntity, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || !response.hasBody()) {
            throw new RuntimeException("Face AI service error");
        }
        Map bodyMap = response.getBody();
        System.out.println("AI response: " + bodyMap);

        Object matchedObj = bodyMap.get("matched");
        Object scoreObj = bodyMap.get("score");
        if (matchedObj == null || scoreObj == null) {
            throw new RuntimeException("Invalid AI response: " + bodyMap);
        }
        boolean matched = Boolean.TRUE.equals(matchedObj) || "true".equals(matchedObj.toString());
        double score = Double.parseDouble(scoreObj.toString());

        return new FaceAiResult(matched, score);
    }



    // Kết quả AI Service trả về
    public static class FaceAiResult {
        private final boolean matched;
        private final double score;
        public FaceAiResult(boolean matched, double score) {
            this.matched = matched;
            this.score = score;
        }
        public boolean isMatched() { return matched; }
        public double getScore() { return score; }
    }

    /**
     * Gửi ảnh lên AI Service, nhận về matched + score
     */
    private FaceAiResult sendImagesToAiService(MultipartFile upload, byte[] templateImage) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("img1", new MultipartInputStreamFileResource(upload));
        body.add("img2", new ByteArrayResource(templateImage) {
            @Override public String getFilename() { return "template.jpg"; }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                faceRecognitionApiUrl, requestEntity, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || !response.hasBody()) {
            throw new RuntimeException("Face AI service error");
        }
        Map bodyMap = response.getBody();
        System.out.println("AI response: " + bodyMap);

        Object matchedObj = bodyMap.get("matched");
        Object scoreObj = bodyMap.get("score");
        if (matchedObj == null || scoreObj == null) {
            throw new RuntimeException("Invalid AI response: " + bodyMap);
        }
        boolean matched = Boolean.TRUE.equals(matchedObj) || "true".equals(matchedObj.toString());
        double score = Double.parseDouble(scoreObj.toString());

        return new FaceAiResult(matched, score);
    }

    /**
     * Lưu điểm danh vào DB và trả về AttendanceResponse cơ bản (chưa chứa thông tin nhân viên)
     */
    private AttendanceResponse recordAttendanceInternal(StaffProfile staff, String method, double faceScore) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalDateTime timestamp = LocalDateTime.now();

        List<AttendanceRecord> records = attendanceRepo.findByStaffAndDate(staff, today);
        AttendanceRecord record = records.stream()
                .filter(r -> r.getCheckOut() == null)
                .findFirst()
                .orElse(null);

        if (record == null) {
            record = new AttendanceRecord();
            record.setStaff(staff);
            record.setDate(today);
            record.setCheckIn(now);
            record.setMethod(method);
            try {
                record.getClass().getMethod("setFaceRecognitionScore", Double.class);
                record.setFaceRecognitionScore(faceScore);
            } catch (NoSuchMethodException e) {
                // Nếu entity chưa khai báo field này thì bỏ qua
            }

            List<ShiftAssignment> shifts = shiftRepo.findByStaffAndDate(staff, today);
            ShiftAssignment shift = shifts.isEmpty() ? null : shifts.get(0);
            if (shift != null && now.isAfter(shift.getStart().plusMinutes(5))) {
                record.setStatus("LATE");
            } else {
                record.setStatus("ON_TIME");
            }

            attendanceRepo.save(record);

            return new AttendanceResponse(
                    "Check-in successful",
                    record.getStatus(),
                    record.getCheckIn(),
                    null,
                    timestamp,
                    staff.getStaffCode(),
                    staff.getUser() != null ? staff.getUser().getName() : "Unknown",
                    staff.getPosition() != null ? staff.getPosition() : "",
                    staff.getWorkLocation() != null ? staff.getWorkLocation() : "",
                    staff.getUser() != null ? staff.getUser().getImageUrl() : ""
            );
        }

        record.setCheckOut(now);
        long minutesWorked = ChronoUnit.MINUTES.between(record.getCheckIn(), now);
        record.setHoursWorked(minutesWorked / 60.0);
        attendanceRepo.save(record);

        return new AttendanceResponse(
                "Check-out successful",
                record.getStatus(),
                record.getCheckIn(),
                record.getCheckOut(),
                timestamp,
                staff.getStaffCode(),
                staff.getUser() != null ? staff.getUser().getName() : "Unknown",
                staff.getPosition() != null ? staff.getPosition() : "",
                staff.getWorkLocation() != null ? staff.getWorkLocation() : "",
                staff.getUser() != null ? staff.getUser().getImageUrl() : ""
        );
    }

    // ... Các hàm history, report ... (không đổi so với bản bạn gửi lên)

    public List<AttendanceResponse> getHistory(String staffCode, LocalDate from, LocalDate to) {
        StaffProfile staff = staffRepo.findByStaffCode(staffCode)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        LocalDate start = (from != null) ? from : LocalDate.now().minusDays(7);
        LocalDate end = (to != null) ? to : LocalDate.now();

        List<AttendanceRecord> recs = attendanceRepo.findByStaffAndDateBetween(staff, start, end);

        return recs.stream()
                .map(r -> {
                    AttendanceResponse res = new AttendanceResponse(
                            staff.getUser() != null ? staff.getUser().getName() : "Unknown",
                            r.getDate().toString(),
                            r.getCheckIn(),
                            r.getCheckOut(),
                            LocalDateTime.of(r.getDate(), r.getCheckIn()),
                            staff.getStaffCode(),
                            staff.getUser() != null ? staff.getUser().getName() : "Unknown",
                            staff.getPosition() != null ? staff.getPosition() : "",
                            staff.getWorkLocation() != null ? staff.getWorkLocation() : "",
                            staff.getUser() != null ? staff.getUser().getImageUrl() : ""
                    );
                    res.setStaffCode(staff.getStaffCode());
                    res.setStaffName(staff.getUser() != null ? staff.getUser().getName() : null);
                    res.setAvatarUrl(staff.getUser() != null ? staff.getUser().getImageUrl() : null);
                    res.setPosition(staff.getPosition());
                    res.setDepartment(staff.getWorkLocation());
                    return res;
                })
                .collect(Collectors.toList());
    }

    public List<AttendanceReport> generateMonthlyReports(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<StaffProfile> staffs = staffRepo.findAll();
        return staffs.stream()
                .map(staff -> {
                    List<AttendanceRecord> recs = attendanceRepo.findByStaffAndDateBetween(staff, start, end);

                    int totalDays = recs.size();
                    double totalHours = recs.stream()
                            .filter(r -> r.getHoursWorked() != null)
                            .mapToDouble(AttendanceRecord::getHoursWorked)
                            .sum();

                    long timesLate = recs.stream()
                            .filter(r -> "LATE".equals(r.getStatus()))
                            .count();

                    long daysAbsent = ym.lengthOfMonth() - totalDays;

                    AttendanceReport rpt = new AttendanceReport();
                    rpt.setStaffCode(staff.getStaffCode());
                    rpt.setName(staff.getUser() != null ? staff.getUser().getName() : "Unknown");
                    rpt.setTotalDays(totalDays);
                    rpt.setTotalHours(totalHours);
                    rpt.setTimesLate(timesLate);
                    rpt.setDaysAbsent(daysAbsent);
                    return rpt;
                })
                .collect(Collectors.toList());
    }

    // Helper cho multipart upload từ MultipartFile
    private static class MultipartInputStreamFileResource extends org.springframework.core.io.InputStreamResource {
        private final String filename;
        MultipartInputStreamFileResource(MultipartFile file) throws IOException {
            super(file.getInputStream());
            this.filename = file.getOriginalFilename();
        }
        @Override public String getFilename() { return this.filename; }
        @Override public long contentLength() { return -1; }
    }
}
