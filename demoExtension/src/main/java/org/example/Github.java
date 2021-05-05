package org.example;

import com.google.gson.JsonParser;
import org.alindner.cish.extension.annotations.CishExtension;
import org.alindner.cish.extension.annotations.MavenDependency;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


@CishExtension("0.2.6")
//@CishExtension(value="0.2.6", provides = {"com.google.gson.JsonParser"})
@MavenDependency(value = "com.google.code.gson", name = "gson", version = "2.8.6")
public class Github {

	@MavenDependency(value = "com.google.code.gson", name = "gson", version = "2.8.6")
	public static Integer getStars(final String repoName) {
		try {
			final var request = HttpRequest.newBuilder(
					URI.create(String.format("https://api.github.com/repos/%s", repoName))
			).header("accept", "application/json").build();

			final var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

			return JsonParser.parseString(response.body()).getAsJsonObject().get("stargazers_count").getAsInt();
		} catch (final Exception exception) {
			return 0;
		}
	}
}