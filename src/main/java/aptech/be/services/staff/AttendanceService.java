package aptech.be.services.staff;

import aptech.be.dto.staff.AttendanceReport;
import aptech.be.dto.staff.AttendanceResponse;
import aptech.be.models.UserEntity;
import aptech.be.models.staff.AttendanceRecord;
import aptech.be.models.staff.ShiftAssignment;
import aptech.be.models.staff.StaffProfile;
import aptech.be.repositories.UserRepository;
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

import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final UserRepository userRepository;
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
            ShiftAssignmentRepository shiftRepo, UserRepository userRepository) {
        this.staffRepo = staffRepo;
        this.attendanceRepo = attendanceRepo;
        this.shiftRepo = shiftRepo;
        this.userRepository = userRepository;
    }

    /**
     * Điểm danh bằng nhận diện khuôn mặt, trả về thông tin điểm danh và nhân viên
     */

    public AttendanceResponse recordAttendanceByFace(List<MultipartFile> frames, String staffCode) {
        if (staffCode == null || staffCode.trim().isEmpty()) {
            // Nhận diện tự động - gửi frames lên Flask, lấy staffCode trả về
            FaceAiResult aiResult = sendFramesToAiServiceAuto(frames);
            if (!aiResult.isMatched() || aiResult.getStaffCode() == null) {
                throw new RuntimeException(aiResult.getMessage() != null ? aiResult.getMessage() : "Không nhận diện được khuôn mặt!");
            }
            staffCode = aiResult.getStaffCode();
            // Lấy info staff từ DB qua staffCode
            String finalStaffCode = staffCode; // copy sang biến mới
            StaffProfile staff = null;
            if (finalStaffCode != null && finalStaffCode.startsWith("NV")) {
                // Nếu là mã staff thật
                staff = staffRepo.findByStaffCode(finalStaffCode)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên: " + finalStaffCode));
            } else {
                // Nếu là tên file ảnh, map sang user rồi staff
                // Giả sử AI trả về đúng tên file hoặc path, ví dụ: "6bdac0b9-0843-455e-bcca-10b155e19734_z6787778616152_51e043384308daceea501910ed4a80ef"
                UserEntity user = userRepository.findByImageUrlContaining(finalStaffCode)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ảnh: " + finalStaffCode));
                staff = staffRepo.findByUserId(user.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với user id: " + user.getId()));
            }


            // Ghi nhận điểm danh
            AttendanceResponse attendanceRes = recordAttendanceInternal(staff, "FACE", aiResult.getScore());

            attendanceRes.setStaffCode(staff.getStaffCode());
            attendanceRes.setStaffName(staff.getUser() != null ? staff.getUser().getName() : null);
            attendanceRes.setAvatarUrl(staff.getUser() != null ? staff.getUser().getImageUrl() : null);
            attendanceRes.setPosition(staff.getPosition());
            attendanceRes.setDepartment(staff.getWorkLocation());
            attendanceRes.setMessage(aiResult.getMessage());
            return attendanceRes;
        } else {
            // Logic cũ: điểm danh xác thực đúng người nhập staffCode
            StaffProfile staff = staffRepo.findByStaffCode(staffCode)
                    .orElseThrow(() -> new RuntimeException("Staff not found"));

            UserEntity user = staff.getUser();
            if (user == null || user.getImageUrl() == null || user.getImageUrl().isEmpty()) {
                throw new RuntimeException("Staff has not registered a face image yet");
            }

            byte[] faceTemplate = null;
            try (var fis = new java.io.FileInputStream(uploadBasePath + "/" + user.getImageUrl())) {
                faceTemplate = IOUtils.toByteArray(fis);
            } catch (IOException e) {
                throw new RuntimeException("Cannot load user face image", e);
            }

            FaceAiResult aiResult;
            try {
                aiResult = sendFramesToAiService(frames, faceTemplate);
            } catch (IOException e) {
                throw new RuntimeException("Cannot send frames to AI service", e);
            }
            boolean matched = aiResult.isMatched();
            double score = aiResult.getScore();

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

    }

    /**
     * Gửi frames lên AI Service (flow tự động, không truyền staffCode, không truyền ảnh mẫu)
     * Nhận về staffCode, score, matched, message
     */
    private FaceAiResult sendFramesToAiServiceAuto(List<MultipartFile> frames) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            for (MultipartFile frame : frames) {
                body.add("frames", new MultipartInputStreamFileResource(frame));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(faceRecognitionApiUrl, requestEntity, Map.class);

            if (response.getStatusCode() != HttpStatus.OK || !response.hasBody()) {
                throw new RuntimeException("Face AI service error");
            }
            Map bodyMap = response.getBody();

            boolean matched = Boolean.TRUE.equals(bodyMap.get("matched")) || "true".equals(String.valueOf(bodyMap.get("matched")));
            String staffCode = bodyMap.get("staffCode") != null ? bodyMap.get("staffCode").toString() : null;
            double score = bodyMap.get("score") != null ? Double.parseDouble(bodyMap.get("score").toString()) : 1.0;
            String message = bodyMap.get("message") != null ? bodyMap.get("message").toString() : null;

            return new FaceAiResult(matched, score, staffCode, message);
        } catch (Exception e) {
            throw new RuntimeException("AI Service error: " + e.getMessage(), e);
        }
    }

    /**
     * Gửi frames + ảnh mẫu lên AI Service (flow xác thực cũ)
     */
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

        boolean matched = Boolean.TRUE.equals(bodyMap.get("matched")) || "true".equals(String.valueOf(bodyMap.get("matched")));
        double score = bodyMap.get("score") != null ? Double.parseDouble(bodyMap.get("score").toString()) : 1.0;

        return new FaceAiResult(matched, score, null, null);
    }

    // Kết quả AI Service trả về
    public static class FaceAiResult {
        private final boolean matched;
        private final double score;
        private final String staffCode;
        private final String message;

        public FaceAiResult(boolean matched, double score, String staffCode, String message) {
            this.matched = matched;
            this.score = score;
            this.staffCode = staffCode;
            this.message = message;
        }

        public boolean isMatched() { return matched; }
        public double getScore() { return score; }
        public String getStaffCode() { return staffCode; }
        public String getMessage() { return message; }
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
    public List<AttendanceResponse> getHistory(String staffCode, LocalDate fromDate, LocalDate toDate) {
        StaffProfile staff = staffRepo.findByStaffCode(staffCode)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        LocalDate start = (fromDate != null) ? fromDate : LocalDate.now().minusDays(7);
        LocalDate end = (toDate != null) ? toDate : LocalDate.now();

        List<AttendanceRecord> recs = attendanceRepo.findByStaffAndDateBetween(staff, start, end);

        return recs.stream()
                .map(r -> new AttendanceResponse(
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
                ))
                .collect(Collectors.toList());
    }

    // Tạo báo cáo điểm danh hàng tháng cho tất cả nhân viên
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
}
