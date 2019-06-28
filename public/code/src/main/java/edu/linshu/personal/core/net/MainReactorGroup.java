package edu.linshu.personal.core.net;

import java.util.Objects;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/27 10:27
 */
public class MainReactorGroup extends ReactorGroup {

    private final IReactor[] reactors;
    private final int reactorCount;
    private ReactorGroup subReactorGroup;

    public MainReactorGroup(IServerSocket selector, int count) {
        this.reactorCount = count;
        this.reactors = new IReactor[count];
        for (int i = 0; i < count; i++) {
            reactors[i] = new MainReactor(selector, "MainReactor_" + i);
        }
    }

    @Override
    public void setSubReactorGroup(ReactorGroup subReactorGroup) {
        this.subReactorGroup = subReactorGroup;

        for (int i = 0; i < reactorCount(); i++) {
            reactors[i].setSubReactorGroup(subReactorGroup);
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

    @Override
    public IReactorGroup getSubReactorGroup() {
        return subReactorGroup;
    }

    @Override
    public void start() {
        if (Objects.isNull(subReactorGroup)) {
            throw new IllegalStateException("subReactorGroup 未初始化");
        }

        super.start();

        getSubReactorGroup().start();
    }
}
