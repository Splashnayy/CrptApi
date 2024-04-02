import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        try {
            this.semaphore.acquire(requestLimit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        startRefillThread(requestLimit, timeUnit);
    }

    public HttpResponse<String> createDocument(Object document, String signature) throws IOException, InterruptedException {
        semaphore.acquire();
        String json = "{ \"description\": " + document + ", \"signature\": \"" + signature + "\" }";
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .uri(API_URL)
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void startRefillThread(int requestLimit, TimeUnit timeUnit) {
        new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(timeUnit.toMillis(1));
                    semaphore.release();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
}