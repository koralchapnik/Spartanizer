package il.org.spartan.spartanizer.java;

import org.eclipse.jdt.core.dom.*;

/** Maintain a canonical order of modifiers.
 * @author Yossi Gil
 * @since 2016 */
public enum ModifiersOrdering {
  $ANNOTATION$, //
  PUBLIC, //
  PROTECTED, //
  PRIVATE, //
  ABSTRACT, //
  STATIC, //
  DEFAULT, //
  FINAL, //
  TRANSIENT, //
  VOLATILE, //
  SYNCHRONIZED, //
  NATIVE, //
  STRICTFP, //
  ;
  public static int compare(final IExtendedModifier modifier1, final IExtendedModifier modifier2) {
    return compare(find(modifier1), find(modifier2));
  }

  public static int compare(IExtendedModifier m, ModifiersOrdering o) {
    return compare(find(m), o);
  }

  public static int compare(String modifier1, String modifier2) {
    return compare(find(modifier1), find(modifier2));
  }

  public static ModifiersOrdering find(final IExtendedModifier m) {
    return find(m + "");
  }

  public static boolean greaterThanOrEquals(IExtendedModifier m1, ModifiersOrdering m2) {
    return compare(m1, m2) >= 0;
  }

  private static int compare(final ModifiersOrdering m1, final ModifiersOrdering m2) {
    return m1.ordinal() - m2.ordinal();
  }

  static ModifiersOrdering find(String modifier) {
    for (final ModifiersOrdering $ : ModifiersOrdering.values())
      if (modifier.equals(($ + "").toLowerCase()))
        return $;
    return $ANNOTATION$;
  }
}