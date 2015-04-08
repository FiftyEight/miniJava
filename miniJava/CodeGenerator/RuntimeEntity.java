package miniJava.CodeGenerator;

/**
 * Created by cloftin on 4/7/15.
 * Each declaration is given an entity description corresponding to a KnownValue (a constant) or an UnknownValue (a variable with a known address)
 * the entities also have a size corresponding to the size of the type of data
 *
 * ask about where to do this in class (a seperate traversal? could we do it duing type checking?)
 */
public abstract class RuntimeEntity {
    public int size;
}
