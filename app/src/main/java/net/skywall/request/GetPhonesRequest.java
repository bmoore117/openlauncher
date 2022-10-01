package net.skywall.request;

public class GetPhonesRequest {

    private String skywallUsername;
    private String skywallPassword;

    public String getSkywallUsername() {
        return skywallUsername;
    }

    public void setSkywallUsername(String skywallUsername) {
        this.skywallUsername = skywallUsername;
    }

    public String getSkywallPassword() {
        return skywallPassword;
    }

    public void setSkywallPassword(String skywallPassword) {
        this.skywallPassword = skywallPassword;
    }
}
