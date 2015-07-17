package org.spartan.refacotring.utils;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;

/**
 * * A an empty <code><b>enum</b></code> for fluent programming. The name says
 * it all: The name, followed by a dot, followed by a method name, should read
 * like a word phrase.
 *
 * Specifically, this class determines precedence and associativity of Java
 * operators; data is drawn from
 * {@link "http://introcs.cs.princeton.edu/java/11precedence/"}
 *
 * @author Yossi Gil
 * @since 2015-07-14
 *
 */
public enum Precedence {
  ;
  /**
   * * A an empty <code><b>enum</b></code> for fluent programming. The name says
   * it all: The name, followed by a dot, followed by a method name, should read
   * like a word phrase.
   *
   * @author Yossi Gil
   * @since 2015-07-14
   *
   */
  public enum Is {
    ;
    /**
     * determine whether an integer falls within the legal range of precedences.
     *
     * @param precedence
     *          JD
     *
     * @return <code><b>true</b></code> <i>iff</i> the parameter a legal
     *         precedence of Java.
     */
    public static boolean legal(final int precedence) {
      return precedence >= 1 && precedence <= 15;
    }
  }

  public static int of(final Expression e) {
    if (e instanceof InfixExpression)
      return of((InfixExpression) e);
    if (e instanceof Assignment)
      return of((Assignment) e);
    return UNDEFINED;
  }
  public static int of(final InfixExpression e) {
    return of(e.getOperator());
  }
  public static int of(final Assignment a) {
    return of(a.getOperator());
  }
  private static int of(final Assignment.Operator o) {
    return of(o.toString());
  }
  public static int of(final InfixExpression.Operator o) {
    return of(o.toString());
  }

  private final static int UNDEFINED = -1;

  private static int of(final String key) {
    return of.containsKey(key) ? of.get(key) : UNDEFINED;
  }

  private static final ChainStringToIntMap of = new ChainStringToIntMap()//
      .putOn(1, "[]", ".", "() invoke", "++ post", "-- post") //
      .putOn(2, "++ pre", "-- pre", "+ unary", "- unary", "!", "~") //
      .putOn(3, "() cast", "new") //
      .putOn(4, "*", "/", "%") // multiplicative
      .putOn(5, "+", "-") // additive
      .putOn(6, ">>", "<<", ">>>") // shift
      .putOn(7, "<", "<=", ">", ">=", "instanceof") // relational
      .putOn(8, "==", "!=") // equality
      .putOn(9, "&") // bitwise AND
      .putOn(10, "^") // bitwise XOR
      .putOn(11, "|") // bitwise OR
      .putOn(12, "&&") // conditional AND
      .putOn(13, "||") // conditional OR
      .putOn(14, "?", ":") // conditional
      .putOn(15, "=", // assignment
          "+=", "-=", // assignment, additive
          "*= ", "/=", "%=", // assignment, multiplicative
          "&=", "^=", "|=", // assignment, bitwise
          "<<=", ">>=", ">>>="// assignment, shift
  );
}
