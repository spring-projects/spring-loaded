package grails;

public abstract class Implementation1<T> implements Interface1<T> {

    @Override
    public final T process1(T type) {
        return type;
    }

}