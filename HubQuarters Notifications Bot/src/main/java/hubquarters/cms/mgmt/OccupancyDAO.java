package hubquarters.cms.mgmt;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class OccupancyDAO {
    public static List<String> retrieveData() throws URISyntaxException, IOException {
        List<String> result = new ArrayList<>();

        String fullQuery = "";

        CloseableHttpClient client = HttpClients.createDefault();

        try {
            String requestURI = new java.net.URI("http", "3.1.241.179", "/api/liveimage", fullQuery, "").toString();
            System.out.println("Request URI=" + requestURI);

            HttpGet httpGet = new HttpGet(requestURI);

            // Send GET request
            CloseableHttpResponse response = client.execute(httpGet);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                result.add("Sorry, something went wrong. Please try again!");
                return result;
            } else {
                try {
                    // Do the needful with entity.
                    HttpEntity entity = response.getEntity();
                    InputStream rstream = entity.getContent();

                    BufferedReader br = new BufferedReader(new InputStreamReader(rstream));
                    String responseMsg;
                    if ((responseMsg = br.readLine()) != null) {
//                        System.out.println("Retrieved status=" + responseMsg);
                    }

                    Gson g = new Gson();
                    Response res = g.fromJson(responseMsg, Response.class);

                    if (res.getStatus() == 200) {
                        Image latestImg = res.getLatestImg();
                        result.add(latestImg.getNumPeopleDetected() + "");
                        result.add(latestImg.getCreatedAt());
                    } else {
                        result.add("Sorry, something went wrong. Please try again!");
                    }

                    br.close();
                } finally {
                    // Closing the response
                    response.close();
                }
            }
        } finally {
                client.close();
        }

        return result;
    }
}
