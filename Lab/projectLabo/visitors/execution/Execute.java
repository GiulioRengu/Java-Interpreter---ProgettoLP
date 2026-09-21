package projectLabo.visitors.execution;

import java.io.PrintWriter;

import projectLabo.visitors.environments.EnvironmentException;
import projectLabo.parser.ast.Block;
import projectLabo.parser.ast.Exp;
import projectLabo.parser.ast.Stmt;
import projectLabo.parser.ast.StmtSeq;
import projectLabo.parser.ast.Variable;
import projectLabo.visitors.Visitor;

import static java.util.Objects.requireNonNull;

public class Execute implements Visitor<Value> {
	// aggiungere i metodi mancanti

	private final DynamicEnv env = new DynamicEnv();
	private final PrintWriter printWriter; // output stream used to print values

	public Execute() {
		printWriter = new PrintWriter(System.out, true);
	}

	public Execute(PrintWriter printWriter) {
		this.printWriter = requireNonNull(printWriter);
	}

	// dynamic semantics for programs; no value returned by the visitor

	@Override
	public Value visitExpProg(StmtSeq stmtSeq) {
		try {
			stmtSeq.accept(this);
			// possible runtime errors
			// EnvironmentException: undefined variable
		} catch (EnvironmentException e) {
			throw new InterpreterException(e);
		}
		return null;
	}

	// dynamic semantics for sequences of statements
	// no value returned by the visitor

	@Override
	public Value visitEmptyStmtSeq() {
		return null;
	}

	@Override
	public Value visitNonEmptyStmtSeq(Stmt first, StmtSeq rest) {
		first.accept(this);
		rest.accept(this);
		return null;
	}

	// dynamic semantics for statements; no value returned by the visitor

	@Override
	public Value visitIfStmt(Exp exp, Block thenBlock, Block elseBlock) {
		if (exp.accept(this).toBool())
			thenBlock.accept(this);
		else if (elseBlock != null)
			elseBlock.accept(this);
		return null;
	}

	@Override
	public Value visitPrintStmt(Exp exp) {
		printWriter.println(exp.accept(this));
		return null;
	}

	@Override
	public Value visitVarStmt(Variable var, Exp exp) {
		env.dec(var, exp.accept(this));
		return null;
	}

	@Override
	public Value visitBlock(StmtSeq stmtSeq) {
		env.enterLevel();
		stmtSeq.accept(this);
		env.exitLevel();
		return null;
	}

	@Override
    public Value visitAdd(Exp left, Exp right) {
        Value lVal = left.accept(this);
        Value rVal = right.accept(this);
        
        if (lVal instanceof VectorValue || rVal instanceof VectorValue) {
            VectorValue lVect = lVal.toVector();
            VectorValue rVect = rVal.toVector();
            
            java.util.List<Value> lList = lVect.getElements();
            java.util.List<Value> rList = rVect.getElements();
            
            if (lList.size() != rList.size()) {
                throw new InterpreterException("vectors must have the same size");
            }
            
            java.util.List<Value> result = new java.util.ArrayList<>();
            for (int i = 0; i < lList.size(); i++) {
                result.add(new IntValue(lList.get(i).toInt() + rList.get(i).toInt()));
            }
            return new VectorValue(result);
        }
        
        return new IntValue(lVal.toInt() + rVal.toInt());
    }

	@Override
	public BoolValue visitBoolLiteral(boolean value) {
		return new BoolValue(value);
	}

	@Override
	public BoolValue visitEq(Exp left, Exp right) {
		return new BoolValue(left.accept(this).equals(right.accept(this)));
	}

	@Override
    public Value visitFst(Exp exp) {
        Value val = exp.accept(this);
        if (val instanceof VectorValue) {
            java.util.List<Value> result = new java.util.ArrayList<>();
            for (Value v : ((VectorValue) val).getElements()) {
                result.add(v.toPair().fstVal());
            }
            return new VectorValue(result);
        }
        return val.toPair().fstVal();
    }

	@Override
	public IntValue visitIntLiteral(int value) {
		return new IntValue(value);
	}

	@Override
	public IntValue visitMinus(Exp exp) {
		return new IntValue(-exp.accept(this).toInt());
	}

	@Override
    public Value visitMul(Exp left, Exp right) {
        Value lVal = left.accept(this);
        Value rVal = right.accept(this);
        
        if (lVal instanceof VectorValue || rVal instanceof VectorValue) {
            VectorValue lVect = lVal.toVector();
            VectorValue rVect = rVal.toVector();
            
            java.util.List<Value> lList = lVect.getElements();
            java.util.List<Value> rList = rVect.getElements();
            java.util.List<Value> resultMatrix = new java.util.ArrayList<>();
            
            for (Value j : rList) {
                java.util.List<Value> column = new java.util.ArrayList<>();
                for (Value i : lList) {
                    column.add(new IntValue(j.toInt() * i.toInt()));
                }
                resultMatrix.add(new VectorValue(column));
            }
            return new VectorValue(resultMatrix);
        }
        
        return new IntValue(lVal.toInt() * rVal.toInt());
    }

	@Override
	public PairValue visitPairLit(Exp left, Exp right) {
		return new PairValue(left.accept(this), right.accept(this));
	}

	@Override
    public Value visitSnd(Exp exp) {
        Value val = exp.accept(this);
        if (val instanceof VectorValue) {
            java.util.List<Value> result = new java.util.ArrayList<>();
            for (Value v : ((VectorValue) val).getElements()) {
                result.add(v.toPair().sndVal());
            }
            return new VectorValue(result);
        }
        return val.toPair().sndVal();
    }

	@Override
	public Value visitVariable(Variable var) {
		return env.lookup(var);
	}

	@Override
	public Value visitAnd(Exp left, Exp right) {
		return new BoolValue(left.accept(this).toBool() && right.accept(this).toBool());
	}

	@Override
	public Value visitNot(Exp exp) {
		return new BoolValue(!exp.accept(this).toBool());
	}

	@Override
	public Value visitAssertStmt(Exp exp) {
		if (!exp.accept(this).toBool())
			throw new InterpreterException(new AssertionError());
		return null;
	}

	@Override
	public Value visitAssignStmt(Variable var, Exp exp) {
		env.update(var, exp.accept(this));
		return null;
	}

	@Override
    public Value visitSingletonVect(Exp exp) {
        return new VectorValue(java.util.List.of(exp.accept(this)));
    }

    @Override
    public Value visitCat(Exp left, Exp right) {
        VectorValue v1 = left.accept(this).toVector();
        VectorValue v2 = right.accept(this).toVector();
        
        java.util.List<Value> result = new java.util.ArrayList<>(v1.getElements());
        result.addAll(v2.getElements());
        
        return new VectorValue(result);
    }

    @Override
    public Value visitZip(Exp left, Exp right) {
        VectorValue v1 = left.accept(this).toVector();
        VectorValue v2 = right.accept(this).toVector();
        
        if (v1.getElements().size() != v2.getElements().size()) {
            throw new InterpreterException("vectors must have the same size");
        }
        
        java.util.List<Value> result = new java.util.ArrayList<>();
        for (int i = 0; i < v1.getElements().size(); i++) {
            result.add(new PairValue(v1.getElements().get(i), v2.getElements().get(i)));
        }
        
        return new VectorValue(result);
    }

    @Override
    public Value visitFlatten(Exp exp) {
        VectorValue v = exp.accept(this).toVector();
        java.util.List<Value> result = new java.util.ArrayList<>();
        
        for (Value innerList : v.getElements()) {
            result.addAll(innerList.toVector().getElements());
        }
        
        return new VectorValue(result);
    }

    @Override
    public Value visitForStmt(Variable var, Exp exp, Block block) {
        VectorValue vector = exp.accept(this).toVector();
        
        env.enterLevel(); 
        env.dec(var, new IntValue(0)); 
        
        for (Value elem : vector.getElements()) {
            env.update(var, elem); 
            block.accept(this);    
        }
        
        env.exitLevel(); 
        return null;
    }

	@Override
	public Value visitAt(Exp left, Exp right)
	{
		VectorValue vector=left.accept(this).toVector();
		int index=right.accept(this).toInt();

		if(index<0 || index>=vector.getElements().size())
		{
			throw new InterpreterException("Index out of bounds");
		}
		return vector.getElements().get(index);

	}

	@Override
	public Value visitRev(Exp exp)
	{
		VectorValue vect=exp.accept(this).toVector();
		var elems=vect.getElements();
		java.util.List<Value> result = new java.util.ArrayList<>();
		for (int i=elems.size()-1; i>=0; i--)
		{
			result.add(elems.get(i));
		}
		return new VectorValue(result);
	}
}