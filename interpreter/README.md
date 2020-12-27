

```java
Interpreter.log.debug("Interpreting the following file: " + file.getAbsolutePath());
final CishCompiler cishCompiler = new CishCompiler(this.debug);
try {
    cishCompiler.compile(file);
} catch (final ParseException e) {
    Interpreter.log.error("Error parsing the file " + file.getAbsolutePath(), e);
    e.printStackTrace();
} catch (final FileNotFoundException e) {
    Interpreter.log.error("File not found", e);
}

final List<String> contents = cishCompiler.getContent().lines().collect(Collectors.toList());
final List<String> imports  = new ArrayList<>();
imports.add("import java.io.*;");
imports.add("import org.alindner.cish.lang.utils.*;");
contents.addAll(0, imports);

Interpreter.log.debug(String.format("The final script is:\n =============== \n%s\n ===============", String.join("\n", contents)));

Interpreter.log.debug("Creating shell");
final JShell js = JShell.create();
Interpreter.log.debug("Adding utils to classpath");
try {
    js.addToClasspath(new File(Interpreter.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath());
} catch (final URISyntaxException e) {
    Interpreter.log.error(e);
}
Interpreter.log.debug("Start interpreting files");
contents.forEach(s -> {
    Interpreter.log.debug(s);
    final List<SnippetEvent> event = js.eval(s);
    event.forEach(snippetEvent -> Interpreter.log.debug(String.format("Status: [%s], value: [%s]", snippetEvent.status(), snippetEvent.value())));
});
cishCompiler.proceedJava();
//js.imports().forEach(System.out::println);
//js.variables().forEach(System.out::println);
```