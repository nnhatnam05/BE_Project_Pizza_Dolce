package aptech.be.dto.customer;

public class CustomerLoginResponse {
    private String token;

    public CustomerLoginResponse(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
