package projectLabo.visitors.typechecking;

import static java.util.Objects.requireNonNull;

public record VectorType(Type elemType, int size) implements Type{

    public static final String NAME="VectorType";

    public VectorType{
        requireNonNull(elemType);
        if(size<0)
        {
            throw new IllegalArgumentException("La dim di un vettore non può essere negativa");
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s[%d]",elemType,size);
    }

    //if not a vector -> exception
    @Override
    public VectorType toVectorType()
    {
        return this;
    }
}
