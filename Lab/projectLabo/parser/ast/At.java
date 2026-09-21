package projectLabo.parser.ast;

import projectLabo.visitors.Visitor;

public class At extends BinaryOp{

    public At(Exp left, Exp right)
    {
        super(left, right);
    }

    @Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitAt(left, right);
	}
}
