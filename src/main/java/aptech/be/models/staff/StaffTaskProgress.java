package aptech.be.models.staff;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "task_progress")
public class StaffTaskProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private StaffProfile staff;

    private LocalDate date;
    private String taskType; // DELIVERY, REFUND, ORDER
    private String taskStatus; // IN_PROGRESS, COMPLETED, FAILED

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StaffProfile getStaff() {
        return staff;
    }

    public void setStaff(StaffProfile staff) {
        this.staff = staff;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }
}

