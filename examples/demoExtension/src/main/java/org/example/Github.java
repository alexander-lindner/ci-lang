package org.example;

import com.google.gson.JsonParser;
import me.tongfei.progressbar.ProgressBar;
import org.alindner.cish.extension.annotations.CishExtension;
import org.alindner.cish.extension.annotations.MavenDependency;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@CishExtension("0.2.6")
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

	@MavenDependency(value = "org.apache.commons", name = "commons-lang3", version = "3.12.0")
	public static Double test() {
		return org.apache.commons.lang3.math.NumberUtils.createDouble("5");
	}

	@MavenDependency(value = "me.tongfei", name = "progressbar", version = "0.9.1")
	public static void progress() {
		final List<Integer> list = IntStream.generate(() -> (int) (Math.random() * 10000))
		                                    .limit(100)
		                                    .boxed()
		                                    .collect(Collectors.toList());

		for (final Integer x : ProgressBar.wrap(list, "count integer")) {
			try {
				Thread.sleep(1500);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}