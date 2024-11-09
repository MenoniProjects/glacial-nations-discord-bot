package net.menoni.glacial.bot.util;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class Async {

	public static CompletableFuture<Void> allOf(Collection<? extends CompletableFuture<?>> futures) {
		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
	}

}
