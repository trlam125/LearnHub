import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public class Healthcheck {
    public static void main(String[] args) throws Exception {
        var response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080/health")).timeout(Duration.ofSeconds(4)).GET().build(), HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 200) System.exit(1);
    }
}
