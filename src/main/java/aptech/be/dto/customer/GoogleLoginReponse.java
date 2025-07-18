package aptech.be.dto.customer;

public class GoogleLoginReponse {
    private String token;
    public GoogleLoginReponse(String token) { this.token = token; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
