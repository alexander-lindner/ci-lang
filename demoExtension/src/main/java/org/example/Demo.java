package org.example;


import org.alindner.cish.extension.annotations.CishExtension;
import org.alindner.cish.lang.Console;

@CishExtension
public class Demo {
	public static void greetings() {
		Console.print("Hello World!");
	}
}
