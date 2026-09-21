package projectLabo.parser.ast;
import projectLabo.visitors.Visitor;

public class SingletonVect implements Exp {
    public final Exp exp;

    public SingletonVect(Exp exp) {
        this.exp = exp;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitSingletonVect(exp);
    }

    @Override
    public String toString() {
        return String.format("SingletonVect(%s)", exp);
    }
}