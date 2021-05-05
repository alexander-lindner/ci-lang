open module org.example {
	requires transitive java.net.http;
	requires transitive com.google.gson;
	exports org.example;
	requires transitive org.alindner.cish.lang;
	requires transitive org.alindner.cish.extension;
}