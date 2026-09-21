package projectLabo.visitors.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VectorValue implements Value {
    private final List<Value> elements;

    public VectorValue(List<Value> elements) {
        this.elements = new ArrayList<>(elements);
    }

    public List<Value> getElements() {
        return elements;
    }

    @Override
    public VectorValue toVector() {
        return this; 
    }

    @Override
    public String toString() {
        return "VectorValue[" + elements.size() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        VectorValue that = (VectorValue) obj;
        return Objects.equals(elements, that.elements);
    }
}