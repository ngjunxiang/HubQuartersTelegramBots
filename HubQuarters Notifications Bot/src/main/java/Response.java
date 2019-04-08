import java.util.ArrayList;

public class Response {
    private boolean success;
    private int status;
    private ArrayList<Image> images;

    public Image getLatestImg() {
        return images.get(images.size() - 1);
    }

    public int getStatus() {
        return status;
    }
}
