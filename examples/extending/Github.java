import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.net.URI;

package demo.extension.extending;

@CishExtension
public class Github {
	@Dependencies({
			              Maven(pkg = "com.google.code.gson", name = "gson", version = "2.8.6")
	              })
	public static Integer getStars(final String repoName) {
		try {
			final var request = HttpRequest.newBuilder(
					URI.create(String.format("https://api.github.com/repos/%s", repoName))
			).header("accept", "application/json").build();


			final var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());


			return new JsonParser().parse(response.body()).getAsJsonObject().get("stargazers_count").getAsString(); //John
		} catch (final Exception exception) {
			return 0;
		}

	}
}