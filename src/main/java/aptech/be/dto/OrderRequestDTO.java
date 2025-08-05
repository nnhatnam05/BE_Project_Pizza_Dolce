package aptech.be.dto;

import java.util.List;

public class OrderRequestDTO {
    private Long customerId;
    private List<FoodOrderItemDTO> foods; // <--- giữ lại cái này
    private String note;
    private AddressSelectionDTO deliveryAddress; // Thêm field mới cho địa chỉ giao hàng
    private String voucherCode; // Mã voucher được áp dụng
    private Double voucherDiscount; // Số tiền giảm giá từ voucher
    private Boolean needInvoice; // Khách hàng có muốn xuất hóa đơn không
    private Long tableId; // ID của bàn (cho dine-in orders)

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

    public AddressSelectionDTO getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(AddressSelectionDTO deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public Double getVoucherDiscount() {
        return voucherDiscount;
    }

    public void setVoucherDiscount(Double voucherDiscount) {
        this.voucherDiscount = voucherDiscount;
    }

    public Boolean getNeedInvoice() {
        return needInvoice;
    }

    public void setNeedInvoice(Boolean needInvoice) {
        this.needInvoice = needInvoice;
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }
}
