package aptech.be.dto.customer;

public class CustomerSignupResponse {
    private String message;

    public CustomerSignupResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
