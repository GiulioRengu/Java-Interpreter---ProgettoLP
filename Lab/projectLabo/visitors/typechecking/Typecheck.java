package projectLabo.visitors.typechecking;

import static projectLabo.visitors.typechecking.AtomicType.*;

import projectLabo.visitors.environments.EnvironmentException;
import projectLabo.parser.ast.Block;
import projectLabo.parser.ast.Exp;
import projectLabo.parser.ast.Stmt;
import projectLabo.parser.ast.StmtSeq;
import projectLabo.parser.ast.Variable;
import projectLabo.visitors.Visitor;

public class Typecheck implements Visitor<Type> {

	private final StaticEnv env = new StaticEnv();

	// useful to typecheck binary operations where operands must have the same type
	private void checkBinOp(Exp left, Exp right, Type type) {
		type.checkEqual(left.accept(this));
		type.checkEqual(right.accept(this));
	}

	// static semantics for programs; no value returned by the visitor

	@Override
	public Type visitExpProg(StmtSeq stmtSeq) {
		try {
			stmtSeq.accept(this);
		} catch (EnvironmentException e) { // undeclared variable
			throw new TypecheckerException(e);
		}
		return null;
	}

	// static semantics for sequences of statements
	// no value returned by the visitor

	@Override
	public Type visitEmptyStmtSeq() {
		return null;
	}

	@Override
	public Type visitNonEmptyStmtSeq(Stmt first, StmtSeq rest) {
		first.accept(this);
		rest.accept(this);
		return null;
	}

	// static semantics for statements; no value returned by the visitor

	@Override
	public Type visitIfStmt(Exp exp, Block thenBlock, Block elseBlock) {
		BOOL.checkEqual(exp.accept(this));
		thenBlock.accept(this);
		if (elseBlock != null) 
		{
			elseBlock.accept(this);
		}
		return null;
	}

	@Override
	public Type visitPrintStmt(Exp exp) {
		exp.accept(this);
		return null;
	}

	@Override
	public Type visitVarStmt(Variable var, Exp exp) {
		env.dec(var, exp.accept(this));
		return null;
	}

	@Override
	public Type visitBlock(StmtSeq stmtSeq) {
		env.enterLevel();
		stmtSeq.accept(this);
		env.exitLevel();
		return null;
	}

	// static semantics of expressions; a type is returned by the visitor

	@Override
	public Type visitAdd(Exp left, Exp right) {
		Type leftType=left.accept(this);
		Type rightType=right.accept(this);

	if (leftType instanceof VectorType leftVect) {
		VectorType rightVect=rightType.toVectorType();
		
		// Questo controllerà in automatico sia il tipo degli elementi che la dimensione, 
		// lanciando l'eccezione col formato esatto atteso dai test!
		leftVect.checkEqual(rightVect); 
		
		INT.checkEqual(leftVect.elemType());
		return new VectorType(INT, leftVect.size());
	}

		INT.checkEqual(leftType);
		INT.checkEqual(rightType);
		return INT;
	}

	@Override
	public AtomicType visitBoolLiteral(boolean value) {
		return BOOL;
	}

	@Override
	public AtomicType visitEq(Exp left, Exp right) {
		left.accept(this).checkEqual(right.accept(this));
		return BOOL;
	}

	@Override
	public Type visitFst(Exp exp) {
		Type type = exp.accept(this);
		if (type instanceof VectorType vect) {
			try {
				return new VectorType(vect.elemType().toPairType().fstType(), vect.size());
			} catch (TypecheckerException e) {
				throw new TypecheckerException(vect.elemType() + "[]", PairType.NAME + "[]");
			}
		}
		return type.toPairType().fstType();
	}

	@Override
	public AtomicType visitIntLiteral(int value) {
		return INT;
	}

	@Override
	public AtomicType visitMinus(Exp exp) {
		INT.checkEqual(exp.accept(this));
		return INT;
	}

	@Override
    public Type visitMul(Exp left, Exp right) {
        Type leftType = left.accept(this);
        Type rightType = right.accept(this);

        if (leftType instanceof VectorType || rightType instanceof VectorType) {
            
            if (leftType instanceof VectorType && !(rightType instanceof VectorType)) {
                String expected = leftType.toVectorType().elemType().toString() + "[]";
                throw new TypecheckerException(rightType.toString(), expected);
            }
            if (!(leftType instanceof VectorType) && rightType instanceof VectorType) {
                String expected = rightType.toVectorType().elemType().toString() + "[]";
                throw new TypecheckerException(leftType.toString(), expected);
            }

            VectorType leftVect = leftType.toVectorType();
            VectorType rightVect = rightType.toVectorType();
            
            INT.checkEqual(leftVect.elemType());
            INT.checkEqual(rightVect.elemType());
            
            return new VectorType(new VectorType(INT, leftVect.size()), rightVect.size());
        }

        INT.checkEqual(leftType);
        INT.checkEqual(rightType);
        return INT;
    }

	@Override
	public PairType visitPairLit(Exp left, Exp right) {
		return new PairType(left.accept(this), right.accept(this));
	}

	@Override
	public Type visitSnd(Exp exp) {
		Type type = exp.accept(this);
		if (type instanceof VectorType vect) {
			try {
				return new VectorType(vect.elemType().toPairType().sndType(), vect.size());
			} catch (TypecheckerException e) {
				throw new TypecheckerException(vect.elemType() + "[]", PairType.NAME + "[]");
			}
		}
		return type.toPairType().sndType();
	}

	@Override
	public Type visitVariable(Variable var) {
		return env.lookup(var);
	}

	@Override
	public AtomicType visitAnd(Exp left, Exp right) {
		checkBinOp(left, right, BOOL);
		return BOOL;
	}

	@Override
	public AtomicType visitNot(Exp exp) {
		BOOL.checkEqual(exp.accept(this));
		return BOOL;
	}

	@Override
	public Type visitAssertStmt(Exp exp) {
		BOOL.checkEqual(exp.accept(this));
		return null;
	}

	@Override
	public Type visitAssignStmt(Variable var, Exp exp) {
		env.lookup(var).checkEqual(exp.accept(this));
		return null;
	}

	@Override
	public Type visitForStmt(Variable var, Exp exp, Block block) {
		VectorType vect=exp.accept(this).toVectorType();

		env.enterLevel();
		env.dec(var, vect.elemType());
		block.accept(this);
		env.exitLevel();

		return null;
	}

	@Override
    public VectorType visitZip(Exp left, Exp right) {
        VectorType vLeft = left.accept(this).toVectorType();
        VectorType vRight = right.accept(this).toVectorType();

        if (vLeft.size() != vRight.size()) {
            String found = "VectorType[" + vRight.size() + "]";
            String expected = "VectorType[" + vLeft.size() + "]";
            
            throw new TypecheckerException(found, expected);
        }
        
        return new VectorType(new PairType(vLeft.elemType(), vRight.elemType()), vLeft.size());
    }

	@Override
    public VectorType visitCat(Exp left, Exp right) {
        VectorType vLeft = left.accept(this).toVectorType();
        VectorType vRight = right.accept(this).toVectorType();

        if (!vLeft.elemType().equals(vRight.elemType())) {
            String found = vRight.elemType().toString() + "[]";
            String expected = vLeft.elemType().toString() + "[]";
            
            throw new TypecheckerException(found, expected);
        }

        return new VectorType(vLeft.elemType(), vLeft.size() + vRight.size());
    }

	@Override
    public VectorType visitFlatten(Exp exp) {
        VectorType outer = exp.accept(this).toVectorType();
        Type innerType = outer.elemType();
        if (!(innerType instanceof VectorType)) {
            throw new TypecheckerException(innerType.toString() + "[]", "VectorType[]");
        }

        VectorType inner = innerType.toVectorType();

        return new VectorType(inner.elemType(), outer.size() * inner.size());
    }

	@Override
	public VectorType visitSingletonVect(Exp exp) {
		return new VectorType(exp.accept(this), 1);
	}

	@Override
	public Type visitAt(Exp left, Exp right)
	{
		VectorType vleft=left.accept(this).toVectorType();
		INT.checkEqual(right.accept(this));

		return vleft.elemType();
	}

	@Override
	public VectorType visitRev(Exp exp)
	{
		VectorType vect=exp.accept(this).toVectorType();
		return vect;
	}
}