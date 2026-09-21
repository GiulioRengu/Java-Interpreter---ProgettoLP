package projectLabo.parser.ast;
import projectLabo.visitors.Visitor;

public class Flatten extends UnaryOp {
    public Flatten(Exp exp) {
        super(exp);
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitFlatten(exp);
    }
}