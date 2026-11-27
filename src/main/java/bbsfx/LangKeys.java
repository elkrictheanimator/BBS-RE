package bbsfx;

import java.util.function.Function;
import mchorse.bbs_mod.l10n.keys.IKey;

public final class LangKeys implements IKey {
   private static Function<String, String> resolver = key -> key;
   private final String key;

   private LangKeys(String key) {
      this.key = key;
   }

   public static IKey key(String key) {
      return new LangKeys(key);
   }

   public static void resolver(Function<String, String> resolver) {
      LangKeys.resolver = resolver;
   }

   public String get() {
      return (String)resolver.apply(this.key);
   }
}
