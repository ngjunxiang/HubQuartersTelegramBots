package hubquarters.cms.tenants;

public class Image {
    private String id;
    private String imageName;
    private String created_at;
    private String updated_at;
    private int numPeopleDetected;

    public String getCreatedAt() {
        return created_at;
    }

    public int getNumPeopleDetected() {
        return numPeopleDetected;
    }
}
