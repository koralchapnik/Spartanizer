package il.org.spartan.refactoring.wring;

import static il.org.spartan.refactoring.utils.Funcs.duplicate;
import static il.org.spartan.refactoring.utils.Funcs.left;
import static il.org.spartan.refactoring.utils.Funcs.right;
import static il.org.spartan.refactoring.wring.Wrings.size;
import static org.eclipse.jdt.core.dom.Assignment.Operator.ASSIGN;
import static org.eclipse.jdt.core.dom.Assignment.Operator.BIT_AND_ASSIGN;
import static org.eclipse.jdt.core.dom.Assignment.Operator.BIT_OR_ASSIGN;
import static org.eclipse.jdt.core.dom.Assignment.Operator.BIT_XOR_ASSIGN;
import static org.eclipse.jdt.core.dom.Assignment.Operator.DIVIDE_ASSIGN;
import static org.eclipse.jdt.core.dom.Assignment.Operator.LEFT_SHIFT_ASSIGN;
import static org.eclipse.jdt.core.dom.Assignment.Operator.MINUS_ASSIGN;
import static org.eclipse.jdt.core.dom.Assignment.Operator.PLUS_ASSIGN;
import static org.eclipse.jdt.core.dom.Assignment.Operator.REMAINDER_ASSIGN;
import static org.eclipse.jdt.core.dom.Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN;
import static org.eclipse.jdt.core.dom.Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN;
import static org.eclipse.jdt.core.dom.Assignment.Operator.TIMES_ASSIGN;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.AND;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.DIVIDE;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.LEFT_SHIFT;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.MINUS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.OR;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.PLUS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.REMAINDER;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.RIGHT_SHIFT_SIGNED;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.TIMES;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.XOR;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.text.edits.TextEditGroup;

import il.org.spartan.misc.Wrapper;
import il.org.spartan.refactoring.preferences.PluginPreferencesResources.WringGroup;
import il.org.spartan.refactoring.utils.Collect;
import il.org.spartan.refactoring.utils.Extract;
import il.org.spartan.refactoring.utils.Is;
import il.org.spartan.refactoring.utils.Plant;
import il.org.spartan.refactoring.utils.Rewrite;
import il.org.spartan.refactoring.utils.Scalpel;
import il.org.spartan.refactoring.utils.Source;
import il.org.spartan.refactoring.utils.Subject;
import il.org.spartan.refactoring.utils.expose;

/**
 * A wring is a transformation that works on an AstNode. Such a transformation
 * make a single simplification of the tree. A wring is so small that it is
 * idempotent: Applying a wring to the output of itself is the empty operation.
 *
 * @param <N> type of node which triggers the transformation.
 * @author Yossi Gil
 * @author Daniel Mittelman <code><mittelmania [at] gmail.com></code>
 * @since 2015-07-09
 */
public abstract class Wring<N extends ASTNode> {
  protected CompilationUnit u;
  protected Scalpel scalpel;

  /**
   * @param u current compilation unit
   * @return this wring
   */
  public Wring<N> initialize(@SuppressWarnings("hiding") CompilationUnit u) {
    this.u = u;
    return this;
  }
  /**
   * @param r rewriter
   * @param g text edit group
   * @return this wring
   */
  public Wring<N> createScalpel(ASTRewrite r, TextEditGroup g) {
    scalpel = Source.getScalpel(u, r, g);
    return this;
  }
  abstract String description(N n);
  /**
   * Determine whether the parameter is "eligible" for application of this
   * instance. The parameter must be within the scope of the current instance.
   *
   * @param n JD
   * @return <code><b>true</b></code> <i>iff</i> the argument is eligible for
   *         the simplification offered by this object.
   */
  boolean eligible(final N n) {
    return true;
  }
  Rewrite make(final N n) {
    return make(n, null);
  }
  Rewrite make(final N n, @SuppressWarnings("unused") final ExclusionManager exclude) {
    return make(n);
  }
  /**
   * Returns the preference group to which the wring belongs to. This method
   * should be overridden for each wring and should return one of the values of
   * {@link WringGroup}
   *
   * @return the preference group this wring belongs to
   */
  abstract WringGroup wringGroup();
  /**
   * Determines whether this {@link Wring} object is not applicable for a given
   * {@link PrefixExpression} is within the "scope" of this . Note that a
   * {@link Wring} is applicable in principle to an object, but that actual
   * application will be vacuous.
   *
   * @param e JD
   * @return <code><b>true</b></code> <i>iff</i> the argument is noneligible for
   *         the simplification offered by this object.
   * @see #eligible(InfixExpression)
   */
  final boolean nonEligible(final N n) {
    return !eligible(n);
  }
  /**
   * Determines whether this {@link Wring} object is applicable for a given
   * {@link InfixExpression} is within the "scope" of this . Note that it could
   * be the case that a {@link Wring} is applicable in principle to an object,
   * but that actual application will be vacuous.
   *
   * @param n JD
   * @return <code><b>true</b></code> <i>iff</i> the argument is within the
   *         scope of this object
   */
  boolean scopeIncludes(final N n) {
    return make(n, null) != null;
  }

  static abstract class AbstractSorting extends ReplaceCurrentNode<InfixExpression> {
    @Override final String description(final InfixExpression e) {
      return "Reorder operands of " + e.getOperator();
    }
    abstract boolean sort(List<Expression> operands);
  }

  static abstract class InfixSorting extends AbstractSorting {
    @Override boolean eligible(final InfixExpression e) {
      final List<Expression> es = Extract.allOperands(e);
      return !Wrings.mixedLiteralKind(es) && sort(es);
    }
    @Override Expression replacement(final InfixExpression e) {
      final List<Expression> operands = Extract.allOperands(e);
      return !sort(operands) ? null : Subject.operands(operands).to(e.getOperator());
    }
  }

  static abstract class InfixSortingOfCDR extends AbstractSorting {
    @Override boolean eligible(final InfixExpression e) {
      final List<Expression> es = Extract.allOperands(e);
      es.remove(0);
      return !Wrings.mixedLiteralKind(es) && sort(es);
    }
    @Override Expression replacement(final InfixExpression e) {
      final List<Expression> operands = Extract.allOperands(e);
      final Expression first = operands.remove(0);
      if (!sort(operands))
        return null;
      operands.add(0, first);
      return Subject.operands(operands).to(e.getOperator());
    }
  }

  static abstract class ReplaceCurrentNode<N extends ASTNode> extends Wring<N> {
    @Override final Rewrite make(final N n) {
      return !eligible(n) ? null : new Rewrite(description(n), n) {
        @SuppressWarnings("unused") @Override public void go(final ASTRewrite r, final TextEditGroup g) {
          scalpel.operate(n).replaceWith(replacement(n));
        }
      };
    }
    abstract ASTNode replacement(N n);
    @Override boolean scopeIncludes(final N n) {
      return replacement(n) != null;
    }
  }

  /**
   * Similar to {@link ReplaceCurrentNode}, but with an {@link ExclusionManager}
   */
  static abstract class ReplaceCurrentNodeExclude<N extends ASTNode> extends Wring<N> {
    @Override final Rewrite make(final N n, final ExclusionManager em) {
      preExclude(n, em);
      return !eligible(n) ? null : new Rewrite(description(n), n) {
        @SuppressWarnings("unused") @Override public void go(final ASTRewrite r, final TextEditGroup g) {
          scalpel.operate(n).replaceWith(replacement(n, em));
        }
      };
    }
    abstract ASTNode replacement(N n, final ExclusionManager em);
    abstract void preExclude(final N n, final ExclusionManager em);
    @Override boolean scopeIncludes(final N n) {
      return replacement(n, new ExclusionManager()) != null;
    }
  }

  /**
   * MultipleReplaceCurrentNode replaces multiple nodes in current statement
   * with multiple nodes (or a single node).
   *
   * @author Ori Roth <code><ori.rothh [at] gmail.com></code>
   * @since 2016-04-25
   */
  static abstract class MultipleReplaceCurrentNode<N extends ASTNode> extends Wring<N> {
    abstract ASTRewrite go(ASTRewrite r, N n, TextEditGroup g, List<ASTNode> bss, List<ASTNode> crs);
    @Override Rewrite make(final N n) {
      return new Rewrite(description(n), n) {
        @Override public void go(final ASTRewrite r, final TextEditGroup g) {
          final List<ASTNode> bss = new ArrayList<>();
          final List<ASTNode> crs = new ArrayList<>();
          MultipleReplaceCurrentNode.this.go(r, n, g, bss, crs);
          if (bss.size() != crs.size() && crs.size() != 1)
            return; // indicates bad wring design
          final boolean ucr = crs.size() == 1;
          final int s = bss.size();
          for (int i = 0; i < s; ++i)
            scalpel.operate(bss.get(i)).replaceWith(crs.get(ucr ? 0 : i));
        }
      };
    }
    @Override boolean scopeIncludes(final N n) {
      return go(ASTRewrite.create(n.getAST()), n, null, new ArrayList<>(), new ArrayList<>()) != null;
    }
  }

  /* TODO complete cases 1 and 2 of IfCommandsSequencerNoElseSingletonSequencer */
  static abstract class ReplaceToNextStatement<N extends ASTNode> extends Wring<N> {
    abstract ASTRewrite go(ASTRewrite r, N n, Statement nextStatement, TextEditGroup g);
    @Override Rewrite make(final N n, final ExclusionManager exclude) {
      final Statement nextStatement = Extract.nextStatement(n);
      if (nextStatement == null || !eligible(n))
        return null;
      exclude.exclude(nextStatement);
      return new Rewrite(description(n), n, nextStatement) {
        @Override public void go(final ASTRewrite r, final TextEditGroup g) {
          ASTNode p = n;
          while (!(p instanceof Statement))
            p = p.getParent();
          scalpel.operate(nextStatement, n);
          ReplaceToNextStatement.this.go(r, n, nextStatement, g);
        }
      };
    }
    @Override boolean scopeIncludes(final N n) {
      final Statement nextStatement = Extract.nextStatement(n);
      return nextStatement != null && go(ASTRewrite.create(n.getAST()), n, nextStatement, null) != null;
    }
  }

  /**
   * MultipleReplaceToNextStatement replaces multiple nodes in current statement
   * with multiple nodes (or a single node) in next statement.
   *
   * @author Ori Roth <code><ori.rothh [at] gmail.com></code>
   * @since 2016-04-25
   */
  static abstract class MultipleReplaceToNextStatement<N extends ASTNode> extends Wring<N> {
    abstract ASTRewrite go(ASTRewrite r, N n, Statement nextStatement, TextEditGroup g, List<ASTNode> bss, List<ASTNode> crs);
    @Override Rewrite make(final N n, final ExclusionManager exclude) {
      final Statement nextStatement = Extract.nextStatement(n);
      if (nextStatement == null || !eligible(n))
        return null;
      exclude.exclude(nextStatement);
      return new Rewrite(description(n), n, nextStatement) {
        @Override public void go(final ASTRewrite r, final TextEditGroup g) {
          final List<ASTNode> bss = new ArrayList<>();
          final List<ASTNode> crs = new ArrayList<>();
          MultipleReplaceToNextStatement.this.go(r, n, nextStatement, g, bss, crs);
          if (bss.size() != crs.size() && crs.size() != 1)
            return; // indicates bad wring design
          final boolean ucr = crs.size() == 1;
          final int s = bss.size();
          for (int i = 0; i < s; ++i)
            scalpel.operate(bss.get(i)).addComments(nextStatement).replaceWith(crs.get(ucr ? 0 : i));
        }
      };
    }
    @Override boolean scopeIncludes(final N n) {
      final Statement nextStatement = Extract.nextStatement(n);
      return nextStatement != null
          && go(ASTRewrite.create(n.getAST()), n, nextStatement, null, new ArrayList<>(), new ArrayList<>()) != null;
    }
  }

  static abstract class VariableDeclarationFragementAndStatement extends ReplaceToNextStatement<VariableDeclarationFragment> {
    static InfixExpression.Operator asInfix(final Assignment.Operator o) {
      return o == PLUS_ASSIGN ? PLUS
          : o == MINUS_ASSIGN ? MINUS
              : o == TIMES_ASSIGN ? TIMES
                  : o == DIVIDE_ASSIGN ? DIVIDE
                      : o == BIT_AND_ASSIGN ? AND
                          : o == BIT_OR_ASSIGN ? OR
                              : o == BIT_XOR_ASSIGN ? XOR
                                  : o == REMAINDER_ASSIGN ? REMAINDER
                                      : o == LEFT_SHIFT_ASSIGN ? LEFT_SHIFT //
                                          : o == RIGHT_SHIFT_SIGNED_ASSIGN ? RIGHT_SHIFT_SIGNED //
                                              : o == RIGHT_SHIFT_UNSIGNED_ASSIGN ? RIGHT_SHIFT_UNSIGNED : null;
    }
    static boolean hasAnnotation(final VariableDeclarationFragment f) {
      return hasAnnotation((VariableDeclarationStatement) f.getParent());
    }
    @SuppressWarnings("unchecked") static boolean hasAnnotation(final VariableDeclarationStatement s) {
      return hasAnnotation(s.modifiers());
    }
    static boolean hasAnnotation(final List<IExtendedModifier> ms) {
      for (final IExtendedModifier m : ms)
        if (m.isAnnotation())
          return true;
      return false;
    }
    static Expression assignmentAsExpression(final Assignment a) {
      final Operator o = a.getOperator();
      return o == ASSIGN ? duplicate(right(a)) : Subject.pair(left(a), right(a)).to(asInfix(o));
    }
    static boolean doesUseForbiddenSiblings(final VariableDeclarationFragment f, final ASTNode... ns) {
      for (final VariableDeclarationFragment b : forbiddenSiblings(f))
        if (Collect.BOTH_SEMANTIC.of(b).existIn(ns))
          return true;
      return false;
    }
    static List<VariableDeclarationFragment> forbiddenSiblings(final VariableDeclarationFragment f) {
      final List<VariableDeclarationFragment> $ = new ArrayList<>();
      boolean collecting = false;
      for (final VariableDeclarationFragment brother : expose.fragments((VariableDeclarationStatement) f.getParent())) {
        if (brother == f) {
          collecting = true;
          continue;
        }
        if (collecting)
          $.add(brother);
      }
      return $;
    }
    static int removalSaving(final VariableDeclarationFragment f) {
      final VariableDeclarationStatement parent = (VariableDeclarationStatement) f.getParent();
      final int $ = size(parent);
      if (parent.fragments().size() <= 1)
        return $;
      final VariableDeclarationStatement newParent = duplicate(parent);
      newParent.fragments().remove(parent.fragments().indexOf(f));
      return $ - size(newParent);
    }
    static int eliminationSaving(final VariableDeclarationFragment f) {
      final VariableDeclarationStatement parent = (VariableDeclarationStatement) f.getParent();
      final List<VariableDeclarationFragment> live = live(f, expose.fragments(parent));
      final int $ = size(parent);
      if (live.isEmpty())
        return $;
      final VariableDeclarationStatement newParent = duplicate(parent);
      final List<VariableDeclarationFragment> fs = expose.fragments(newParent);
      fs.clear();
      fs.addAll(live);
      return $ - size(newParent);
    }
    /**
     * Removes a {@link VariableDeclarationFragment}, leaving intact any other
     * fragment fragments in the containing {@link VariabelDeclarationStatement}
     * . Still, if the containing node left empty, it is removed as well.
     *
     * @param f
     * @param r
     * @param g
     */
    static void remove(final VariableDeclarationFragment f, final ASTRewrite r, final TextEditGroup g) {
      final VariableDeclarationStatement parent = (VariableDeclarationStatement) f.getParent();
      r.remove(parent.fragments().size() > 1 ? f : parent, g);
    }
    /**
     * Eliminates a {@link VariableDeclarationFragment}, with any other fragment
     * fragments which are not live in the containing
     * {@link VariabelDeclarationStatement}. If no fragments are left, then this
     * containing node is eliminated as well.
     *
     * @param f
     * @param r
     * @param g
     */
    static void eliminate(final VariableDeclarationFragment f, final ASTRewrite r, final TextEditGroup g) {
      final VariableDeclarationStatement parent = (VariableDeclarationStatement) f.getParent();
      final List<VariableDeclarationFragment> live = live(f, expose.fragments(parent));
      if (live.isEmpty()) {
        r.remove(parent, g);
        return;
      }
      final VariableDeclarationStatement newParent = duplicate(parent);
      final List<VariableDeclarationFragment> fs = expose.fragments(newParent);
      fs.clear();
      fs.addAll(live);
      r.replace(parent, newParent, g);
    }
    private static List<VariableDeclarationFragment> live(final VariableDeclarationFragment f,
        final List<VariableDeclarationFragment> fs) {
      final List<VariableDeclarationFragment> $ = new ArrayList<>();
      for (final VariableDeclarationFragment brother : fs)
        if (brother != null && brother != f && brother.getInitializer() != null)
          $.add(duplicate(brother));
      return $;
    }
    abstract ASTRewrite go(ASTRewrite r, VariableDeclarationFragment f, SimpleName n, Expression initializer,
        Statement nextStatement, TextEditGroup g);
    @Override final ASTRewrite go(final ASTRewrite r, final VariableDeclarationFragment f, final Statement nextStatement,
        final TextEditGroup g) {
      if (!Is.variableDeclarationStatement(f.getParent()))
        return null;
      final SimpleName n = f.getName();
      return n == null ? null : go(r, f, n, f.getInitializer(), nextStatement, g);
    }
    @Override Rewrite make(final VariableDeclarationFragment f, final ExclusionManager exclude) {
      final Rewrite $ = super.make(f, exclude);
      if ($ != null && exclude != null)
        exclude.exclude(f.getParent());
      return $;
    }
  }
}

final class LocalInliner {
  static Wrapper<ASTNode>[] wrap(final ASTNode[] ns) {
    @SuppressWarnings("unchecked") final Wrapper<ASTNode>[] $ = new Wrapper[ns.length];
    int i = 0;
    for (final ASTNode t : ns)
      $[i++] = new Wrapper<>(t);
    return $;
  }

  final SimpleName name;
  final ASTRewrite rewriter;
  final TextEditGroup editGroup;

  LocalInliner(final SimpleName n) {
    this(n, null, null);
  }
  LocalInliner(final SimpleName n, final ASTRewrite rewriter, final TextEditGroup g) {
    name = n;
    this.rewriter = rewriter;
    editGroup = g;
  }
  LocalInlineWithValue byValue(final Expression replacement) {
    return new LocalInlineWithValue(replacement);
  }

  class LocalInlineWithValue extends Wrapper<Expression> {
    LocalInlineWithValue(final Expression replacement) {
      super(Extract.core(replacement));
    }
    @SafeVarargs protected final void inlineInto(final ASTNode... ns) {
      inlineInto(wrap(ns));
    }
    @SafeVarargs private final void inlineInto(final Wrapper<ASTNode>... ns) {
      for (final Wrapper<ASTNode> e : ns)
        inlineIntoSingleton(get(), e);
    }
    /**
     * Computes the total number of AST nodes in the replaced parameters
     *
     * @param es JD
     * @return A non-negative integer, computed from original size of the
     *         parameters, the number of occurrences of {@link #name} in the
     *         operands, and the size of the replacement.
     */
    int replacedSize(final ASTNode... ns) {
      return size(ns) + uses(ns).size() * (size(get()) - 1);
    }
    /**
     * Computes the number of AST nodes added as a result of the replacement
     * operation.
     *
     * @param es JD
     * @return A non-negative integer, computed from the number of occurrences
     *         of {@link #name} in the operands, and the size of the
     *         replacement.
     */
    int addedSize(final ASTNode... ns) {
      return uses(ns).size() * (size(get()) - 1);
    }
    boolean canInlineInto(final ASTNode... ns) {
      return Collect.definitionsOf(name).in(ns).isEmpty() && (Is.sideEffectFree(get()) || uses(ns).size() <= 1);
    }
    private List<SimpleName> uses(final ASTNode... ns) {
      return Collect.usesOf(name).in(ns);
    }
    private void inlineIntoSingleton(final ASTNode replacement, final Wrapper<ASTNode> ns) {
      final ASTNode oldExpression = ns.get();
      final ASTNode newExpression = duplicate(ns.get());
      ns.set(newExpression);
      rewriter.replace(oldExpression, newExpression, editGroup);
      for (final ASTNode use : Collect.forAllOccurencesExcludingDefinitions(name).in(newExpression))
        rewriter.replace(use,
            !(use instanceof Expression) ? replacement : new Plant((Expression) replacement).into(use.getParent()), editGroup);
    }
  }
}