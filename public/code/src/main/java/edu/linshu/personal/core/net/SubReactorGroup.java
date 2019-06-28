package edu.linshu.personal.core.net;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/27 10:27
 */
public class SubReactorGroup extends ReactorGroup {

    private final IReactor[] reactors;
    private final int reactorCount;


    public SubReactorGroup(int count) {
        this.reactorCount = count;
        this.reactors = new IReactor[count];
        for (int i = 0; i < count; i++) {
            reactors[i] = new SubReactor("SubReactor_" + i);
        }
    }


    @Override
    public int reactorCount() {
        return reactorCount;
    }

    @Override
    public IReactor get(int index) {
        return reactors[index];
    }
}
