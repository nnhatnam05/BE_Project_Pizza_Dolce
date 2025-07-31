package aptech.be.dto;

import java.util.List;

public class OrderRequestDTO {
    private Long customerId;
    private List<FoodOrderItemDTO> foods; // <--- giữ lại cái này
    private String note;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<FoodOrderItemDTO> getFoods() {
        return foods;
    }

    public void setFoods(List<FoodOrderItemDTO> foods) {
        this.foods = foods;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
