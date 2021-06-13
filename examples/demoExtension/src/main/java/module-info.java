open module org.example {
	requires transitive java.net.http;
	requires transitive com.google.gson;
	exports org.example;
	requires transitive cish.lang;
	requires transitive cish.extension;
	requires org.apache.commons.lang3;
	requires progressbar;
}