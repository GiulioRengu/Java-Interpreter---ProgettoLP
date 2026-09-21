package projectLabo.parser.ast;
import projectLabo.visitors.Visitor;

public class ForStmt implements Stmt {
    public final Variable var;
    public final Exp exp;
    public final Block block;

    public ForStmt(Variable var, Exp exp, Block block) {
        this.var = var;
        this.exp = exp;
        this.block = block;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitForStmt(var, exp, block);
    }

    @Override
    public String toString() {
        return String.format("ForStmt(%s,%s,%s)", var, exp, block);
    }
}