package projectLabo.parser.ast;

import projectLabo.visitors.Visitor;

public class Rev extends UnaryOp{
    
    public Rev(Exp exp)
    {
        super(exp);
    }

    @Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitRev(exp);
	}
}
