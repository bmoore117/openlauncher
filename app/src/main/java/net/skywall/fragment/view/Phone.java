package net.skywall.fragment.view;

public class Phone {

    private String phoneName;
    private String enrollmentTokenName;

    public Phone(String phoneName, String enrollmentTokenName) {
        this.phoneName = phoneName;
        this.enrollmentTokenName = enrollmentTokenName;
    }

    public String getPhoneName() {
        return phoneName;
    }

    public void setPhoneName(String phoneName) {
        this.phoneName = phoneName;
    }

    public String getEnrollmentTokenName() {
        return enrollmentTokenName;
    }

    public void setEnrollmentTokenName(String enrollmentTokenName) {
        this.enrollmentTokenName = enrollmentTokenName;
    }
}

